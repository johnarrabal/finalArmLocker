package com.example.finalarmlocker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.FirebaseDatabaseHelper
import com.example.finalarmlocker.data.FirebaseDatabaseHelper.RegisterPackageResult
import com.example.finalarmlocker.data.LockerPackage
import com.example.finalarmlocker.data.Lockers
import com.example.finalarmlocker.databinding.ActivityRegisterPackageBinding
import com.example.finalarmlocker.location.AddressLocator
import com.example.finalarmlocker.mqtt.LockerCommand
import com.example.finalarmlocker.mqtt.MqttManager
import com.example.finalarmlocker.util.MaskFormatter
import com.example.finalarmlocker.util.clearErrorsOnEdit
import com.example.finalarmlocker.util.toast
import com.example.finalarmlocker.util.trimmedText
import com.example.finalarmlocker.util.validateFields
import kotlinx.coroutines.launch

/**
 * Tela do entregador: registra a encomenda em um armário livre e avisa o armário por MQTT.
 *
 * A localização (CEP e rua de onde a encomenda foi registrada) é um dado extra: se a
 * permissão for negada ou o GPS não responder, a encomenda é registrada mesmo assim.
 */
class RegisterPackageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterPackageBinding
    private val database = FirebaseDatabaseHelper()
    private val addressLocator by lazy { AddressLocator(this) }
    private lateinit var lockerAdapter: ArrayAdapter<String>

    private var isLoading = false
    private var lockersLoaded = false

    /** Última encomenda registrada nesta tela, usada pelo botão "Fechar armário". */
    private var lastRegistered: LockerPackage? = null

    /** Encomenda esperando a resposta do pedido de permissão de localização. */
    private var pendingPackage: LockerPackage? = null

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Com ou sem permissão, o registro continua
            pendingPackage?.let {
                pendingPackage = null
                register(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lockerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf<String>()).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerLockers.adapter = lockerAdapter

        clearErrorsOnEdit(binding.inputLayoutApartment, binding.inputLayoutCpf)
        binding.buttonRegisterPackage.setOnClickListener { onRegisterClick() }
        binding.buttonCloseLocker.setOnClickListener { onCloseClick() }
        binding.textLockerStatus.setOnClickListener { if (!lockersLoaded) loadFreeLockers() }

        loadFreeLockers()
    }

    private fun loadFreeLockers() {
        binding.textLockerStatus.setText(R.string.lockers_loading)
        binding.textLockerStatus.isVisible = true
        binding.spinnerLockers.isVisible = false
        lifecycleScope.launch {
            val occupied = database.getOccupiedLockers()
            lockersLoaded = occupied != null
            val free = if (occupied == null) emptyList() else Lockers.ALL.filterNot { it in occupied }
            lockerAdapter.clear()
            lockerAdapter.addAll(free)

            binding.spinnerLockers.isVisible = free.isNotEmpty()
            binding.textLockerStatus.isVisible = free.isEmpty()
            binding.textLockerStatus.setText(
                if (occupied == null) R.string.lockers_load_error else R.string.no_free_lockers
            )
            updateButtons()
        }
    }

    private fun onRegisterClick() {
        val apartment = binding.editTextApartmentNumber.trimmedText()
        val cpfDigits = MaskFormatter.digitsOnly(binding.editTextCPF.text ?: "")
        val locker = binding.spinnerLockers.selectedItem as? String

        val valid = validateFields(
            Triple(binding.inputLayoutApartment, apartment.isNotEmpty(), R.string.error_required),
            Triple(binding.inputLayoutCpf, cpfDigits.length == 11, R.string.error_invalid_cpf),
        )
        if (locker == null) toast(R.string.error_select_locker)
        if (!valid || locker == null) return

        val pkg = LockerPackage(
            apartmentNumber = apartment,
            cpf = MaskFormatter.format(MaskFormatter.CPF, cpfDigits),
            locker = locker,
        )
        setLoading(true)
        if (addressLocator.hasPermission()) {
            register(pkg)
        } else {
            pendingPackage = pkg
            locationPermissionRequest.launch(AddressLocator.PERMISSIONS)
        }
    }

    private fun register(form: LockerPackage) {
        lifecycleScope.launch {
            val address = addressLocator.currentAddress()
            val pkg = form.copy(cep = address?.cep, street = address?.street)
            val locker = pkg.locker.orEmpty()

            when (database.registerPackage(pkg)) {
                RegisterPackageResult.SUCCESS -> {
                    lastRegistered = pkg
                    val notified = MqttManager.send(LockerCommand.register(pkg.cpf.orEmpty(), locker))
                    toast(if (notified) R.string.package_success else R.string.package_success_mqtt_error, locker)
                    binding.editTextApartmentNumber.text?.clear()
                    binding.editTextCPF.text?.clear()
                    binding.inputLayoutApartment.error = null
                    binding.inputLayoutCpf.error = null
                    loadFreeLockers()
                }
                RegisterPackageResult.LOCKER_IN_USE -> {
                    toast(R.string.error_locker_in_use)
                    loadFreeLockers()
                }
                RegisterPackageResult.USER_NOT_FOUND ->
                    binding.inputLayoutCpf.error = getString(R.string.error_user_not_found)
                RegisterPackageResult.APARTMENT_MISMATCH ->
                    binding.inputLayoutApartment.error = getString(R.string.error_apartment_mismatch)
                RegisterPackageResult.ERROR -> toast(R.string.error_generic)
            }
            setLoading(false)
        }
    }

    /**
     * Usa o CPF digitado; com o campo vazio, usa a última encomenda registrada nesta tela
     * (o formulário é limpo depois de cada registro).
     */
    private fun onCloseClick() {
        val typedDigits = MaskFormatter.digitsOnly(binding.editTextCPF.text ?: "")
        val last = lastRegistered
        val cpf: String
        val locker: String?
        when {
            typedDigits.length == 11 -> {
                cpf = MaskFormatter.format(MaskFormatter.CPF, typedDigits)
                locker = last?.takeIf { it.cpf == cpf }?.locker
            }
            last?.cpf != null -> {
                cpf = last.cpf
                locker = last.locker
            }
            else -> {
                binding.inputLayoutCpf.error = getString(R.string.close_need_cpf)
                return
            }
        }

        binding.buttonCloseLocker.isEnabled = false
        lifecycleScope.launch {
            val sent = MqttManager.send(LockerCommand.close(cpf, locker))
            binding.buttonCloseLocker.isEnabled = true
            toast(if (sent) R.string.close_sent else R.string.close_error)
        }
    }

    private fun setLoading(loading: Boolean) {
        isLoading = loading
        binding.buttonRegisterPackage.setText(if (loading) R.string.loading else R.string.action_register_package)
        updateButtons()
    }

    private fun updateButtons() {
        binding.buttonRegisterPackage.isEnabled = !isLoading && lockerAdapter.count > 0
    }
}
