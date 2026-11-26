package com.example.finalarmlocker.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finalarmlocker.R
import com.example.finalarmlocker.databinding.ActivityUserLogBinding
import com.example.finalarmlocker.util.toast

/** Área do morador depois do login. */
class UserLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getIntExtra(IntentKeys.USER_ID, -1)
        val firstName = intent.getStringExtra(IntentKeys.USER_FIRST_NAME).orEmpty()
        val cpf = intent.getStringExtra(IntentKeys.USER_CPF).orEmpty()
        if (userId == -1 || cpf.isEmpty()) {
            toast(R.string.error_session)
            finish()
            return
        }

        binding.welcomeText.text = getString(R.string.welcome_message, firstName)

        binding.userPhoto.setOnClickListener {
            startActivity(
                Intent(this, UserDetailsActivity::class.java).putExtra(IntentKeys.USER_ID, userId)
            )
        }

        binding.buttonPackages.setOnClickListener {
            startActivity(
                Intent(this, TakePackageActivity::class.java).putExtra(IntentKeys.USER_CPF, cpf)
            )
        }

        binding.buttonLogout.setOnClickListener {
            // Volta para a tela inicial que já está aberta, fechando as telas do morador
            val intent = Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            toast(R.string.logout_done)
            finish()
        }
    }
}
