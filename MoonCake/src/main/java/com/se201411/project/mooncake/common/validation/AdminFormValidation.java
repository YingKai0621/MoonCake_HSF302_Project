package com.se201411.project.mooncake.common.validation;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AdminFormValidation {

    private final Validator validator;

    public AdminFormValidation(Validator validator) {
        this.validator = validator;
    }

    public void validate(Object form) {
        if (form == null) {
            throw new BusinessRuleViolationException("Thiếu dữ liệu biểu mẫu.");
        }
        Set<ConstraintViolation<Object>> violations = validator.validate(form);
        if (!violations.isEmpty()) {
            ConstraintViolation<Object> violation = violations.iterator().next();
            throw new BusinessRuleViolationException(violation.getPropertyPath() + ": " + violation.getMessage());
        }
    }
}
