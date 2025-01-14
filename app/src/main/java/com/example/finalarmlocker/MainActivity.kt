package com.example.finalarmlocker

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.finalarmlocker.databinding.ActivityMainBinding
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mqttHandler: MqttHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita o modo de borda a borda para a atividade
        FirebaseDatabase.getInstance().setPersistenceEnabled(true) // Habilita a persistência offline do Firebase
        binding = ActivityMainBinding.inflate(layoutInflater) // Infla o layout usando View Binding
        setContentView(binding.root) // Define o layout da atividade

        // Ajusta o padding da view principal para considerar as barras do sistema
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializa o MqttHandler
        mqttHandler = MqttHandler()

        // Conecta ao broker MQTT
        val brokerUrl = "tcp://broker.emqx.io:1883"
        val clientId = "Armario"
        val username = "Armario"
        val password = "1235"
        mqttHandler.connect(brokerUrl, clientId, username, password)

        // Inscreve-se no tópico MQTT
        mqttHandler.subscribe("api/saida") { message ->
            runOnUiThread {
                // Manipula a mensagem recebida (por exemplo, atualiza a UI ou armazena os dados)
                println("Received message: $message")
            }
        }

        // Exemplo: Publica uma mensagem no tópico MQTT
        mqttHandler.publish("api/entrada", """{"cpf": "12345678900", "acao": "open", "tipo": "package"}""")

        // Configura o botão de login para abrir a LoginActivity
        binding.buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão de registro para abrir a RegisterActivity
        binding.buttonRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Configura o botão de registro de pacote para abrir a RegisterPackage
        binding.buttonRegisterPackage.setOnClickListener {
            val intent = Intent(this, RegisterPackage::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desconecta do broker MQTT
        mqttHandler.disconnect()
    }
}