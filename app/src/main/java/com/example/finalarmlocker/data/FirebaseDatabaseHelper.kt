package com.example.finalarmlocker.data

import android.util.Log
import com.example.finalarmlocker.util.HashUtil
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Query
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Todo o acesso ao Firebase Realtime Database passa por aqui.
 *
 * Estrutura do banco:
 * - `users/{id}`: moradores ([User])
 * - `packages/{id}`: encomendas ([LockerPackage])
 * - `counters/userId` e `counters/packageId`: contadores que geram IDs sequenciais
 *
 * As funções são `suspend` (coroutines). As leituras usam `get()`, que busca no servidor
 * quando há conexão, para não decidir nada com base em dados antigos do cache offline.
 */
class FirebaseDatabaseHelper(database: FirebaseDatabase = FirebaseDatabase.getInstance()) {

    private val usersRef = database.getReference("users")
    private val packagesRef = database.getReference("packages")
    private val userCounterRef = database.getReference("counters/userId")
    private val packageCounterRef = database.getReference("counters/packageId")

    enum class RegisterUserResult { SUCCESS, EMAIL_IN_USE, CPF_IN_USE, ERROR }

    enum class RegisterPackageResult { SUCCESS, LOCKER_IN_USE, USER_NOT_FOUND, APARTMENT_MISMATCH, ERROR }

    sealed interface LoginResult {
        data class Success(val user: User) : LoginResult
        data object InvalidCredentials : LoginResult
        data object Error : LoginResult
    }

    /** Cadastra o morador, recusando e-mail ou CPF que já existam. */
    suspend fun registerUser(user: User): RegisterUserResult = safeCall(RegisterUserResult.ERROR) {
        when {
            exists(usersRef.orderByChild("email").equalTo(user.email.orEmpty())) ->
                RegisterUserResult.EMAIL_IN_USE

            exists(usersRef.orderByChild("cpf").equalTo(user.cpf.orEmpty())) ->
                RegisterUserResult.CPF_IN_USE

            else -> {
                val id = nextId(userCounterRef)
                usersRef.child(id.toString()).setValue(user.copy(id = id)).await()
                RegisterUserResult.SUCCESS
            }
        }
    }

    /**
     * Confere e-mail e senha. Procura o e-mail como foi digitado e também em minúsculas,
     * para funcionar com contas novas (salvas em minúsculas) e antigas.
     */
    suspend fun login(email: String, password: String): LoginResult = safeCall<LoginResult>(LoginResult.Error) {
        val hashedPassword = HashUtil.hashPassword(password)
        val candidates = listOf(email.trim(), email.trim().lowercase()).distinct()
        for (candidate in candidates) {
            val user = usersRef.orderByChild("email").equalTo(candidate).get().await()
                .children
                .mapNotNull { it.getValue(User::class.java) }
                .firstOrNull { it.password == hashedPassword }
            if (user != null) return@safeCall LoginResult.Success(user)
        }
        LoginResult.InvalidCredentials
    }

    suspend fun getUser(id: Int): User? = safeCall<User?>(null) {
        usersRef.child(id.toString()).get().await().getValue(User::class.java)
    }

    /**
     * Registra a encomenda depois de conferir que:
     * 1. o armário está livre;
     * 2. existe um morador com o CPF informado;
     * 3. o apartamento informado é o mesmo do cadastro desse morador.
     */
    suspend fun registerPackage(pkg: LockerPackage): RegisterPackageResult =
        safeCall(RegisterPackageResult.ERROR) {
            if (exists(packagesRef.orderByChild("locker").equalTo(pkg.locker.orEmpty()))) {
                return@safeCall RegisterPackageResult.LOCKER_IN_USE
            }

            val owner = usersRef.orderByChild("cpf").equalTo(pkg.cpf.orEmpty()).get().await()
                .children
                .firstNotNullOfOrNull { it.getValue(User::class.java) }
                ?: return@safeCall RegisterPackageResult.USER_NOT_FOUND

            if (!sameApartment(owner.apartament, pkg.apartmentNumber)) {
                return@safeCall RegisterPackageResult.APARTMENT_MISMATCH
            }

            val id = nextId(packageCounterRef)
            packagesRef.child(id.toString()).setValue(pkg.copy(id = id)).await()
            RegisterPackageResult.SUCCESS
        }

    /** Armários ocupados no momento, ou `null` se não foi possível consultar. */
    suspend fun getOccupiedLockers(): Set<String>? = safeCall<Set<String>?>(null) {
        packagesRef.get().await().toPackages().mapNotNull { it.locker }.toSet()
    }

    /** Leitura única das encomendas de um CPF, ou `null` em caso de erro. */
    suspend fun getPackagesByCpf(cpf: String): List<LockerPackage>? = safeCall<List<LockerPackage>?>(null) {
        packagesRef.orderByChild("cpf").equalTo(cpf).get().await().toPackages()
    }

    /**
     * Encomendas de um CPF em tempo real. O listener do Firebase é removido sozinho
     * quando quem está coletando o Flow para (por exemplo, quando a tela fecha).
     */
    fun observePackagesByCpf(cpf: String): Flow<List<LockerPackage>> = callbackFlow {
        val query = packagesRef.orderByChild("cpf").equalTo(cpf)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.toPackages())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /** Apaga a encomenda, o que também libera o armário. */
    suspend fun deletePackage(id: Int): Boolean = safeCall(false) {
        packagesRef.child(id.toString()).removeValue().await()
        true
    }

    private fun sameApartment(registered: String?, informed: String?): Boolean =
        registered != null && informed != null &&
            registered.trim().equals(informed.trim(), ignoreCase = true)

    private fun DataSnapshot.toPackages(): List<LockerPackage> =
        children.mapNotNull { it.getValue(LockerPackage::class.java) }

    private suspend fun exists(query: Query): Boolean = query.get().await().exists()

    /** Incrementa o contador com uma transação (seguro com vários aparelhos ao mesmo tempo). */
    private suspend fun nextId(counterRef: DatabaseReference): Int =
        suspendCancellableCoroutine { continuation ->
            counterRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val current = currentData.getValue(Int::class.java) ?: 0
                    currentData.value = current + 1
                    return Transaction.success(currentData)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    val id = snapshot?.getValue(Int::class.java)
                    when {
                        error != null -> continuation.resumeWithException(error.toException())
                        !committed || id == null ->
                            continuation.resumeWithException(IllegalStateException("Não foi possível gerar o ID"))
                        else -> continuation.resume(id)
                    }
                }
            })
        }

    /**
     * Executa [block] com limite de tempo e devolve [fallback] (registrando o erro no log)
     * se algo der errado. Sem o limite, uma operação sem resposta do servidor (sem internet
     * ou banco desativado) deixaria a tela em "Aguarde…" para sempre.
     */
    private suspend inline fun <T> safeCall(fallback: T, crossinline block: suspend () -> T): T = try {
        withTimeout(TIMEOUT_MS) { block() }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "O Firebase não respondeu em ${TIMEOUT_MS / 1000} s", e)
        fallback
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Erro ao acessar o Firebase", e)
        fallback
    }

    private companion object {
        const val TAG = "FirebaseDatabaseHelper"
        const val TIMEOUT_MS = 15_000L
    }
}
