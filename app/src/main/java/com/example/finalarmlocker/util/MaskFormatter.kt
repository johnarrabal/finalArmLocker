package com.example.finalarmlocker.util

/**
 * Aplica máscaras de digitação, em que cada `#` é um dígito (ex.: "###.###.###-##").
 * Fica separado da View para poder ser testado com testes unitários comuns.
 */
object MaskFormatter {
    const val CPF = "###.###.###-##"
    const val CEP = "#####-###"
    const val PHONE = "(##) #####-####"
    const val DATE = "##/##/####"

    fun digitsOnly(text: CharSequence): String = text.filter { it.isDigit() }.toString()

    /**
     * Formata [input] com a [mask], usando só os dígitos.
     * Os separadores só entram quando há um dígito depois deles. Assim, apagar com
     * backspace funciona normalmente e o texto nunca termina em "." ou "-".
     */
    fun format(mask: String, input: CharSequence): String {
        val digits = digitsOnly(input)
        val result = StringBuilder()
        var index = 0
        for (maskChar in mask) {
            if (index >= digits.length) break
            if (maskChar == '#') {
                result.append(digits[index])
                index++
            } else {
                result.append(maskChar)
            }
        }
        return result.toString()
    }
}
