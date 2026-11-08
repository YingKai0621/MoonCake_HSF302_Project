package com.se201411.project.mooncake.order.repository;
import com.se201411.project.mooncake.entity.SalesOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public interface VnpayRepository {
    SalesOrder lockOwned(String code,long customer);
    SalesOrder lock(long id);
    Object[] attempt(String ref);
    boolean pending(long order);
    record Pending(String ref, BigDecimal amount, LocalDateTime createdAt) {}
    Pending pendingAttempt(long order);
    void create(String ref,long order,BigDecimal amount,LocalDateTime now);
    void finish(String ref,String status,String transaction);
}
