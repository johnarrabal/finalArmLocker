package com.example.finalarmlocker.util

import java.security.MessageDigest

object HashUtil {
    /**
     * SHA-256 da senha em hexadecimal (64 caracteres).
     * Mantido igual à versão original para que as contas já cadastradas continuem entrando.
     */
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
