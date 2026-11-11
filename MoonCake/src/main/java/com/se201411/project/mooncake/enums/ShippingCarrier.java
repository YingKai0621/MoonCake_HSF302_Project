package com.se201411.project.mooncake.enums;

public enum ShippingCarrier {
    GHN("Giao Hàng Nhanh", true),
    GHTK("Giao Hàng Tiết Kiệm", true),
    VIETTEL_POST("Viettel Post", true),
    STORE("Cửa hàng tự giao", false);

    private final String label;
    private final boolean trackingCodeRequired;

    ShippingCarrier(String label, boolean trackingCodeRequired) {
        this.label = label;
        this.trackingCodeRequired = trackingCodeRequired;
    }

    public String label() {
        return label;
    }

    public boolean trackingCodeRequired() {
        return trackingCodeRequired;
    }
}
