package com.se201411.project.mooncake.product.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class RecipeCakesForm {
    private Map<Long, Integer> quantities = new LinkedHashMap<>();

    public Map<Long, Integer> getQuantities() {
        return quantities;
    }

    public void setQuantities(Map<Long, Integer> quantities) {
        this.quantities = quantities;
    }
}
