package com.juanarton.privynote.core.validation

import com.juanarton.privynote.R

abstract class BaseValidator: IValidator {
    companion object {
        fun validate(vararg validators: IValidator): ValidationResult {
            validators.forEachIndexed { index, validator ->
                val result = validator.validate()
                if (!result.isSuccess) return result
                if (validators[index] !is EmptyValidator) {
                    return result
                }
            }

            return ValidationResult(true, R.string.validation_success)
        }
    }
}