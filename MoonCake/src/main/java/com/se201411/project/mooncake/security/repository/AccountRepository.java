package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.Account;
import org.springframework.data.repository.Repository;

import java.util.Optional;

public interface AccountRepository extends Repository<Account, Long> {

    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    Account save(Account account);
}
