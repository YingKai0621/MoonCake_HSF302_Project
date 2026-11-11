package com.se201411.project.mooncake.security.service;

import com.se201411.project.mooncake.common.exception.BusinessRuleViolationException;
import com.se201411.project.mooncake.enums.AccountRole;
import com.se201411.project.mooncake.enums.AccountStatus;
import com.se201411.project.mooncake.order.dto.CustomerOpenOrderView;
import com.se201411.project.mooncake.order.service.OrderCancellationService;
import com.se201411.project.mooncake.order.service.OrderStatisticsService;
import com.se201411.project.mooncake.security.auth.MoonCakeUserPrincipal;
import com.se201411.project.mooncake.security.dto.AccountLockResult;
import com.se201411.project.mooncake.security.repository.CustomerRepository;
import com.se201411.project.mooncake.security.service.impl.CustomerAdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAdminServiceImplTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long ACCOUNT_ID = 3L;

    @Mock
    private AdminAccessService adminAccessService;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerProfileService customerProfileService;

    @Mock
    private OrderStatisticsService orderStatisticsService;

    @Mock
    private OrderCancellationService orderCancellationService;

    @Mock
    private SessionRegistry sessionRegistry;

    @Test
    void lockCancelsOnlyUnpaidPlacedOrders() {
        accountCanBeLocked();
        when(orderStatisticsService.openOrdersOf(CUSTOMER_ID)).thenReturn(List.of(
            openOrder(1L, "MC-1", true),
            openOrder(2L, "MC-2", false),
            openOrder(3L, "MC-3", true)
        ));

        AccountLockResult result = service().lock(CUSTOMER_ID, true);

        verify(orderCancellationService).cancelForAdmin(1L);
        verify(orderCancellationService).cancelForAdmin(3L);
        verify(orderCancellationService, never()).cancelForAdmin(2L);
        assertThat(result.cancelledCount()).isEqualTo(2);
        assertThat(result.manualOrderCodes()).containsExactly("MC-2");
        assertThat(result.message())
            .isEqualTo("Đã khóa tài khoản. Đã hủy 2 đơn chưa thanh toán và hoàn kho. Cần xử lý thủ công: MC-2");
    }

    @Test
    void failedCancellationGoesToManualListAndLockStays() {
        accountCanBeLocked();
        when(orderStatisticsService.openOrdersOf(CUSTOMER_ID)).thenReturn(List.of(
            openOrder(1L, "MC-1", true)
        ));
        doThrow(new BusinessRuleViolationException("VNPay đã thanh toán"))
            .when(orderCancellationService)
            .cancelForAdmin(1L);

        AccountLockResult result = service().lock(CUSTOMER_ID, true);

        assertThat(result.cancelledCount()).isZero();
        assertThat(result.manualOrderCodes()).containsExactly("MC-1");
        verify(customerProfileService, never()).enableAccount(anyLong());
    }

    @Test
    void lockWithoutCancellationKeepsEveryOrder() {
        accountCanBeLocked();

        AccountLockResult result = service().lock(CUSTOMER_ID, false);

        verifyNoInteractions(orderCancellationService);
        verify(orderStatisticsService, never()).openOrdersOf(anyLong());
        assertThat(result.cancelledCount()).isZero();
        assertThat(result.message()).isEqualTo("Đã khóa tài khoản.");
    }

    @Test
    void lockingAlreadyLockedAccountIsRejected() {
        when(customerProfileService.disableAccount(CUSTOMER_ID)).thenReturn(false);

        CustomerAdminServiceImpl service = service();

        assertThatThrownBy(() -> service.lock(CUSTOMER_ID, true))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Tài khoản đã bị khóa trước đó.");
        verifyNoInteractions(orderCancellationService);
        verifyNoInteractions(sessionRegistry);
    }

    @Test
    void lockExpiresTheCustomerSessions() {
        accountCanBeLocked();
        MoonCakeUserPrincipal lockedCustomer = principal(ACCOUNT_ID);
        MoonCakeUserPrincipal otherCustomer = principal(99L);
        SessionInformation lockedSession = mock(SessionInformation.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(lockedCustomer, otherCustomer));
        when(sessionRegistry.getAllSessions(lockedCustomer, false)).thenReturn(List.of(lockedSession));

        service().lock(CUSTOMER_ID, false);

        verify(lockedSession).expireNow();
        verify(sessionRegistry, never()).getAllSessions(otherCustomer, false);
    }

    @Test
    void unlockActivatesAccount() {
        when(customerProfileService.enableAccount(CUSTOMER_ID)).thenReturn(true);

        service().unlock(CUSTOMER_ID);

        verify(adminAccessService).requireAdmin();
        verify(customerProfileService).enableAccount(CUSTOMER_ID);
    }

    @Test
    void unlockingActiveAccountIsRejected() {
        when(customerProfileService.enableAccount(CUSTOMER_ID)).thenReturn(false);

        CustomerAdminServiceImpl service = service();

        assertThatThrownBy(() -> service.unlock(CUSTOMER_ID))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessage("Tài khoản đang hoạt động.");
    }

    private void accountCanBeLocked() {
        when(customerProfileService.disableAccount(CUSTOMER_ID)).thenReturn(true);
        when(customerProfileService.accountIdOf(CUSTOMER_ID)).thenReturn(ACCOUNT_ID);
    }

    private CustomerAdminServiceImpl service() {
        return new CustomerAdminServiceImpl(
            adminAccessService,
            customerRepository,
            customerProfileService,
            orderStatisticsService,
            orderCancellationService,
            sessionRegistry
        );
    }

    private static CustomerOpenOrderView openOrder(long orderId, String code, boolean cancellable) {
        return new CustomerOpenOrderView(orderId, code, "Chờ xử lý", "COD · Chưa thanh toán", "250.000đ", cancellable);
    }

    private static MoonCakeUserPrincipal principal(long accountId) {
        return new MoonCakeUserPrincipal(
            accountId,
            CUSTOMER_ID,
            "customer@example.com",
            "hash",
            AccountRole.CUSTOMER,
            AccountStatus.ACTIVE
        );
    }
}
