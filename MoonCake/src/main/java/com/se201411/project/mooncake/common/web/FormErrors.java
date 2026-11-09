package com.se201411.project.mooncake.common.web;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public final class FormErrors {
    private FormErrors() {
    }

    public static String first(BindingResult result) {
        FieldError error = result.getFieldError();
        if (error == null || error.getDefaultMessage() == null) {
            return "Dữ liệu chưa hợp lệ. Hãy kiểm tra lại.";
        }
        if (error.isBindingFailure()) {
            return "Vui lòng kiểm tra lại ô " + label(error.getField()) + ".";
        }
        return error.getDefaultMessage();
    }

    private static String label(String field) {
        if (field.equals("variantId") || field.equals("productId")) {
            return "sản phẩm";
        }
        if (field.equals("categoryId")) {
            return "danh mục";
        }
        if (field.equals("unitPrice")) {
            return "giá bán";
        }
        if (field.equals("weightGrams")) {
            return "khối lượng";
        }
        if (field.equals("capacityCakes")) {
            return "sức chứa";
        }
        if (field.equals("quantity")) {
            return "số lượng";
        }
        return "đang nhập";
    }
}
