package com.juanarton.encnotes.core.validation

import com.juanarton.encnotes.R

class EmptyValidator(val input: String) : BaseValidator() {
    override fun validate(): ValidationResult {
        val isValid = input.isNotEmpty()
        return ValidationResult(
            isValid,
            if (isValid) R.string.not_empty_field else R.string.empty_field
        )
    }
}