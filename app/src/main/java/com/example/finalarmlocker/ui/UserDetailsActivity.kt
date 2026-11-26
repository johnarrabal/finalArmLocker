package com.example.finalarmlocker.ui

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.FirebaseDatabaseHelper
import com.example.finalarmlocker.data.User
import com.example.finalarmlocker.databinding.ActivityUserDetailsBinding
import kotlinx.coroutines.launch

/** Dados cadastrais do morador. */
class UserDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserDetailsBinding
    private val database = FirebaseDatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getIntExtra(IntentKeys.USER_ID, -1)
        if (userId == -1) {
            showError(R.string.error_load_user)
            return
        }

        lifecycleScope.launch {
            val user = database.getUser(userId)
            if (user == null) showError(R.string.error_load_user) else showUser(user)
        }
    }

    private fun showUser(user: User) = with(binding) {
        progressBar.isVisible = false
        contentGroup.isVisible = true
        textViewFullName.text = listOfNotNull(user.firstName, user.lastName).joinToString(" ")
        textViewEmail.text = user.email.orEmpty()
        textViewPhone.text = getString(R.string.detail_phone, user.phone.orDash())
        textViewCPF.text = getString(R.string.detail_cpf, user.cpf.orDash())
        textViewBirthDate.text = getString(R.string.detail_birth_date, user.birthDate.orDash())
        textViewAddress.text = getString(R.string.detail_address, user.street.orDash(), user.apartament.orDash())
        textViewCity.text = getString(R.string.detail_city, user.city.orDash(), user.state.orDash())
        textViewCEP.text = getString(R.string.detail_cep, user.cep.orDash())
    }

    private fun showError(@StringRes messageRes: Int) = with(binding) {
        progressBar.isVisible = false
        contentGroup.isVisible = false
        textViewError.isVisible = true
        textViewError.setText(messageRes)
    }

    private fun String?.orDash() = if (isNullOrBlank()) "—" else this
}
