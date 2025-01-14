package com.example.finalarmlocker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.finalarmlocker.databinding.ActivityRegisterBinding
import com.example.projetoarmario.FirebaseDatabaseHelper
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var apiViaCep: ApiViaCep
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isCepSetAutomatically = false
    private var isStreetSetAutomatically = false
    private var isCitySetAutomatically = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Retrofit for API calls
        val retrofit = Retrofit.Builder()
            .baseUrl("https://viacep.com.br/ws/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiViaCep = retrofit.create(ApiViaCep::class.java)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            firebaseDatabaseHelper = FirebaseDatabaseHelper()

            // Add TextWatcher to reset automatic flags when CEP changes
            binding.editTextCEP.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    isCepSetAutomatically = false
                    isStreetSetAutomatically = false
                    isCitySetAutomatically = false
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            // Set listener for CEP field
            binding.editTextCEP.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val cep = binding.editTextCEP.text.toString().replace("-", "")
                    if (cep.isNotEmpty() && cep.length == 8 && cep.matches("\\d{8}".toRegex()) && !isCepSetAutomatically) {
                        fetchAddress(cep)
                    } else if (cep.isNotEmpty() && cep.length != 8) {
                        Toast.makeText(this, "CEP inválido, insira um CEP válido de 8 dígitos", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Set register button listener
            binding.buttonRegister.setOnClickListener {
                try {
                    val firstName = binding.editTextFirstName.text.toString()
                    val lastName = binding.editTextLastName.text.toString()
                    val email = binding.editTextEmail.text.toString()
                    val password = binding.editTextPassword.text.toString()
                    val birthDate = binding.editTextBirthDate.text.toString()
                    val cpf = binding.editTextCPF.text.toString()
                    val cep = binding.editTextCEP.text.toString()
                    val street = binding.editTextStreet.text.toString()
                    val city = binding.editTextCity.text.toString()
                    val state = binding.editTextState.text.toString()
                    val phone = binding.editTextPhone.text.toString()
                    val apartament = binding.editTextApartament.text.toString()

                    // Check if all fields are filled
                    if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() ||
                        birthDate.isEmpty() || cpf.isEmpty() || cep.isEmpty() || street.isEmpty() ||
                        city.isEmpty() || state.isEmpty() || phone.isEmpty() || apartament.isEmpty()) {
                        Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Hash the password
                    val hashedPassword = HashUtil.hashPassword(password)

                    // Create a User object with the hashed password
                    val user = FirebaseDatabaseHelper.User(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        password = hashedPassword,
                        birthDate = birthDate,
                        cpf = cpf,
                        cep = cep,
                        street = street,
                        city = city,
                        state = state,
                        phone = phone,
                        apartament = apartament
                    )

                    Log.d("RegisterActivity", "Tentando registrar usuário: $user")

                    // Insert user into Firebase
                    firebaseDatabaseHelper.insertUser(user) { success ->
                        if (success) {
                            Log.d("RegisterActivity", "Usuário registrado com sucesso")
                            Toast.makeText(this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e("RegisterActivity", "Falha no registro do usuário")
                            Toast.makeText(this, "Email ou CPF já existe!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("RegisterActivity", "Erro ao registrar usuário", e)
                    Toast.makeText(this, "Ocorreu um erro durante o registro", Toast.LENGTH_SHORT).show()
                }
            }

            // Request location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                requestLocationUpdates()
            }
        } catch (e: Exception) {
            Log.e("RegisterActivity", "Erro ao inicializar a atividade", e)
            Toast.makeText(this, "Ocorreu um erro durante a inicialização", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch address by CEP using the ViaCep API
    private fun fetchAddress(cep: String) {
        Log.d("RegisterActivity", "Buscando endereço para o CEP: $cep")
        apiViaCep.getAddress(cep).enqueue(object : Callback<AddressResponse> {
            override fun onResponse(call: Call<AddressResponse>, response: Response<AddressResponse>) {
                if (response.isSuccessful) {
                    val address = response.body()
                    Log.d("RegisterActivity", "Resposta do endereço: $address")
                    if (address != null) {
                        Log.d("RegisterActivity", "Cidade recebida: ${address.localidade}")
                        if (!isStreetSetAutomatically && binding.editTextStreet.text.isNullOrEmpty()) {
                            binding.editTextStreet.setText(address.logradouro ?: "")
                            isStreetSetAutomatically = true
                        }
                        if (!isCitySetAutomatically && binding.editTextCity.text.isNullOrEmpty()) {
                            binding.editTextCity.setText(address.localidade ?: "")
                            isCitySetAutomatically = true
                        }
                        if (binding.editTextState.text.isNullOrEmpty()) {
                            binding.editTextState.setText(address.uf ?: "")
                        }
                    } else {
                        Log.e("RegisterActivity", "Endereço não encontrado para o CEP: $cep")
                        Toast.makeText(this@RegisterActivity, "Endereço não encontrado", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("RegisterActivity", "Falha ao buscar endereço para o CEP: $cep, Código de resposta: ${response.code()}")
                    Toast.makeText(this@RegisterActivity, "Falha ao buscar endereço", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AddressResponse>, t: Throwable) {
                Log.e("RegisterActivity", "Erro ao buscar endereço para o CEP: $cep, Erro: ${t.message}")
                Toast.makeText(this@RegisterActivity, "Erro: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Request location updates
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000 // 5 seconds
            fastestInterval = 2000 // 2 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    fetchAddressFromLocation(latitude, longitude)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    // Fetch address from location
    private fun fetchAddressFromLocation(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val cep = address.postalCode
                val street = address.thoroughfare
                val city = address.locality
                val state = address.adminArea

                Log.d("RegisterActivity", "Endereço: $address")
                Log.d("RegisterActivity", "CEP: $cep, Rua: $street, Cidade: $city, Estado: $state")

                if (cep != null && !isCepSetAutomatically) {
                    binding.editTextCEP.setText(cep)
                    isCepSetAutomatically = true
                }
                if (street != null && !isStreetSetAutomatically && binding.editTextStreet.text.isNullOrEmpty()) {
                    binding.editTextStreet.setText(street)
                    isStreetSetAutomatically = true
                }
                if (city != null && !isCitySetAutomatically && binding.editTextCity.text.isNullOrEmpty()) {
                    binding.editTextCity.setText(city)
                    isCitySetAutomatically = true
                }
                if (state != null && binding.editTextState.text.isNullOrEmpty()) {
                    binding.editTextState.setText(state)
                }
            } else {
                Log.e("RegisterActivity", "Nenhum endereço encontrado para a localização fornecida")
                Toast.makeText(this, "Endereço não encontrado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("RegisterActivity", "Geocoder não disponível", e)
            Toast.makeText(this, "Geocoder não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle permission request results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                requestLocationUpdates()
            } else {
                Toast.makeText(this, "Permissão negada", Toast.LENGTH_SHORT).show()
            }
        }
    }
}