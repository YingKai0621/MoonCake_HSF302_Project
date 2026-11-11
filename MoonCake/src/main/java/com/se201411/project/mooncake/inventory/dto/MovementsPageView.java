package com.se201411.project.mooncake.inventory.dto;

import com.se201411.project.mooncake.inventory.dto.AdminInventoryView.Movement;

import java.util.List;

public record MovementsPageView(
    List<Movement> movements,
    int pageNumber
) {
    public MovementsPageView {
        movements = List.copyOf(movements);
    }
}
