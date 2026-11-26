package com.example.finalarmlocker.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalarmlocker.R
import com.example.finalarmlocker.data.FirebaseDatabaseHelper
import com.example.finalarmlocker.data.FirebaseDatabaseHelper.LoginResult
import com.example.finalarmlocker.data.User
import com.example.finalarmlocker.databinding.ActivityLoginBinding
import com.example.finalarmlocker.util.clearErrorsOnEdit
import com.example.finalarmlocker.util.toast
import com.example.finalarmlocker.util.trimmedText
import com.example.finalarmlocker.util.validateFields
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val database = FirebaseDatabaseHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clearErrorsOnEdit(binding.inputLayoutEmail, binding.inputLayoutPassword)
        binding.buttonLogin.setOnClickListener { attemptLogin() }
        binding.editTextPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                true
            } else {
                false
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.editTextEmail.trimmedText()
        val password = binding.editTextPassword.text?.toString().orEmpty()
        val valid = validateFields(
            Triple(binding.inputLayoutEmail, email.isNotEmpty(), R.string.error_required),
            Triple(binding.inputLayoutPassword, password.isNotEmpty(), R.string.error_required),
        )
        if (!valid) return

        setLoading(true)
        lifecycleScope.launch {
            val result = database.login(email, password)
            setLoading(false)
            when (result) {
                is LoginResult.Success -> openHome(result.user)
                LoginResult.InvalidCredentials -> binding.inputLayoutPassword.error = getString(R.string.login_invalid)
                LoginResult.Error -> toast(R.string.error_generic)
            }
        }
    }

    private fun openHome(user: User) {
        val intent = Intent(this, UserLogActivity::class.java)
            .putExtra(IntentKeys.USER_ID, user.id ?: -1)
            .putExtra(IntentKeys.USER_FIRST_NAME, user.firstName)
            .putExtra(IntentKeys.USER_CPF, user.cpf)
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.buttonLogin.isEnabled = !loading
        binding.buttonLogin.setText(if (loading) R.string.loading else R.string.action_login)
    }
}
