package com.se201411.project.mooncake.product.repository.impl;

import com.se201411.project.mooncake.product.repository.PresetAdminRepository;
import com.se201411.project.mooncake.entity.*;
import com.se201411.project.mooncake.enums.*;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PresetAdminRepositoryImpl implements PresetAdminRepository {
    private final EntityManager em;

    public PresetAdminRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<BoxConfiguration> all() {
        return em.createQuery("from BoxConfiguration c join fetch c.presetVariant join fetch c.box where c.type=:type order by c.id", BoxConfiguration.class)
                .setParameter("type", ConfigurationType.PRESET).getResultList();
    }

    @Override
    public BoxConfiguration lock(long id) {
        return em.createQuery("from BoxConfiguration c where c.id=:id and c.type=:type", BoxConfiguration.class)
                .setParameter("id", id).setParameter("type", ConfigurationType.PRESET).setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream().findFirst().orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy PRESET."));
    }

    @Override
    public List<BoxConfigurationItem> items(long id) {
        return em.createQuery("from BoxConfigurationItem i join fetch i.mooncakeVariant where i.configuration.id=:id and i.configuration.type=:type order by i.id", BoxConfigurationItem.class)
                .setParameter("id", id).setParameter("type", ConfigurationType.PRESET).getResultList();
    }

    @Override
    public List<BoxConfigurationItem> allPresetItems() {
        return em.createQuery("from BoxConfigurationItem i join fetch i.configuration c join fetch i.mooncakeVariant where c.type=:type order by i.id", BoxConfigurationItem.class)
                .setParameter("type", ConfigurationType.PRESET).getResultList();
    }

    @Override
    public boolean historicallyReferenced(long id, long variant) {
        return em.createQuery("select count(i) from OrderItem i where i.configuration.id=:id or i.variant.id=:variant", Long.class)
                .setParameter("id", id).setParameter("variant", variant).getSingleResult() > 0;
    }

    @Override
    public void save(BoxConfiguration c) {
        if (c.getType() != ConfigurationType.PRESET) throw new IllegalArgumentException("PRESET only");
        if (c.getId() == null) em.persist(c);
        em.flush();
    }

    @Override
    public void saveItem(BoxConfigurationItem i) {
        if (i.getConfiguration().getType() != ConfigurationType.PRESET)
            throw new IllegalArgumentException("PRESET only");
        if (i.getId() == null) em.persist(i);
        em.flush();
    }

    @Override
    public void removeItem(BoxConfigurationItem i) {
        if (i.getConfiguration().getType() != ConfigurationType.PRESET)
            throw new IllegalArgumentException("PRESET only");
        em.remove(i);
        em.flush();
    }
}
