package com.se201411.project.mooncake.cart.dto;

public record CustomBoxFormPage(
    CustomBoxPageView builder,
    boolean editing,
    Long configurationId
) {
}
