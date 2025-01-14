package com.example.finalarmlocker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.util.Log
import com.example.finalarmlocker.databinding.ActivityUserDetailsBinding
import com.example.projetoarmario.FirebaseDatabaseHelper

class UserDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserDetailsBinding
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabaseHelper = FirebaseDatabaseHelper()

        val userId = intent.getIntExtra("USER_ID", -1)
        Log.d("UserDetailsActivity", "Received user ID: $userId")

        if (userId != -1) {
            firebaseDatabaseHelper.getUserByID(userId) { user ->
                if (user != null) {
                    binding.textViewFirstName.text = user.firstName
                    binding.textViewLastName.text = user.lastName
                    binding.textViewBirthDate.text = user.birthDate
                    binding.textViewCPF.text = user.cpf
                    binding.textViewCEP.text = user.cep
                    binding.textViewStreet.text = user.street
                    binding.textViewCity.text = user.city
                    binding.textViewState.text = user.state
                    binding.textViewPhone.text = user.phone
                    binding.textViewEmail.text = user.email
                    binding.textViewApartament.text = user.apartament
                } else {
                    binding.textViewFirstName.text = "Usuário não encontrado"
                }
            }
        } else {
            binding.textViewFirstName.text = "ID do usuário não fornecido"
        }
    }
}