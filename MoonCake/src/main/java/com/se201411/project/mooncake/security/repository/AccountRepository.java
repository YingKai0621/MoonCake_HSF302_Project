package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.Account;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    Account save(Account account);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Account account
        set account.status = :newStatus
        where account.id = :accountId
          and account.role = :role
          and account.status = :currentStatus
        """)
    int updateStatus(
        @Param("accountId") long accountId,
        @Param("role") AccountRole role,
        @Param("currentStatus") AccountStatus currentStatus,
        @Param("newStatus") AccountStatus newStatus
    );
}
