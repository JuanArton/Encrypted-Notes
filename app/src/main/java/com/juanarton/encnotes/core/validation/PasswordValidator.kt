package com.juanarton.encnotes.core.validation

import com.juanarton.encnotes.R

class PasswordValidator(val password: String) : BaseValidator() {
    private val minPasswordLength = 6
    private val maxPasswordLength = 12

    override fun validate(): ValidationResult {
        if (password.length < minPasswordLength || password.length > maxPasswordLength)
            return ValidationResult(false, R.string.validation_error_password_length)

        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val strength = when {
            !hasUppercase && !hasDigit && !hasSpecialChar -> R.string.weak_password
            hasUppercase && hasLowercase && hasDigit && !hasSpecialChar -> R.string.weak_password
            hasUppercase && hasLowercase && !hasDigit && !hasSpecialChar -> R.string.weak_password
            hasUppercase && hasDigit && !hasSpecialChar -> R.string.medium_password
            hasUppercase && hasDigit && hasSpecialChar -> {
                val strong = password.count { it.isUpperCase() } > 1 &&
                        password.count { it.isDigit() } > 1 &&
                        password.count { !it.isLetterOrDigit() } > 1
                if (strong) R.string.strong_password else R.string.medium_password
            }
            else -> R.string.weak_password
        }

        return ValidationResult(true, strength)
    }
}