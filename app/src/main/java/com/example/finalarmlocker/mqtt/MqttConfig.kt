package com.example.finalarmlocker.mqtt

/** Configuração da conexão com o armário físico. Antes estava repetida em três arquivos. */
object MqttConfig {
    const val BROKER_URL = "tcp://broker.emqx.io:1883"
    const val USERNAME = "Armario"
    const val PASSWORD = "1235"

    /** Tópico que o firmware do armário escuta. */
    const val TOPIC_COMMANDS = "api/saida"

    /**
     * Prefixo do client ID. Cada instalação do app usa um ID diferente: o broker derruba
     * a conexão anterior quando outra entra com o mesmo ID (antes todas usavam "Armario").
     */
    const val CLIENT_ID_PREFIX = "ArmLocker-"

    /** QoS 1: o broker confirma o recebimento de cada comando. */
    const val QOS = 1
    const val TIMEOUT_SECONDS = 10
    const val KEEP_ALIVE_SECONDS = 30

    /**
     * Inclui o campo "locker" (compartimento) nos comandos enviados ao armário.
     * Mude para `false` se o firmware não aceitar campos extras no JSON.
     */
    const val INCLUDE_LOCKER = true
}
