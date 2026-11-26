package com.example.finalarmlocker.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Validações dos formulários. Não dependem do Android, então têm testes unitários. */
object InputValidators {
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val DATE_REGEX = Regex("^\\d{2}/\\d{2}/\\d{4}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    fun isValidCep(cep: String): Boolean = MaskFormatter.digitsOnly(cep).length == 8

    /** Fixo (10 dígitos) ou celular (11 dígitos), com DDD. */
    fun isValidPhone(phone: String): Boolean = MaskFormatter.digitsOnly(phone).length in 10..11

    /** Data real no formato dd/mm/aaaa, a partir de 1900 e que não esteja no futuro. */
    fun isValidBirthDate(date: String, today: Date = Date()): Boolean {
        if (!DATE_REGEX.matches(date)) return false
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { isLenient = false }
        val parsed = try {
            format.parse(date)
        } catch (e: ParseException) {
            null
        } ?: return false
        val year = date.takeLast(4).toInt()
        return year >= 1900 && !parsed.after(today)
    }
}

/** Validação de CPF pelos dígitos verificadores. */
object CpfValidator {
    fun isValid(cpf: String): Boolean {
        val digits = MaskFormatter.digitsOnly(cpf)
        if (digits.length != 11) return false
        // CPFs com todos os dígitos iguais (000..., 111...) passam na conta, mas não existem
        if (digits.all { it == digits[0] }) return false
        val numbers = digits.map { it - '0' }
        return numbers[9] == checkDigit(numbers, 9) && numbers[10] == checkDigit(numbers, 10)
    }

    private fun checkDigit(numbers: List<Int>, length: Int): Int {
        val sum = (0 until length).sumOf { numbers[it] * (length + 1 - it) }
        val rest = (sum * 10) % 11
        return if (rest == 10) 0 else rest
    }
}
