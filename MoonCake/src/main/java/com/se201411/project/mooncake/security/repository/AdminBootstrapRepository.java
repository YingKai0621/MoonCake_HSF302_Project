package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.Account;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("bootstrap-admin")
public class AdminBootstrapRepository {
    private final EntityManager em;

    public AdminBootstrapRepository(EntityManager em) {
        this.em = em;
    }

    public boolean adminExistsUnderLock() {
        Number count = (Number) em.createNativeQuery("SELECT COUNT(*) FROM dbo.ACCOUNT WITH (TABLOCKX,HOLDLOCK) WHERE role='ADMIN'").getSingleResult();
        return count.longValue() > 0;
    }

    public boolean emailExists(String email) {
        return em.createQuery("select count(a) from Account a where a.email=:email", Long.class).setParameter("email", email).getSingleResult() > 0;
    }

    public void save(Account account) {
        em.persist(account);
        em.flush();
    }
}
