package com.example.finalarmlocker.mqtt

import org.junit.Assert.assertEquals
import org.junit.Test

class LockerCommandTest {

    @Test
    fun `sem o campo locker o JSON e identico ao da versao original`() {
        assertEquals(
            """{"cpf": "529.982.247-25", "acao": "abrir", "tipo": "cliente"}""",
            LockerCommand.open("529.982.247-25", "Locker 1").toJson(includeLocker = false),
        )
        assertEquals(
            """{"cpf": "529.982.247-25", "acao": "register", "tipo": "package"}""",
            LockerCommand.register("529.982.247-25", "Locker 1").toJson(includeLocker = false),
        )
        assertEquals(
            """{"cpf": "529.982.247-25", "acao": "fechar", "tipo": "package"}""",
            LockerCommand.close("529.982.247-25", null).toJson(includeLocker = false),
        )
    }

    @Test
    fun `inclui o locker no fim quando habilitado`() {
        assertEquals(
            """{"cpf": "529.982.247-25", "acao": "abrir", "tipo": "cliente", "locker": "Locker 2"}""",
            LockerCommand.open("529.982.247-25", "Locker 2").toJson(includeLocker = true),
        )
    }

    @Test
    fun `sem locker conhecido o campo nao aparece`() {
        assertEquals(
            """{"cpf": "529.982.247-25", "acao": "fechar", "tipo": "package"}""",
            LockerCommand.close("529.982.247-25", null).toJson(includeLocker = true),
        )
    }

    @Test
    fun `aspas sao escapadas`() {
        assertEquals(
            """{"cpf": "1\"2", "acao": "abrir", "tipo": "cliente"}""",
            LockerCommand.open("1\"2", null).toJson(),
        )
    }
}
