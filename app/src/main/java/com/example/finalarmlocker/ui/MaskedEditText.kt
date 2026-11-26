package com.example.finalarmlocker.ui

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import androidx.core.widget.doAfterTextChanged
import com.example.finalarmlocker.R
import com.example.finalarmlocker.util.MaskFormatter
import com.google.android.material.textfield.TextInputEditText

/**
 * Campo de texto que aplica uma máscara enquanto o usuário digita.
 * Uso no XML: `app:mask="###.###.###-##"` (cada `#` é um dígito).
 *
 * A versão anterior só formatava quando o campo estava completo; agora a formatação
 * acompanha a digitação. As regras ficam em [MaskFormatter].
 */
class MaskedEditText : TextInputEditText {

    private var mask: String? = null
    private var isFormatting = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup(attrs)
    }

    private fun setup(attrs: AttributeSet?) {
        mask = context.obtainStyledAttributes(attrs, R.styleable.MaskedEditText).use {
            it.getString(R.styleable.MaskedEditText_mask)
        }
        doAfterTextChanged { editable ->
            val currentMask = mask
            if (isFormatting || editable == null || currentMask == null) return@doAfterTextChanged
            val formatted = MaskFormatter.format(currentMask, editable)
            if (formatted != editable.toString()) {
                isFormatting = true
                setText(formatted)
                setSelection(formatted.length)
                isFormatting = false
            }
        }
    }
}
