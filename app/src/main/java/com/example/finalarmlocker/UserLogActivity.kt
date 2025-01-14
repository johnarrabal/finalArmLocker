package com.example.finalarmlocker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.finalarmlocker.databinding.ActivityUserLogBinding
import com.squareup.picasso.Picasso

class UserLogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserLogBinding.inflate(layoutInflater) // Infla o layout usando View Binding
        setContentView(binding.root) // Define o layout da atividade

        // Obtém o nome do usuário a partir do Intent e define a mensagem de boas-vindas
        val userName = intent.getStringExtra("USER_FIRST_NAME") ?: ""
        binding.welcomeText.text = getString(R.string.welcome_message, userName)

        // Obtém a URI da foto do usuário a partir do Intent e carrega a imagem usando Picasso
        val photoUriString = intent.getStringExtra("USER_PHOTO_URI")
        if (photoUriString != null) {
            Picasso.get().load(photoUriString).into(binding.userPhoto)
        }

        // Configura o clique na foto do usuário para abrir a UserDetailsActivity
        binding.userPhoto.setOnClickListener {
            val userId = intent.getIntExtra("USER_ID", -1)
            if (userId != -1) {
                val intent = Intent(this, UserDetailsActivity::class.java)
                intent.putExtra("USER_ID", userId)
                startActivity(intent)
            } else {
                Log.e("UserLogActivity", "User ID is invalid")
            }
        }

        // Configura o botão de pacotes para abrir a TakePackage
        binding.buttonPackages.setOnClickListener {
            val userCpf = intent.getStringExtra("USER_CPF") ?: ""
            if (userCpf.isNotEmpty()) {
                val intent = Intent(this, TakePackage::class.java)
                intent.putExtra("USER_CPF", userCpf)
                intent.putExtra("USER_FIRST_NAME", userName)
                intent.putExtra("USER_ID", intent.getIntExtra("USER_ID", -1))
                startActivity(intent)
            } else {
                Log.e("UserLogActivity", "User CPF is invalid")
            }
        }

        // Configura o botão de logout para voltar à MainActivity
        binding.buttonLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Ensure the current activity is finished
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
    }
}