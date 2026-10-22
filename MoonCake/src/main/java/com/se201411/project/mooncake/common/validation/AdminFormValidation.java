package com.se201411.project.mooncake.common.validation;

import jakarta.validation.Validator;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Component;

@Component
public class AdminFormValidation {
    private final Validator validator;

    public AdminFormValidation(Validator validator) {
        this.validator = validator;
    }

    public void validate(Object form) {
        if (form == null) throw new BusinessRuleViolationException("Thiếu dữ liệu biểu mẫu.");
        var violations = validator.validate(form);
        if (!violations.isEmpty()) {
            var violation = violations.iterator().next();
            throw new BusinessRuleViolationException(violation.getPropertyPath() + ": " + violation.getMessage());
        }
    }
}
