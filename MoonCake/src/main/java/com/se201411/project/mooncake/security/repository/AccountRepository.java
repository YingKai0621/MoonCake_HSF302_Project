package com.se201411.project.mooncake.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    Account save(Account account);
}
