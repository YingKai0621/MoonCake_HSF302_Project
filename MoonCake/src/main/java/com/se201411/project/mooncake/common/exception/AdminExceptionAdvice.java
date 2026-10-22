package com.se201411.project.mooncake.common.exception;

import com.se201411.project.mooncake.product.controller.AdminCatalogController;
import com.se201411.project.mooncake.inventory.controller.AdminInventoryController;
import com.se201411.project.mooncake.order.controller.AdminOrderController;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = {AdminCatalogController.class, AdminInventoryController.class, AdminOrderController.class})
public class AdminExceptionAdvice {
    @ExceptionHandler({BusinessRuleViolationException.class, ConcurrentOperationException.class, InsufficientStockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String business(MoonCakeException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "admin/error";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String missing(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "admin/error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String validation(MethodArgumentNotValidException ex, Model model) {
        var errors = new java.util.ArrayList<String>();
        for (var error : ex.getBindingResult().getFieldErrors())
            errors.add(error.getField() + ": " + error.getDefaultMessage());
        model.addAttribute("message", String.join("; ", errors));
        return "admin/error";
    }

    @ExceptionHandler({DataAccessException.class, ArithmeticException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Exception ex, Model model) {
        model.addAttribute("message", "Không thể lưu: dữ liệu trùng, vừa được thay đổi hoặc số lượng vượt giới hạn. Hãy tải lại trang và kiểm tra trước khi thử lại.");
        return "admin/error";
    }
}
