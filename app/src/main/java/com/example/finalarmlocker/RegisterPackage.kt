package com.example.finalarmlocker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.finalarmlocker.databinding.ActivityRegisterPackageBinding
import com.example.projetoarmario.FirebaseDatabaseHelper
import com.google.android.gms.location.*
import java.util.Locale

class RegisterPackage : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterPackageBinding
    private lateinit var firebaseDatabaseHelper: FirebaseDatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isRegistering = false
    private lateinit var locationCallback: LocationCallback
    private lateinit var mqttHandler: MqttHandler
    private val allLockers = listOf("Locker 1", "Locker 2", "Locker 3")
    private val availableLockers = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPackageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabaseHelper = FirebaseDatabaseHelper()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mqttHandler = MqttHandler()

        val brokerUrl = "tcp://broker.emqx.io:1883"
        val clientId = "Armario"
        val username = "Armario"
        val password = "1235"
        mqttHandler.connect(brokerUrl, clientId, username, password)

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableLockers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLockers.adapter = adapter

        fetchAvailableLockers()

        binding.buttonRegisterPackage.setOnClickListener {
            if (isRegistering) return@setOnClickListener

            val apartmentNumber = binding.editTextApartmentNumber.text.toString()
            val cpf = binding.editTextCPF.text.toString()
            val selectedLocker = binding.spinnerLockers.selectedItem as String

            if (apartmentNumber.isNotEmpty() && cpf.isNotEmpty() && selectedLocker.isNotEmpty()) {
                binding.buttonRegisterPackage.isEnabled = false
                isRegistering = true
                requestLocationPermission(apartmentNumber, cpf, selectedLocker)
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonCloseLocker.setOnClickListener {
            val cpf = binding.editTextCPF.text.toString()
            if (cpf.isNotEmpty()) {
                sendMqttMessage(cpf, "fechar", "package")
                Toast.makeText(this, "Mensagem de fechamento enviada.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "CPF não encontrado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAvailableLockers() {
        firebaseDatabaseHelper.getAllPackages { packages ->
            val usedLockers = packages?.map { it.locker } ?: emptyList()
            availableLockers.clear()
            availableLockers.addAll(allLockers.filter { it !in usedLockers })
            adapter.notifyDataSetChanged()
        }
    }

    private fun requestLocationPermission(apartmentNumber: String, cpf: String, selectedLocker: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchAddress(apartmentNumber, cpf, selectedLocker)
        }
    }

    private fun fetchAddress(apartmentNumber: String, cpf: String, selectedLocker: String) {
        val locationRequest = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    try {
                        val geocoder = Geocoder(this@RegisterPackage, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val cep = address.postalCode ?: "CEP não encontrado"
                            val street = address.thoroughfare ?: "Rua não encontrada"

                            val pkg = FirebaseDatabaseHelper.Package(
                                apartmentNumber = apartmentNumber,
                                cpf = cpf,
                                cep = cep,
                                street = street,
                                locker = selectedLocker
                            )

                            fusedLocationClient.removeLocationUpdates(locationCallback)
                            registerPackage(pkg, cpf, selectedLocker)
                        } else {
                            onRegistrationFailed("Endereço não encontrado.")
                        }
                    } catch (e: Exception) {
                        onRegistrationFailed("Erro ao processar localização: ${e.message}")
                        Log.e("RegisterPackage", "Erro ao buscar endereço", e)
                    }
                } else {
                    onRegistrationFailed("Localização indisponível.")
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun registerPackage(pkg: FirebaseDatabaseHelper.Package, cpf: String, selectedLocker: String) {
        firebaseDatabaseHelper.insertPackage(pkg) { success ->
            binding.buttonRegisterPackage.isEnabled = true
            isRegistering = false
            if (success) {
                Toast.makeText(this, "Pacote registrado com sucesso!", Toast.LENGTH_SHORT).show()
                clearInputFields()
                sendMqttMessage(cpf, "register", "package")
                availableLockers.remove(selectedLocker)
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(this, "Falha ao registrar o pacote. Verifique os dados.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMqttMessage(cpf: String, acao: String, tipo: String) {
        val message = """{"cpf": "$cpf", "acao": "$acao", "tipo": "$tipo"}"""
        mqttHandler.publish("api/saida", message)
        Log.d("RegisterPackage", "Sent MQTT message: $message")
    }

    private fun onRegistrationFailed(message: String) {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        binding.buttonRegisterPackage.isEnabled = true
        isRegistering = false
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun clearInputFields() {
        binding.editTextApartmentNumber.text.clear()
        binding.editTextCPF.text?.clear()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão concedida.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão negada.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}