package com.example.finalarmlocker.util

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout

fun Context.toast(@StringRes messageRes: Int, vararg formatArgs: Any) {
    Toast.makeText(this, getString(messageRes, *formatArgs), Toast.LENGTH_LONG).show()
}

/** Texto do campo, sem espaços nas pontas. */
fun EditText.trimmedText(): String = text?.toString()?.trim().orEmpty()

/** Tira a mensagem de erro do campo assim que o usuário volta a digitar nele. */
fun clearErrorsOnEdit(vararg layouts: TextInputLayout) {
    layouts.forEach { layout ->
        layout.editText?.doAfterTextChanged { layout.error = null }
    }
}

/**
 * Valida vários campos de uma vez: mostra o erro em cada campo inválido, coloca o foco
 * no primeiro deles e devolve `true` só se todos estiverem válidos.
 */
fun Context.validateFields(vararg checks: Triple<TextInputLayout, Boolean, Int>): Boolean {
    var firstInvalid: TextInputLayout? = null
    for ((layout, isValid, errorRes) in checks) {
        layout.error = if (isValid) null else getString(errorRes)
        if (!isValid && firstInvalid == null) firstInvalid = layout
    }
    firstInvalid?.editText?.requestFocus()
    return firstInvalid == null
}
