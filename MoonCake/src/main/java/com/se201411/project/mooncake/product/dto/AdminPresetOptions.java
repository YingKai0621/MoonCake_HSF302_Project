package com.se201411.project.mooncake.product.dto;

import java.util.List;

public record AdminPresetOptions(List<Option> giftSets, List<Option> boxes, List<Option> cakes) {
    public record Option(long id, String label) {
    }
}
