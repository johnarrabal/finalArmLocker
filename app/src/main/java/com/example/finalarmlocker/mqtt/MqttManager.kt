package com.example.finalarmlocker.mqtt

import android.util.Log
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Uma única conexão MQTT para o app inteiro.
 *
 * Todo o trabalho de rede roda em uma thread própria (fora da thread principal, então a
 * interface não trava). A conexão é aberta na primeira vez que um comando é enviado e
 * refeita sozinha se tiver caído.
 */
object MqttManager {
    private const val TAG = "MqttManager"

    private val dispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mqtt-worker").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    private val clientId = MqttConfig.CLIENT_ID_PREFIX + UUID.randomUUID().toString().take(8)

    /** Só é acessado dentro de [dispatcher], então não precisa de sincronização. */
    private var client: MqttClient? = null

    /**
     * Publica o comando no tópico do armário.
     * @return `true` se o broker confirmou o recebimento.
     */
    suspend fun send(command: LockerCommand): Boolean = withContext(dispatcher) {
        val payload = command.toJson()
        try {
            val message = MqttMessage(payload.toByteArray()).apply { qos = MqttConfig.QOS }
            connectedClient().publish(MqttConfig.TOPIC_COMMANDS, message)
            Log.d(TAG, "Enviado em ${MqttConfig.TOPIC_COMMANDS}: $payload")
            true
        } catch (e: MqttException) {
            Log.e(TAG, "Falha ao enviar $payload", e)
            false
        }
    }

    private fun connectedClient(): MqttClient {
        val current = client ?: MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence()).also {
            it.timeToWait = MqttConfig.TIMEOUT_SECONDS * 1_000L
            client = it
        }
        if (!current.isConnected) {
            current.connect(
                MqttConnectOptions().apply {
                    isCleanSession = true
                    userName = MqttConfig.USERNAME
                    password = MqttConfig.PASSWORD.toCharArray()
                    connectionTimeout = MqttConfig.TIMEOUT_SECONDS
                    keepAliveInterval = MqttConfig.KEEP_ALIVE_SECONDS
                }
            )
        }
        return current
    }
}
