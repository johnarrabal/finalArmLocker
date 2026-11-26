package com.example.finalarmlocker.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ValidatorsTest {

    // CPF

    @Test
    fun `CPF valido com e sem mascara`() {
        assertTrue(CpfValidator.isValid("529.982.247-25"))
        assertTrue(CpfValidator.isValid("52998224725"))
        assertTrue(CpfValidator.isValid("123.456.789-09"))
    }

    @Test
    fun `CPF com digito verificador errado e invalido`() {
        assertFalse(CpfValidator.isValid("529.982.247-24"))
        assertFalse(CpfValidator.isValid("123.456.789-00"))
    }

    @Test
    fun `CPF com todos os digitos iguais e invalido`() {
        assertFalse(CpfValidator.isValid("111.111.111-11"))
        assertFalse(CpfValidator.isValid("00000000000"))
    }

    @Test
    fun `CPF com tamanho errado e invalido`() {
        assertFalse(CpfValidator.isValid(""))
        assertFalse(CpfValidator.isValid("529.982.247"))
    }

    // E-mail e senha

    @Test
    fun `emails`() {
        assertTrue(InputValidators.isValidEmail("maria@exemplo.com"))
        assertTrue(InputValidators.isValidEmail("  maria.silva+armario@exemplo.com.br "))
        assertFalse(InputValidators.isValidEmail("maria@"))
        assertFalse(InputValidators.isValidEmail("maria.exemplo.com"))
        assertFalse(InputValidators.isValidEmail(""))
    }

    @Test
    fun `senha precisa de pelo menos 6 caracteres`() {
        assertTrue(InputValidators.isValidPassword("123456"))
        assertFalse(InputValidators.isValidPassword("12345"))
    }

    // Data de nascimento

    private val today = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("25/11/2026")!!

    @Test
    fun `data de nascimento valida`() {
        assertTrue(InputValidators.isValidBirthDate("01/01/2000", today))
        assertTrue(InputValidators.isValidBirthDate("29/02/2000", today))
    }

    @Test
    fun `data inexistente, incompleta ou no futuro e invalida`() {
        assertFalse(InputValidators.isValidBirthDate("31/02/2000", today))
        assertFalse(InputValidators.isValidBirthDate("29/02/2001", today))
        assertFalse(InputValidators.isValidBirthDate("01/01/20", today))
        assertFalse(InputValidators.isValidBirthDate("01/01/2030", today))
        assertFalse(InputValidators.isValidBirthDate("01/01/1800", today))
    }

    // CEP e telefone

    @Test
    fun `cep e telefone`() {
        assertTrue(InputValidators.isValidCep("01001-000"))
        assertFalse(InputValidators.isValidCep("01001-00"))
        assertTrue(InputValidators.isValidPhone("(11) 91234-5678"))
        assertTrue(InputValidators.isValidPhone("1132345678"))
        assertFalse(InputValidators.isValidPhone("91234-5678"))
    }
}
