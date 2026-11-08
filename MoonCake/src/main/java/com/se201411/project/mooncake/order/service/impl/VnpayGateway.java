package com.se201411.project.mooncake.order.service.impl;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class VnpayGateway {
    private final Environment env;
    public VnpayGateway(Environment env) { this.env=env; }
    public boolean enabled() { return Boolean.parseBoolean(env.getProperty("VNPAY_ENABLED", "false")); }
    public void requireConfigured() {
        if (!Boolean.parseBoolean(env.getProperty("VNPAY_ENABLED", "false"))
            || env.getProperty("VNPAY_TMN_CODE", "").isBlank()
            || env.getProperty("VNPAY_HASH_SECRET", "").isBlank())
            throw new BusinessRuleViolationException("VNPay chưa được cấu hình. Vui lòng liên hệ cửa hàng.");
    }
    @jakarta.annotation.PostConstruct
    public void validateStartup() {
        if (Boolean.parseBoolean(env.getProperty("VNPAY_ENABLED", "false"))) {
            requireConfigured();
            validateUrl(returnUrl(), false);
            validateUrl(env.getProperty("VNPAY_PAY_URL", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"), true);
        }
    }
    public String returnUrl() {
        String explicit = env.getProperty("VNPAY_RETURN_URL", "").trim();
        if (!explicit.isEmpty()) return explicit;
        return env.getProperty("APP_PUBLIC_URL", "http://localhost:8080").trim().replaceAll("/+$", "") + "/payments/vnpay/return";
    }
    private void validateUrl(String value, boolean gateway) {
        try {
            var uri = java.net.URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || (!gateway && "http".equals(uri.getScheme()))))
                throw new IllegalArgumentException();
            if (gateway && !"sandbox.vnpayment.vn".equals(uri.getHost())) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Cấu hình VNPay Sandbox không hợp lệ: kiểm tra APP_PUBLIC_URL/VNPAY_RETURN_URL và VNPAY_PAY_URL.");
        }
    }
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.US_ASCII); }
    public String canonical(Map<String,String> values) {
        return new TreeMap<>(values).entrySet().stream()
            .filter(e -> e.getValue()!=null && !e.getValue().isEmpty())
            .filter(e -> !e.getKey().equals("vnp_SecureHash") && !e.getKey().equals("vnp_SecureHashType"))
            .map(e -> encode(e.getKey())+"="+encode(e.getValue())).collect(Collectors.joining("&"));
    }
    public String signature(Map<String,String> values) { return sign(canonical(values)); }
    String sign(String data) {
        try {
            Mac mac=Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(env.getRequiredProperty("VNPAY_HASH_SECRET").getBytes(StandardCharsets.UTF_8),"HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Cannot sign payment",e); }
    }
    public boolean valid(Map<String,String> values) {
        if (!enabled() || env.getProperty("VNPAY_HASH_SECRET", "").isBlank()) return false;
        String received=values.getOrDefault("vnp_SecureHash", "");
        return received.matches("[a-fA-F0-9]{128}")
            && env.getRequiredProperty("VNPAY_TMN_CODE").equals(values.get("vnp_TmnCode"))
            && MessageDigest.isEqual(signature(values).getBytes(StandardCharsets.US_ASCII),received.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    public Map<String,String> query(String ref, LocalDateTime created, String ip, Instant now) {
        requireConfigured();
        var format=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<String,String> p=new LinkedHashMap<>();
        p.put("vnp_RequestId",UUID.randomUUID().toString().replace("-",""));
        p.put("vnp_Version","2.1.0"); p.put("vnp_Command","querydr");
        p.put("vnp_TmnCode",env.getRequiredProperty("VNPAY_TMN_CODE")); p.put("vnp_TxnRef",ref);
        p.put("vnp_TransactionDate",format.format(created.toInstant(ZoneOffset.UTC)));
        p.put("vnp_CreateDate",format.format(now)); p.put("vnp_IpAddr",ip);
        p.put("vnp_OrderInfo","Kiem tra thanh toan MoonCake "+ref);
        p.put("vnp_SecureHash",sign(String.join("|",p.values())));
        try {
            var factory=new org.springframework.http.client.JdkClientHttpRequestFactory(
                java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(10));
            var response=org.springframework.web.client.RestClient.builder().requestFactory(factory).build()
                .post().uri("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(p).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<Map<String,String>>(){});
            if(!validQuery(response,ref)) throw new BusinessRuleViolationException("Không xác minh được phản hồi VNPay. Trạng thái đơn được giữ nguyên.");
            return response;
        } catch(org.springframework.web.client.RestClientException ex) {
            throw new BusinessRuleViolationException("Chưa kết nối được VNPay. Vui lòng kiểm tra lại sau; không cần đặt đơn mới.");
        }
    }
    boolean validQuery(Map<String,String> p,String ref) {
        if(p==null || !"querydr".equals(p.get("vnp_Command")) || !ref.equals(p.get("vnp_TxnRef"))
            || !env.getRequiredProperty("VNPAY_TMN_CODE").equals(p.get("vnp_TmnCode")))return false;
        String[] fields={"vnp_ResponseId","vnp_Command","vnp_ResponseCode","vnp_Message","vnp_TmnCode","vnp_TxnRef","vnp_Amount","vnp_BankCode","vnp_PayDate","vnp_TransactionNo","vnp_TransactionType","vnp_TransactionStatus","vnp_OrderInfo","vnp_PromotionCode","vnp_PromotionAmount"};
        String data=Arrays.stream(fields).map(k->Objects.toString(p.get(k),"")).collect(Collectors.joining("|"));
        String hash=p.getOrDefault("vnp_SecureHash","");
        return hash.matches("[a-fA-F0-9]{128}") && MessageDigest.isEqual(sign(data).getBytes(StandardCharsets.US_ASCII),hash.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
    }
    public String url(String ref, BigDecimal amount, String ip, Instant now) {
        requireConfigured();
        var format=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<String,String> p=new TreeMap<>();
        p.put("vnp_Version","2.1.0"); p.put("vnp_Command","pay");
        p.put("vnp_TmnCode",env.getRequiredProperty("VNPAY_TMN_CODE"));
        p.put("vnp_Amount",amount.movePointRight(2).toBigIntegerExact().toString());
        p.put("vnp_CurrCode","VND"); p.put("vnp_TxnRef",ref);
        p.put("vnp_OrderInfo","Thanh toan MoonCake "+ref); p.put("vnp_OrderType","other");
        p.put("vnp_Locale","vn"); p.put("vnp_IpAddr",ip);
        p.put("vnp_ReturnUrl",returnUrl());
        p.put("vnp_CreateDate",format.format(now)); p.put("vnp_ExpireDate",format.format(now.plusSeconds(900)));
        return env.getProperty("VNPAY_PAY_URL","https://sandbox.vnpayment.vn/paymentv2/vpcpay.html")+"?"+canonical(p)+"&vnp_SecureHash="+signature(p);
    }
}
