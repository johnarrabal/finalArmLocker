package com.example.projetoarmario

import android.util.Log
import com.example.finalarmlocker.HashUtil
import com.google.firebase.database.*

class FirebaseDatabaseHelper {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef: DatabaseReference = database.getReference("users")
    private val packagesRef: DatabaseReference = database.getReference("packages")
    private val userCounterRef: DatabaseReference = database.getReference("counters/userId")
    private val packageCounterRef: DatabaseReference = database.getReference("counters/packageId")

    // Inserir um novo usuário com ID gerado automaticamente
    fun insertUser(user: User, callback: (Boolean) -> Unit) {
        userCounterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                var currentId = mutableData.getValue(Int::class.java) ?: 0
                currentId += 1
                mutableData.value = currentId
                return Transaction.success(mutableData)
            }

            override fun onComplete(databaseError: DatabaseError?, committed: Boolean, dataSnapshot: DataSnapshot?) {
                if (committed) {
                    val userId = dataSnapshot?.getValue(Int::class.java)
                    if (userId != null) {
                        val newUser = user.copy(id = userId)
                        usersRef.child(userId.toString()).setValue(newUser).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("FirebaseDatabaseHelper", "User registered successfully")
                                callback(true)
                            } else {
                                Log.e("FirebaseDatabaseHelper", "Error: ${task.exception?.message}")
                                callback(false)
                            }
                        }
                    } else {
                        Log.e("FirebaseDatabaseHelper", "Failed to generate user ID")
                        callback(false)
                    }
                } else {
                    Log.e("FirebaseDatabaseHelper", "Transaction failed: ${databaseError?.message}")
                    callback(false)
                }
            }
        })
    }

    // Inserir um novo pacote com ID gerado automaticamente
    fun insertPackage(pkg: Package, callback: (Boolean) -> Unit) {
        packagesRef.orderByChild("locker").equalTo(pkg.locker).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.e("FirebaseDatabaseHelper", "Locker already in use")
                    callback(false)
                } else {
                    usersRef.orderByChild("cpf").equalTo(pkg.cpf).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val userSnapshot = dataSnapshot.children.firstOrNull()
                            if (userSnapshot != null) {
                                val user = userSnapshot.getValue(User::class.java)
                                if (user?.apartament == pkg.apartmentNumber) {
                                    packageCounterRef.runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                            var currentId = mutableData.getValue(Int::class.java) ?: 0
                                            currentId += 1
                                            mutableData.value = currentId
                                            return Transaction.success(mutableData)
                                        }

                                        override fun onComplete(
                                            databaseError: DatabaseError?,
                                            committed: Boolean,
                                            dataSnapshot: DataSnapshot?
                                        ) {
                                            if (committed) {
                                                val packageId = dataSnapshot?.getValue(Int::class.java)
                                                if (packageId != null) {
                                                    val packageWithId = pkg.copy(id = packageId)
                                                    packagesRef.child(packageId.toString()).setValue(packageWithId)
                                                        .addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                Log.d("FirebaseDatabaseHelper", "Package registered successfully")
                                                                callback(true)
                                                            } else {
                                                                Log.e("FirebaseDatabaseHelper", "Error: ${task.exception?.message}")
                                                                callback(false)
                                                            }
                                                        }
                                                } else {
                                                    Log.e("FirebaseDatabaseHelper", "Failed to generate package ID")
                                                    callback(false)
                                                }
                                            } else {
                                                Log.e("FirebaseDatabaseHelper", "Transaction failed: ${databaseError?.message}")
                                                callback(false)
                                            }
                                        }
                                    })
                                } else {
                                    Log.e("FirebaseDatabaseHelper", "Apartment number does not match")
                                    callback(false)
                                }
                            } else {
                                Log.e("FirebaseDatabaseHelper", "User with given CPF not found")
                                callback(false)
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                            callback(false)
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(false)
            }
        })
    }

    // Checar unicidade de email e CPF
    private fun checkUniqueFields(email: String?, cpf: String?, callback: (Boolean) -> Unit) {
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    callback(false)
                } else {
                    usersRef.orderByChild("cpf").equalTo(cpf).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            callback(!dataSnapshot.exists())
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                            callback(false)
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(false)
            }
        })
    }

    // Obter pacotes associados a um CPF
    fun getPackagesByCPF(cpf: String, callback: (List<Package>?) -> Unit) {
        packagesRef.orderByChild("cpf").equalTo(cpf).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val packages = mutableListOf<Package>()
                for (packageSnapshot in dataSnapshot.children) {
                    val pkg = packageSnapshot.getValue(Package::class.java)
                    if (pkg != null) {
                        packages.add(pkg)
                    }
                }
                callback(packages.takeIf { it.isNotEmpty() })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(null)
            }
        })
    }

    // Deletar um pacote por ID
    fun deletePackageById(packageId: String, callback: (Boolean) -> Unit) {
        packagesRef.child(packageId).removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FirebaseDatabaseHelper", "Package deleted successfully")
                callback(true)
            } else {
                Log.e("FirebaseDatabaseHelper", "Error: ${task.exception?.message}")
                callback(false)
            }
        }
    }

    fun getPackagesRef(): DatabaseReference {
        return packagesRef
    }


    // Checar login de usuário
    fun checkUser(email: String, password: String, callback: (User?) -> Unit) {
        val hashedPassword = HashUtil.hashPassword(password)
        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user?.password == hashedPassword) {
                        callback(user)
                        return
                    }
                }
                callback(null)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(null)
            }
        })
    }

    // Obter usuário por ID
    fun getUserByID(userId: Int, callback: (User?) -> Unit) {
        usersRef.child(userId.toString()).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                callback(dataSnapshot.getValue(User::class.java))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(null)
            }
        })
    }



    fun addPackagesListener(cpf: String, callback: (List<Package>?) -> Unit) {
        packagesRef.orderByChild("cpf").equalTo(cpf).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val packages = mutableListOf<Package>()
                for (packageSnapshot in dataSnapshot.children) {
                    val pkg = packageSnapshot.getValue(Package::class.java)
                    if (pkg != null) {
                        packages.add(pkg)
                    }
                }
                callback(packages.takeIf { it.isNotEmpty() })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(null)
            }
        })
    }

    fun getAllPackages(callback: (List<Package>?) -> Unit) {
        packagesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val packages = mutableListOf<Package>()
                for (packageSnapshot in dataSnapshot.children) {
                    val pkg = packageSnapshot.getValue(Package::class.java)
                    if (pkg != null) {
                        packages.add(pkg)
                    }
                }
                callback(packages.takeIf { it.isNotEmpty() })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("FirebaseDatabaseHelper", "Error: ${databaseError.message}")
                callback(null)
            }
        })
    }

    // Estruturas de dados
    data class Package(
        val id: Int? = null,
        val apartmentNumber: String? = null,
        val cpf: String? = null,
        val cep: String? = null,
        val street: String? = null,
        val locker: String? = null // Adicione este campo
    )

    data class User(
        val id: Int? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val email: String? = null,
        val password: String? = null,
        val birthDate: String? = null,
        val cpf: String? = null,
        val cep: String? = null,
        val street: String? = null,
        val city: String? = null,
        val state: String? = null,
        val phone: String? = null,
        val apartament: String? = null
    )
}
