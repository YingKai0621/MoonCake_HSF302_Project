package com.se201411.project.mooncake.product.service.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.enums.ConfigurationStatus;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.product.dto.PresetComponentSnapshot;
import com.se201411.project.mooncake.product.dto.PresetRecipeSnapshot;
import com.se201411.project.mooncake.product.repository.PresetConfigurationItemRepository;
import com.se201411.project.mooncake.product.repository.PresetConfigurationRepository;
import com.se201411.project.mooncake.product.service.PresetConfigurationQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PresetConfigurationQueryServiceImpl implements PresetConfigurationQueryService {

    private final PresetConfigurationRepository configurationRepository;
    private final PresetConfigurationItemRepository itemRepository;

    public PresetConfigurationQueryServiceImpl(
        PresetConfigurationRepository configurationRepository,
        PresetConfigurationItemRepository itemRepository
    ) {
        this.configurationRepository = configurationRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    public PresetRecipeSnapshot requireReadyPreset(long giftSetVariantId) {
        Optional<BoxConfiguration> configurationResult = configurationRepository
            .findReadyByGiftSetVariantId(
                giftSetVariantId,
                ConfigurationType.PRESET,
                ConfigurationStatus.READY
            );
        if (configurationResult.isEmpty()) {
            throw new ResourceNotFoundException(
                "Không tìm thấy PRESET READY cho SKU hộp quà: " + giftSetVariantId
            );
        }
        BoxConfiguration configuration = configurationResult.get();

        List<BoxConfigurationItem> items = itemRepository.findByConfigurationIds(
            List.of(configuration.getId())
        );
        List<PresetComponentSnapshot> components = new ArrayList<>();
        for (BoxConfigurationItem item : items) {
            PresetComponentSnapshot component = new PresetComponentSnapshot(
                item.getMooncakeVariant().getId(),
                item.getQuantityPerBox()
            );
            components.add(component);
        }

        return new PresetRecipeSnapshot(
            configuration.getId(),
            giftSetVariantId,
            configuration.getBox().getEmptyBoxVariant().getId(),
            configuration.getBox().getCapacityCakes(),
            configuration.getStatus(),
            components
        );
    }
}
