package com.example.finalarmlocker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HashUtilTest {

    @Test
    fun `gera o SHA-256 em hexadecimal, igual a versao original`() {
        // Valor conhecido do SHA-256 de "123456". Se mudar, as contas antigas param de entrar.
        assertEquals(
            "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92",
            HashUtil.hashPassword("123456"),
        )
    }
}
