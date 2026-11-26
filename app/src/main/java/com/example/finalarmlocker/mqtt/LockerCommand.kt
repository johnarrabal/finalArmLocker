package com.example.finalarmlocker.mqtt

/**
 * Comando enviado ao armário físico.
 *
 * O JSON mantém o formato original (`{"cpf": "...", "acao": "...", "tipo": "..."}`, com
 * os mesmos espaços e a mesma ordem dos campos) para continuar compatível com o firmware.
 * O campo `locker` é novo e só entra quando conhecido e habilitado em [MqttConfig.INCLUDE_LOCKER].
 */
data class LockerCommand(
    val cpf: String,
    val action: Action,
    val type: Type,
    val locker: String? = null,
) {
    enum class Action(val value: String) { REGISTER("register"), CLOSE("fechar"), OPEN("abrir") }

    enum class Type(val value: String) { PACKAGE("package"), CLIENT("cliente") }

    fun toJson(includeLocker: Boolean = MqttConfig.INCLUDE_LOCKER): String = buildString {
        append("{\"cpf\": \"").append(escape(cpf)).append('"')
        append(", \"acao\": \"").append(action.value).append('"')
        append(", \"tipo\": \"").append(type.value).append('"')
        if (includeLocker && locker != null) {
            append(", \"locker\": \"").append(escape(locker)).append('"')
        }
        append('}')
    }

    private fun escape(value: String) = value.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        /** Entregador registrou uma encomenda. */
        fun register(cpf: String, locker: String?) = LockerCommand(cpf, Action.REGISTER, Type.PACKAGE, locker)

        /** Entregador pediu para fechar o armário. */
        fun close(cpf: String, locker: String?) = LockerCommand(cpf, Action.CLOSE, Type.PACKAGE, locker)

        /** Morador vai retirar a encomenda. */
        fun open(cpf: String, locker: String?) = LockerCommand(cpf, Action.OPEN, Type.CLIENT, locker)
    }
}
