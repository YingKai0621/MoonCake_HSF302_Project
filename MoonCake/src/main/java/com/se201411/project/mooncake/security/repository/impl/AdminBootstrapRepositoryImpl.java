package com.se201411.project.mooncake.security.repository.impl;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.security.repository.AdminBootstrapRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("bootstrap-admin")
public class AdminBootstrapRepositoryImpl implements AdminBootstrapRepository {

    private final EntityManager em;

    public AdminBootstrapRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean adminExistsUnderLock() {
        Number count = (Number) em.createNativeQuery(
            "SELECT COUNT(*) FROM dbo.ACCOUNT WITH (TABLOCKX, HOLDLOCK) WHERE role = 'ADMIN'"
        ).getSingleResult();
        return count.longValue() > 0;
    }

    @Override
    public boolean emailExists(String email) {
        Long count = em.createQuery("select count(a) from Account a where a.email = :email", Long.class)
            .setParameter("email", email)
            .getSingleResult();
        return count > 0;
    }

    @Override
    public void save(Account account) {
        em.persist(account);
        em.flush();
    }
}
