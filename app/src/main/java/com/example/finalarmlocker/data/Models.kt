package com.example.finalarmlocker.data

/**
 * Morador cadastrado (nó `users/{id}` do Firebase).
 *
 * Os nomes das propriedades são exatamente os gravados no banco. Não renomeie nenhum
 * (inclusive `apartament`, com essa grafia) sem migrar os dados já existentes.
 * Os valores padrão geram o construtor vazio que o Firebase precisa para ler os dados.
 */
data class User(
    val id: Int? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    /** Hash SHA-256 da senha (nunca a senha em texto puro). */
    val password: String? = null,
    /** Formato dd/mm/aaaa. */
    val birthDate: String? = null,
    /** Formato 000.000.000-00. É o que liga o morador às encomendas. */
    val cpf: String? = null,
    val cep: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val phone: String? = null,
    /** Apartamento / complemento. O entregador precisa informar o mesmo valor. */
    val apartament: String? = null,
)

/**
 * Encomenda guardada em um armário (nó `packages/{id}` do Firebase).
 */
data class LockerPackage(
    val id: Int? = null,
    val apartmentNumber: String? = null,
    /** CPF do morador, no formato 000.000.000-00. */
    val cpf: String? = null,
    /** CEP e rua de onde o entregador registrou a encomenda (quando a localização está disponível). */
    val cep: String? = null,
    val street: String? = null,
    /** Compartimento ocupado, por exemplo "Locker 1". */
    val locker: String? = null,
)

/** Compartimentos que existem no armário físico. */
object Lockers {
    val ALL = listOf("Locker 1", "Locker 2", "Locker 3")
}
