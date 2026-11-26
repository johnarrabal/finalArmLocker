package com.example.finalarmlocker.ui

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.FirebaseDatabaseHelper
import com.example.finalarmlocker.data.FirebaseDatabaseHelper.RegisterUserResult
import com.example.finalarmlocker.data.User
import com.example.finalarmlocker.data.ViaCepApi
import com.example.finalarmlocker.databinding.ActivityRegisterBinding
import com.example.finalarmlocker.location.AddressLocator
import com.example.finalarmlocker.util.CpfValidator
import com.example.finalarmlocker.util.HashUtil
import com.example.finalarmlocker.util.InputValidators
import com.example.finalarmlocker.util.MaskFormatter
import com.example.finalarmlocker.util.clearErrorsOnEdit
import com.example.finalarmlocker.util.toast
import com.example.finalarmlocker.util.trimmedText
import com.example.finalarmlocker.util.validateFields
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Cadastro do morador. O endereço é preenchido de duas formas, sempre só nos campos vazios:
 * - pela localização do aparelho, ao abrir a tela (se houver permissão);
 * - pela API ViaCEP, assim que o CEP é digitado por completo.
 */
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val database = FirebaseDatabaseHelper()
    private val addressLocator by lazy { AddressLocator(this) }
    private var lastCepLookup: String? = null

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) fillAddressFromLocation()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            clearErrorsOnEdit(
                inputLayoutFirstName, inputLayoutLastName, inputLayoutEmail, inputLayoutPassword,
                inputLayoutBirthDate, inputLayoutCpf, inputLayoutPhone, inputLayoutCep,
                inputLayoutState, inputLayoutCity, inputLayoutStreet, inputLayoutApartment,
            )
            editTextCEP.doAfterTextChanged { text ->
                val cep = MaskFormatter.digitsOnly(text ?: "")
                if (cep.length == 8 && cep != lastCepLookup) {
                    lastCepLookup = cep
                    fillAddressFromCep(cep)
                }
            }
            buttonRegister.setOnClickListener { submit() }
        }

        if (savedInstanceState == null) {
            if (addressLocator.hasPermission()) {
                fillAddressFromLocation()
            } else {
                locationPermissionRequest.launch(AddressLocator.PERMISSIONS)
            }
        }
    }

    private fun submit(): Unit = with(binding) {
        val firstName = editTextFirstName.trimmedText()
        val lastName = editTextLastName.trimmedText()
        val email = editTextEmail.trimmedText().lowercase()
        val password = editTextPassword.text?.toString().orEmpty()
        val birthDate = editTextBirthDate.trimmedText()
        val cpf = editTextCPF.trimmedText()
        val phone = editTextPhone.trimmedText()
        val cep = editTextCEP.trimmedText()
        val state = editTextState.trimmedText()
        val city = editTextCity.trimmedText()
        val street = editTextStreet.trimmedText()
        val apartment = editTextApartament.trimmedText()

        val valid = validateFields(
            Triple(inputLayoutFirstName, firstName.isNotEmpty(), R.string.error_required),
            Triple(inputLayoutLastName, lastName.isNotEmpty(), R.string.error_required),
            Triple(inputLayoutEmail, InputValidators.isValidEmail(email), R.string.error_invalid_email),
            Triple(inputLayoutPassword, InputValidators.isValidPassword(password), R.string.error_invalid_password),
            Triple(inputLayoutBirthDate, InputValidators.isValidBirthDate(birthDate), R.string.error_invalid_date),
            Triple(inputLayoutCpf, CpfValidator.isValid(cpf), R.string.error_invalid_cpf),
            Triple(inputLayoutPhone, InputValidators.isValidPhone(phone), R.string.error_invalid_phone),
            Triple(inputLayoutCep, InputValidators.isValidCep(cep), R.string.error_invalid_cep),
            Triple(inputLayoutState, state.isNotEmpty(), R.string.error_required),
            Triple(inputLayoutCity, city.isNotEmpty(), R.string.error_required),
            Triple(inputLayoutStreet, street.isNotEmpty(), R.string.error_required),
            Triple(inputLayoutApartment, apartment.isNotEmpty(), R.string.error_required),
        )
        if (!valid) return@with

        val user = User(
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = HashUtil.hashPassword(password),
            birthDate = birthDate,
            cpf = MaskFormatter.format(MaskFormatter.CPF, cpf),
            cep = MaskFormatter.format(MaskFormatter.CEP, cep),
            street = street,
            city = city,
            state = state,
            phone = phone,
            apartament = apartment,
        )

        setLoading(true)
        lifecycleScope.launch {
            val result = database.registerUser(user)
            setLoading(false)
            when (result) {
                RegisterUserResult.SUCCESS -> {
                    toast(R.string.register_success)
                    finish()
                }
                RegisterUserResult.EMAIL_IN_USE -> showFieldError(inputLayoutEmail, R.string.error_email_in_use)
                RegisterUserResult.CPF_IN_USE -> showFieldError(inputLayoutCpf, R.string.error_cpf_in_use)
                RegisterUserResult.ERROR -> toast(R.string.error_generic)
            }
        }
    }

    private fun fillAddressFromCep(cep: String) {
        lifecycleScope.launch {
            val address = try {
                ViaCepApi.service.getAddress(cep)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao consultar o CEP $cep", e)
                null
            }
            when {
                address == null -> toast(R.string.cep_lookup_failed)
                address.erro == true -> binding.inputLayoutCep.error = getString(R.string.error_cep_not_found)
                else -> {
                    fillIfEmpty(binding.editTextStreet, address.logradouro)
                    fillIfEmpty(binding.editTextCity, address.localidade)
                    fillIfEmpty(binding.editTextState, address.uf)
                }
            }
        }
    }

    private fun fillAddressFromLocation() {
        lifecycleScope.launch {
            val address = addressLocator.currentAddress() ?: return@launch
            fillIfEmpty(binding.editTextCEP, address.cep)
            fillIfEmpty(binding.editTextStreet, address.street)
            fillIfEmpty(binding.editTextCity, address.city)
            fillIfEmpty(binding.editTextState, address.state)
        }
    }

    private fun fillIfEmpty(field: EditText, value: String?) {
        if (field.text.isNullOrBlank() && !value.isNullOrBlank()) field.setText(value)
    }

    private fun showFieldError(layout: TextInputLayout, @StringRes messageRes: Int) {
        layout.error = getString(messageRes)
        layout.editText?.requestFocus()
    }

    private fun setLoading(loading: Boolean) {
        binding.buttonRegister.isEnabled = !loading
        binding.buttonRegister.setText(if (loading) R.string.loading else R.string.action_create_account)
    }

    private companion object {
        const val TAG = "RegisterActivity"
    }
}
