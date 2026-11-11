package com.se201411.project.mooncake.product.repository.impl;

import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.BoxConfiguration;
import com.se201411.project.mooncake.entity.BoxConfigurationItem;
import com.se201411.project.mooncake.enums.ConfigurationType;
import com.se201411.project.mooncake.product.repository.PresetAdminRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PresetAdminRepositoryImpl implements PresetAdminRepository {

    private static final String PRESET_ONLY = "PRESET only";

    private final EntityManager em;

    public PresetAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<BoxConfiguration> all() {
        String query = """
            from BoxConfiguration c
            join fetch c.presetVariant
            join fetch c.box
            where c.type = :type
            order by c.id
            """;
        return em.createQuery(query, BoxConfiguration.class)
            .setParameter("type", ConfigurationType.PRESET)
            .getResultList();
    }

    @Override
    public BoxConfiguration lock(long id) {
        String query = "from BoxConfiguration c where c.id = :id and c.type = :type";
        List<BoxConfiguration> found = em.createQuery(query, BoxConfiguration.class)
            .setParameter("id", id)
            .setParameter("type", ConfigurationType.PRESET)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .getResultList();
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy PRESET.");
        }
        return found.get(0);
    }

    @Override
    public List<BoxConfigurationItem> items(long id) {
        String query = """
            from BoxConfigurationItem i
            join fetch i.mooncakeVariant
            where i.configuration.id = :id
              and i.configuration.type = :type
            order by i.id
            """;
        return em.createQuery(query, BoxConfigurationItem.class)
            .setParameter("id", id)
            .setParameter("type", ConfigurationType.PRESET)
            .getResultList();
    }

    @Override
    public List<BoxConfigurationItem> allPresetItems() {
        String query = """
            from BoxConfigurationItem i
            join fetch i.configuration c
            join fetch i.mooncakeVariant
            where c.type = :type
            order by i.id
            """;
        return em.createQuery(query, BoxConfigurationItem.class)
            .setParameter("type", ConfigurationType.PRESET)
            .getResultList();
    }

    @Override
    public void save(BoxConfiguration configuration) {
        if (configuration.getType() != ConfigurationType.PRESET) {
            throw new IllegalArgumentException(PRESET_ONLY);
        }
        if (configuration.getId() == null) {
            em.persist(configuration);
        }
        em.flush();
    }

    @Override
    public void saveItem(BoxConfigurationItem item) {
        if (item.getConfiguration().getType() != ConfigurationType.PRESET) {
            throw new IllegalArgumentException(PRESET_ONLY);
        }
        if (item.getId() == null) {
            em.persist(item);
        }
        em.flush();
    }

    @Override
    public void removeItem(BoxConfigurationItem item) {
        if (item.getConfiguration().getType() != ConfigurationType.PRESET) {
            throw new IllegalArgumentException(PRESET_ONLY);
        }
        em.remove(item);
        em.flush();
    }
}
