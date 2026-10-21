package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.order.dto.AddressOption;
import com.se201411.project.mooncake.order.service.DeliveryAddressService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeliveryAddressServiceImpl implements DeliveryAddressService {

    private static final int MAX_SHIPPING_ADDRESS_LENGTH = 500;
    private static final String DATA_FILE =
        "data/vietnam-administrative-divisions.json";

    private final List<AddressOption> provinces;
    private final List<AddressOption> wards;

    public DeliveryAddressServiceImpl(ObjectMapper objectMapper) {
        List<AddressOption> loadedProvinces = new ArrayList<>();
        List<AddressOption> loadedWards = new ArrayList<>();
        loadAddressData(objectMapper, loadedProvinces, loadedWards);
        this.provinces = List.copyOf(loadedProvinces);
        this.wards = List.copyOf(loadedWards);
    }

    @Override
    public List<AddressOption> getProvinces() {
        return provinces;
    }

    @Override
    public List<AddressOption> getWards() {
        return wards;
    }

    @Override
    public String buildShippingAddress(
        String streetAddress,
        String wardCode,
        String provinceCode
    ) {
        if (streetAddress == null || streetAddress.isBlank()) {
            throw new BusinessRuleViolationException("Vui lòng nhập số nhà và tên đường");
        }

        AddressOption province = findOption(provinces, provinceCode);
        AddressOption ward = findChildOption(wards, wardCode, province.code());

        String shippingAddress = streetAddress.trim()
            + ", " + ward.name()
            + ", " + province.name();
        if (shippingAddress.length() > MAX_SHIPPING_ADDRESS_LENGTH) {
            throw new BusinessRuleViolationException(
                "Địa chỉ giao hàng không được vượt quá 500 ký tự"
            );
        }
        return shippingAddress;
    }

    private void loadAddressData(
        ObjectMapper objectMapper,
        List<AddressOption> loadedProvinces,
        List<AddressOption> loadedWards
    ) {
        ClassPathResource resource = new ClassPathResource(DATA_FILE);
        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode provinceNodes = root.path("data");

            for (JsonNode provinceNode : provinceNodes) {
                String provinceCode = provinceNode.path("id").asText();
                String provinceName = provinceNode.path("name").path("local").asText();
                loadedProvinces.add(new AddressOption(
                    provinceCode,
                    provinceName,
                    null
                ));

                for (JsonNode wardNode : provinceNode.path("ward")) {
                    loadedWards.add(new AddressOption(
                        wardNode.path("id").asText(),
                        wardNode.path("name").path("local").asText(),
                        provinceCode
                    ));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc dữ liệu địa chỉ Việt Nam", exception);
        }

        if (loadedProvinces.isEmpty() || loadedWards.isEmpty()) {
            throw new IllegalStateException("Dữ liệu địa chỉ Việt Nam đang trống");
        }
    }

    private AddressOption findOption(List<AddressOption> options, String code) {
        if (code != null) {
            for (AddressOption option : options) {
                if (option.code().equals(code)) {
                    return option;
                }
            }
        }
        throw new BusinessRuleViolationException("Địa chỉ giao hàng không hợp lệ");
    }

    private AddressOption findChildOption(
        List<AddressOption> options,
        String code,
        String expectedParentCode
    ) {
        AddressOption option = findOption(options, code);
        if (!expectedParentCode.equals(option.parentCode())) {
            throw new BusinessRuleViolationException("Địa chỉ giao hàng không hợp lệ");
        }
        return option;
    }
}
