package com.se201411.project.mooncake.security.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.se201411.project.mooncake.entity.Customer;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByAccount_Id(Long accountId);

    Customer save(Customer customer);
}
