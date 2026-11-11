package com.se201411.project.mooncake.security.service.impl;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.common.exception.MoonCakeException;
import com.se201411.project.mooncake.common.exception.ResourceNotFoundException;
import com.se201411.project.mooncake.entity.Customer;
import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;
import com.se201411.project.mooncake.order.dto.CustomerOrderCount;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.OrderStatisticsService;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.AccountLockResult;
import com.se201411.project.mooncake.security.dto.AdminCustomerDetailPageView;
import com.se201411.project.mooncake.security.dto.AdminCustomerListPageView;
import com.se201411.project.mooncake.security.dto.AdminCustomerRow;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import com.se201411.project.mooncake.security.service.CustomerAdminService;
import com.se201411.project.mooncake.security.service.CustomerProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CustomerAdminServiceImpl implements CustomerAdminService {

    private static final int PAGE_SIZE = 20;

    private final AdminAccessService adminAccessService;
    private final CustomerRepository customerRepository;
    private final CustomerProfileService customerProfileService;
    private final OrderStatisticsService orderStatisticsService;
    private final OrderCancellationService orderCancellationService;
    private final SessionRegistry sessionRegistry;

    public CustomerAdminServiceImpl(
        AdminAccessService adminAccessService,
        CustomerRepository customerRepository,
        CustomerProfileService customerProfileService,
        OrderStatisticsService orderStatisticsService,
        OrderCancellationService orderCancellationService,
        SessionRegistry sessionRegistry
    ) {
        this.adminAccessService = adminAccessService;
        this.customerRepository = customerRepository;
        this.customerProfileService = customerProfileService;
        this.orderStatisticsService = orderStatisticsService;
        this.orderCancellationService = orderCancellationService;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCustomerListPageView customersPage(String keyword, int page) {
        adminAccessService.requireAdmin();
        String trimmedKeyword = "";
        if (keyword != null) {
            trimmedKeyword = keyword.trim();
        }
        String searchKeyword = null;
        if (!trimmedKeyword.isEmpty()) {
            searchKeyword = trimmedKeyword;
        }
        int pageNumber = Math.max(0, page);

        Slice<Customer> customers = customerRepository.searchWithAccount(
            searchKeyword,
            PageRequest.of(pageNumber, PAGE_SIZE)
        );
        List<Long> customerIds = new ArrayList<>();
        for (Customer customer : customers) {
            customerIds.add(customer.getId());
        }
        Map<Long, CustomerOrderCount> counts = orderStatisticsService.orderCountsByCustomer(customerIds);

        List<AdminCustomerRow> rows = new ArrayList<>();
        for (Customer customer : customers) {
            rows.add(toRow(customer, counts));
        }
        return new AdminCustomerListPageView(rows, trimmedKeyword, pageNumber, customers.hasNext());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCustomerDetailPageView customerPage(long customerId) {
        adminAccessService.requireAdmin();
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng"));
        Map<Long, CustomerOrderCount> counts = orderStatisticsService.orderCountsByCustomer(List.of(customerId));
        List<CustomerOpenOrderView> openOrders = orderStatisticsService.openOrdersOf(customerId);

        int cancellableCount = 0;
        for (CustomerOpenOrderView order : openOrders) {
            if (order.cancellableOnLock()) {
                cancellableCount = cancellableCount + 1;
            }
        }
        return new AdminCustomerDetailPageView(toRow(customer, counts), openOrders, cancellableCount);
    }

    @Override
    public AccountLockResult lock(long customerId, boolean cancelUnpaidOrders) {
        adminAccessService.requireAdmin();
        boolean changed = customerProfileService.disableAccount(customerId);
        if (!changed) {
            throw new BusinessRuleViolationException("Tài khoản đã bị khóa trước đó.");
        }
        expireSessions(customerProfileService.accountIdOf(customerId));

        int cancelled = 0;
        List<String> manual = new ArrayList<>();
        if (cancelUnpaidOrders) {
            for (CustomerOpenOrderView order : orderStatisticsService.openOrdersOf(customerId)) {
                if (!order.cancellableOnLock()) {
                    manual.add(order.orderCode());
                    continue;
                }
                try {
                    orderCancellationService.cancelForAdmin(order.orderId());
                    cancelled = cancelled + 1;
                } catch (MoonCakeException exception) {
                    manual.add(order.orderCode());
                }
            }
        }
        return new AccountLockResult(cancelled, manual);
    }

    @Override
    public void unlock(long customerId) {
        adminAccessService.requireAdmin();
        boolean changed = customerProfileService.enableAccount(customerId);
        if (!changed) {
            throw new BusinessRuleViolationException("Tài khoản đang hoạt động.");
        }
    }

    private AdminCustomerRow toRow(Customer customer, Map<Long, CustomerOrderCount> counts) {
        CustomerOrderCount count = counts.getOrDefault(customer.getId(), CustomerOrderCount.none(customer.getId()));
        return AdminCustomerRow.entityToDto(customer, count);
    }

    private void expireSessions(long accountId) {
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!(principal instanceof MoonCakeUserPrincipal userPrincipal)) {
                continue;
            }
            if (userPrincipal.getAccountId() != accountId) {
                continue;
            }
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, false)) {
                session.expireNow();
            }
        }
    }
}
