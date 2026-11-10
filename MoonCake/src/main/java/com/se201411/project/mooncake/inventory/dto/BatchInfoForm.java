package com.se201411.project.mooncake.inventory.dto;

import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record BatchInfoForm(
    @Size(max = 80, message = "Mã lô tối đa 80 ký tự.")
    String batchCode,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate producedOn,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate expiresOn
) {
}
