package com.se201411.project.mooncake.order.service;

public interface OrderCancellationService {

    void cancelCurrentCustomerOrder(String orderCode);

    void cancelForAdmin(long orderId);
}
