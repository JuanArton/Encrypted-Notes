package com.juanarton.encnotes.core.validation

import com.juanarton.encnotes.R

class UsernameValidation(val username: String) : BaseValidator() {
    override fun validate(): ValidationResult {
        val isValid = username.matches(Regex("^[a-zA-Z0-9]+$"))
        return ValidationResult(
            isValid,
            if (isValid) R.string.validation_username_success else R.string.validation_error_username
        )
    }
}
