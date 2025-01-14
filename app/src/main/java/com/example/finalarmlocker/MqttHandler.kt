package com.example.finalarmlocker

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttHandler {

    private var client: MqttClient? = null

    fun connect(brokerUrl: String, clientId: String, username: String, password: String) {
        try {
            val persistence = MemoryPersistence()
            client = MqttClient(brokerUrl, clientId, persistence)
            val connectOptions = MqttConnectOptions().apply {
                isCleanSession = true
                userName = username
                this.password = password.toCharArray()
            }
            client?.connect(connectOptions)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun publish(topic: String, message: String) {
        try {
            val mqttMessage = MqttMessage(message.toByteArray())
            client?.publish(topic, mqttMessage)
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        try {
            client?.subscribe(topic) { _, message ->
                callback(String(message.payload))
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
}