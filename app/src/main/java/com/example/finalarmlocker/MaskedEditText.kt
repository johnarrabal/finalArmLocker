package com.example.finalarmlocker

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class MaskedEditText(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {

    private var mask: String? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MaskedEditText,
            0, 0
        ).apply {
            try {
                mask = getString(R.styleable.MaskedEditText_mask)
            } finally {
                recycle()
            }
        }

        addTextChangedListener(object : TextWatcher {
            private var isUpdating: Boolean = false
            private var oldText: String = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                oldText = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                val unmaskedText = s.toString().replace(Regex("[^\\d]"), "")
                val maskedText = StringBuilder()
                var i = 0

                mask?.forEach { char ->
                    if (char != '#' && unmaskedText.length > i) {
                        maskedText.append(char)
                    } else {
                        if (i >= unmaskedText.length) {
                            return
                        }
                        maskedText.append(unmaskedText[i])
                        i++
                    }
                }

                isUpdating = true
                setText(maskedText.toString())
                setSelection(maskedText.length)
            }
        })
    }
}