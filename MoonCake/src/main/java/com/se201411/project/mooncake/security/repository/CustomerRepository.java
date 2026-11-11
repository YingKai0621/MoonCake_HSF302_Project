package com.se201411.project.mooncake.security.repository;

import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.enums.AccountStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findById(Long customerId);

    Optional<Customer> findByAccount_Id(Long accountId);

    Customer save(Customer customer);

    @Query("""
        select customer
        from Customer customer
        join fetch customer.account account
        where :keyword is null
           or lower(customer.email) like lower(concat('%', :keyword, '%'))
           or customer.phone like concat('%', :keyword, '%')
        order by customer.createdAt desc, customer.id desc
        """)
    Slice<Customer> searchWithAccount(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        select customer.id
        from Customer customer
        where customer.id in :customerIds
          and customer.account.status = :status
        """)
    List<Long> findIdsWithAccountStatus(
        @Param("customerIds") Collection<Long> customerIds,
        @Param("status") AccountStatus status
    );
}
