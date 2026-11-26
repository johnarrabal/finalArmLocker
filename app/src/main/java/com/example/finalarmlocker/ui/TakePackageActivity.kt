package com.example.finalarmlocker.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.FirebaseDatabaseHelper
import com.example.finalarmlocker.data.LockerPackage
import com.example.finalarmlocker.databinding.ActivityTakePackageBinding
import com.example.finalarmlocker.mqtt.LockerCommand
import com.example.finalarmlocker.mqtt.MqttManager
import com.example.finalarmlocker.util.toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * Encomendas do morador, atualizadas em tempo real.
 *
 * Retirada: primeiro manda o armário abrir e, só se o comando chegou ao broker, apaga a
 * encomenda do banco. Antes era o contrário: se o comando falhasse, a encomenda sumia
 * da lista e o armário continuava fechado.
 */
class TakePackageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTakePackageBinding
    private val database = FirebaseDatabaseHelper()
    private val adapter = PackageAdapter(onWithdrawClick = ::confirmWithdraw)
    private lateinit var userCpf: String
    private var withdrawInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakePackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userCpf = intent.getStringExtra(IntentKeys.USER_CPF).orEmpty()
        if (userCpf.isEmpty()) {
            toast(R.string.error_session)
            finish()
            return
        }

        binding.recyclerViewPackages.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPackages.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener { refresh() }

        // Carga inicial com limite de tempo (mostra erro se o servidor não responder)
        binding.swipeRefreshLayout.isRefreshing = true
        refresh()

        // Depois, escuta mudanças em tempo real, só enquanto a tela está visível.
        // O listener do Firebase é removido sozinho quando a tela sai de cena.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                database.observePackagesByCpf(userCpf)
                    .catch { error ->
                        Log.e(TAG, "Erro ao observar encomendas", error)
                        binding.swipeRefreshLayout.isRefreshing = false
                        toast(R.string.error_generic)
                    }
                    .collect { showPackages(it) }
            }
        }
    }

    private fun refresh() {
        lifecycleScope.launch {
            val packages = database.getPackagesByCpf(userCpf)
            binding.swipeRefreshLayout.isRefreshing = false
            if (packages == null) toast(R.string.error_generic) else showPackages(packages)
        }
    }

    private fun showPackages(packages: List<LockerPackage>) {
        if (!withdrawInProgress) binding.swipeRefreshLayout.isRefreshing = false
        adapter.submitList(packages.sortedBy { it.locker })
        binding.textEmpty.isVisible = packages.isEmpty()
    }

    private fun confirmWithdraw(pkg: LockerPackage) {
        if (withdrawInProgress) return
        val locker = pkg.locker ?: getString(R.string.locker_unknown)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.withdraw_confirm_title)
            .setMessage(getString(R.string.withdraw_confirm_message, locker))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_withdraw) { _, _ -> withdraw(pkg, locker) }
            .show()
    }

    private fun withdraw(pkg: LockerPackage, lockerName: String) {
        val id = pkg.id ?: return
        withdrawInProgress = true
        binding.swipeRefreshLayout.isRefreshing = true
        lifecycleScope.launch {
            val opened = MqttManager.send(LockerCommand.open(pkg.cpf ?: userCpf, pkg.locker))
            when {
                !opened -> toast(R.string.withdraw_mqtt_error)
                database.deletePackage(id) -> toast(R.string.withdraw_success, lockerName)
                else -> toast(R.string.withdraw_delete_error)
            }
            withdrawInProgress = false
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private companion object {
        const val TAG = "TakePackageActivity"
    }
}
