package com.example.finalarmlocker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MaskFormatterTest {

    @Test
    fun `formata CPF completo`() {
        assertEquals("529.982.247-25", MaskFormatter.format(MaskFormatter.CPF, "52998224725"))
    }

    @Test
    fun `formata enquanto digita, sem separador sobrando no fim`() {
        assertEquals("123", MaskFormatter.format(MaskFormatter.CPF, "123"))
        assertEquals("123.4", MaskFormatter.format(MaskFormatter.CPF, "1234"))
        assertEquals("123.456.7", MaskFormatter.format(MaskFormatter.CPF, "1234567"))
    }

    @Test
    fun `apagar o ultimo digito remove o separador junto`() {
        // "123.4" -> backspace -> "123." -> deve virar "123"
        assertEquals("123", MaskFormatter.format(MaskFormatter.CPF, "123."))
    }

    @Test
    fun `ignora caracteres que nao sao digitos e reformata texto ja formatado`() {
        assertEquals("01001-000", MaskFormatter.format(MaskFormatter.CEP, "01001-000"))
        assertEquals("01001-000", MaskFormatter.format(MaskFormatter.CEP, "01.001 000"))
    }

    @Test
    fun `corta digitos que passam do tamanho da mascara`() {
        assertEquals("(11) 91234-5678", MaskFormatter.format(MaskFormatter.PHONE, "119123456789999"))
    }

    @Test
    fun `formata data`() {
        assertEquals("31/12/2000", MaskFormatter.format(MaskFormatter.DATE, "31122000"))
    }

    @Test
    fun `texto vazio continua vazio`() {
        assertEquals("", MaskFormatter.format(MaskFormatter.CPF, ""))
    }

    @Test
    fun `digitsOnly remove a mascara`() {
        assertEquals("52998224725", MaskFormatter.digitsOnly("529.982.247-25"))
    }
}
