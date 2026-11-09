package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Component
public class VnpayGateway {

    private static final String DEFAULT_PAY_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String QUERY_URL = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private static final String SANDBOX_HOST = "sandbox.vnpayment.vn";
    private static final String HMAC_ALGORITHM = "HmacSHA512";
    private static final String HASH_PATTERN = "[a-fA-F0-9]{128}";
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final String[] QUERY_RESPONSE_FIELDS = {
        "vnp_ResponseId",
        "vnp_Command",
        "vnp_ResponseCode",
        "vnp_Message",
        "vnp_TmnCode",
        "vnp_TxnRef",
        "vnp_Amount",
        "vnp_BankCode",
        "vnp_PayDate",
        "vnp_TransactionNo",
        "vnp_TransactionType",
        "vnp_TransactionStatus",
        "vnp_OrderInfo",
        "vnp_PromotionCode",
        "vnp_PromotionAmount"
    };

    private final Environment env;

    public VnpayGateway(Environment env) {
        this.env = env;
    }

    public boolean enabled() {
        return Boolean.parseBoolean(env.getProperty("VNPAY_ENABLED", "false"));
    }

    public void requireConfigured() {
        boolean missingSettings = !enabled()
            || env.getProperty("VNPAY_TMN_CODE", "").isBlank()
            || env.getProperty("VNPAY_HASH_SECRET", "").isBlank();
        if (missingSettings) {
            throw new BusinessRuleViolationException("VNPay chưa được cấu hình. Vui lòng liên hệ cửa hàng.");
        }
    }

    @PostConstruct
    public void validateStartup() {
        if (!enabled()) {
            return;
        }
        requireConfigured();
        validateUrl(returnUrl(), false);
        validateUrl(payUrl(), true);
    }

    public String returnUrl() {
        String explicit = env.getProperty("VNPAY_RETURN_URL", "").trim();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String publicUrl = env.getProperty("APP_PUBLIC_URL", "http://localhost:8080").trim();
        return publicUrl.replaceAll("/+$", "") + "/payments/vnpay/return";
    }

    public String canonical(Map<String, String> values) {
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : new TreeMap<>(values).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            boolean empty = value == null || value.isEmpty();
            boolean hashField = key.equals("vnp_SecureHash") || key.equals("vnp_SecureHashType");
            if (empty || hashField) {
                continue;
            }
            pairs.add(encode(key) + "=" + encode(value));
        }
        return String.join("&", pairs);
    }

    public String signature(Map<String, String> values) {
        return sign(canonical(values));
    }

    String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            byte[] secret = env.getRequiredProperty("VNPAY_HASH_SECRET").getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot sign payment", exception);
        }
    }

    public boolean valid(Map<String, String> values) {
        if (!enabled() || env.getProperty("VNPAY_HASH_SECRET", "").isBlank()) {
            return false;
        }
        String received = values.getOrDefault("vnp_SecureHash", "");
        boolean sameMerchant = env.getRequiredProperty("VNPAY_TMN_CODE").equals(values.get("vnp_TmnCode"));
        return received.matches(HASH_PATTERN)
            && sameMerchant
            && sameHash(signature(values), received);
    }

    public Map<String, String> query(String ref, LocalDateTime created, String ip, Instant now) {
        requireConfigured();
        Map<String, String> request = new LinkedHashMap<>();
        request.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        request.put("vnp_Version", "2.1.0");
        request.put("vnp_Command", "querydr");
        request.put("vnp_TmnCode", env.getRequiredProperty("VNPAY_TMN_CODE"));
        request.put("vnp_TxnRef", ref);
        request.put("vnp_TransactionDate", VNPAY_TIME.format(created.toInstant(ZoneOffset.UTC)));
        request.put("vnp_CreateDate", VNPAY_TIME.format(now));
        request.put("vnp_IpAddr", ip);
        request.put("vnp_OrderInfo", "Kiem tra thanh toan MoonCake " + ref);
        request.put("vnp_SecureHash", sign(String.join("|", request.values())));

        try {
            Map<String, String> response = sendQuery(request);
            if (isUnsignedError(response)) {
                return response;
            }
            if (!validQuery(response, ref)) {
                throw new BusinessRuleViolationException(
                    "Không xác minh được phản hồi VNPay. Trạng thái đơn được giữ nguyên."
                );
            }
            return response;
        } catch (RestClientException exception) {
            throw new BusinessRuleViolationException(
                "Chưa kết nối được VNPay. Vui lòng kiểm tra lại sau; không cần đặt đơn mới."
            );
        }
    }

    boolean validQuery(Map<String, String> response, String ref) {
        if (response == null) {
            return false;
        }
        boolean matchesRequest = "querydr".equals(response.get("vnp_Command"))
            && ref.equals(response.get("vnp_TxnRef"))
            && env.getRequiredProperty("VNPAY_TMN_CODE").equals(response.get("vnp_TmnCode"));
        if (!matchesRequest) {
            return false;
        }
        List<String> parts = new ArrayList<>();
        for (String field : QUERY_RESPONSE_FIELDS) {
            parts.add(Objects.toString(response.get(field), ""));
        }
        String hash = response.getOrDefault("vnp_SecureHash", "");
        return hash.matches(HASH_PATTERN) && sameHash(sign(String.join("|", parts)), hash);
    }

    public String url(String ref, BigDecimal amount, String ip, Instant now) {
        requireConfigured();
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", env.getRequiredProperty("VNPAY_TMN_CODE"));
        params.put("vnp_Amount", amount.movePointRight(2).toBigIntegerExact().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", ref);
        params.put("vnp_OrderInfo", "Thanh toan MoonCake " + ref);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_IpAddr", ip);
        params.put("vnp_ReturnUrl", returnUrl());
        params.put("vnp_CreateDate", VNPAY_TIME.format(now));
        params.put("vnp_ExpireDate", VNPAY_TIME.format(now.plusSeconds(attemptLifetimeSeconds())));
        return payUrl() + "?" + canonical(params) + "&vnp_SecureHash=" + signature(params);
    }

    public long attemptLifetimeSeconds() {
        return env.getProperty("VNPAY_ATTEMPT_SECONDS", Long.class, 900L);
    }

    public long unpaidOrderLifetimeMinutes() {
        return env.getProperty("UNPAID_ORDER_CANCEL_MINUTES", Long.class, 30L);
    }

    private Map<String, String> sendQuery(Map<String, String> request) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder()
            .requestFactory(factory)
            .build()
            .post()
            .uri(QUERY_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String, String>>() { });
    }

    private boolean isUnsignedError(Map<String, String> response) {
        if (response == null) {
            return false;
        }
        String hash = response.get("vnp_SecureHash");
        boolean unsigned = hash == null || hash.isBlank();
        return unsigned && !"00".equals(response.get("vnp_ResponseCode"));
    }

    private String payUrl() {
        return env.getProperty("VNPAY_PAY_URL", DEFAULT_PAY_URL);
    }

    private void validateUrl(String value, boolean gateway) {
        try {
            URI uri = URI.create(value);
            boolean secure = "https".equals(uri.getScheme());
            boolean allowedScheme = secure || (!gateway && "http".equals(uri.getScheme()));
            boolean malformed = uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null;
            boolean wrongGatewayHost = gateway && !SANDBOX_HOST.equals(uri.getHost());
            if (malformed || !allowedScheme || wrongGatewayHost) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Cấu hình VNPay Sandbox không hợp lệ: kiểm tra APP_PUBLIC_URL/VNPAY_RETURN_URL và VNPAY_PAY_URL."
            );
        }
    }

    private boolean sameHash(String expected, String received) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.US_ASCII);
        byte[] receivedBytes = received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expectedBytes, receivedBytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.US_ASCII);
    }
}
