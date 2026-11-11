package com.se201411.project.mooncake.security.dto;

public class AccountLockForm {

    private boolean cancelUnpaidOrders = true;

    public boolean isCancelUnpaidOrders() {
        return cancelUnpaidOrders;
    }

    public void setCancelUnpaidOrders(boolean cancelUnpaidOrders) {
        this.cancelUnpaidOrders = cancelUnpaidOrders;
    }
}
