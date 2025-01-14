package com.example.finalarmlocker


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.finalarmlocker.databinding.ActivityTakePackageBinding
import com.example.projetoarmario.FirebaseDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TakePackage : AppCompatActivity() {
    private lateinit var binding: ActivityTakePackageBinding
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var packageAdapter: PackageAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var userCpf: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakePackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabaseHelper = FirebaseDatabaseHelper()

        // Obtém o CPF do usuário a partir do Intent
        userCpf = intent.getStringExtra("USER_CPF") ?: ""
        val userName = intent.getStringExtra("USER_FIRST_NAME") ?: ""

        // Define a mensagem de boas-vindas
        binding.welcomeText.text = getString(R.string.welcome_message, userName)

        // Configura o RecyclerView
        binding.recyclerViewPackages.layoutManager = LinearLayoutManager(this)
        packageAdapter = PackageAdapter(firebaseDatabaseHelper, userCpf)
        binding.recyclerViewPackages.adapter = packageAdapter

        // Configura o SwipeRefreshLayout
        swipeRefreshLayout = binding.swipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener {
            fetchUserPackages(userCpf)
        }

        // Busca e exibe os pacotes do usuário
        if (userCpf.isNotEmpty()) {
            fetchUserPackages(userCpf)
        } else {
            showToast("User CPF not found")
        }

        // Adiciona listener para atualizações em tempo real
        firebaseDatabaseHelper.addPackagesListener(userCpf) { packages ->
            if (packages != null) {
                packageAdapter.setPackages(packages)
            } else {
                showToast("No packages found")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Busca e exibe os pacotes do usuário quando a atividade é retomada
        if (userCpf.isNotEmpty()) {
            fetchUserPackages(userCpf)
        }
    }

    private fun fetchUserPackages(cpf: String) {
        // Mostra o indicador de carregamento
        swipeRefreshLayout.isRefreshing = true

        CoroutineScope(Dispatchers.Main).launch {
            firebaseDatabaseHelper.getPackagesByCPF(cpf) { packages ->
                // Esconde o indicador de carregamento
                swipeRefreshLayout.isRefreshing = false

                if (packages != null && packages.isNotEmpty()) {
                    Log.d("TakePackage", "Packages found: ${packages.size}")
                    packageAdapter.setPackages(packages)
                } else {
                    Log.d("TakePackage", "No packages found for CPF: $cpf")
                    showToast("No packages found")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}