package com.se201411.project.mooncake.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

public interface VnpayGateway {

    boolean enabled();

    void requireConfigured();

    boolean valid(Map<String, String> values);

    Map<String, String> query(String ref, LocalDateTime created, String ip, Instant now);

    String url(String ref, BigDecimal amount, String ip, Instant now);

    long attemptLifetimeSeconds();

    long unpaidOrderLifetimeMinutes();
}
