package com.juanarton.privynote.core.validation

import android.text.TextUtils
import com.juanarton.privynote.R

class EmailValidator(val email: String) : BaseValidator() {
    override fun validate(): ValidationResult {
        val isValid =
            !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
                .matches()
        return ValidationResult(
            isValid,
            if (isValid) R.string.validation_email_success else R.string.validation_error_email
        )
    }
}