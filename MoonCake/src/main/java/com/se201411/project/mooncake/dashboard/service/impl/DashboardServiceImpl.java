package com.se201411.project.mooncake.dashboard.service.impl;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardTaskView;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;
import com.se201411.project.mooncake.dashboard.dto.DiscountSummaryView;
import com.se201411.project.mooncake.dashboard.dto.KpiView;
import com.se201411.project.mooncake.dashboard.dto.PaidAmount;
import com.se201411.project.mooncake.dashboard.dto.RecentOrderView;
import com.se201411.project.mooncake.dashboard.dto.TopProductView;
import com.se201411.project.mooncake.dashboard.repository.DashboardRepository;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.enums.PaymentStatus;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final Locale VIETNAMESE = Locale.forLanguageTag("vi-VN");
    private static final int TOP_PRODUCT_DAYS = 30;
    private static final int EXPIRY_WARNING_DAYS = 14;

    private final DashboardRepository dashboardRepository;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public DashboardServiceImpl(
        DashboardRepository dashboardRepository,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.dashboardRepository = dashboardRepository;
        this.adminAccessService = adminAccessService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView dashboard(DashboardPeriod period) {
        adminAccessService.requireAdmin();
        LocalDate today = LocalDate.now(clock.withZone(VIETNAM));

        List<LocalDate> bucketStarts = bucketStarts(period, today);
        int bucketCount = bucketStarts.size();
        long[] revenue = new long[bucketCount];
        long[] orderCount = new long[bucketCount];
        long[] orderValue = new long[bucketCount];
        long[] collected = new long[bucketCount];
        long[] discountedOrders = new long[bucketCount];
        long[] discountTotal = new long[bucketCount];

        LocalDateTime from = toUtc(bucketStarts.get(0));
        for (SalesOrder order : dashboardRepository.findByCreatedAtGreaterThanEqual(from)) {
            int index = bucketIndex(bucketStarts, period, toVietnamDate(order.getCreatedAt()));
            if (index < 0 || order.getStatus() == OrderStatus.CANCELLED) {
                continue;
            }
            long total = order.getTotalAmount().longValue();
            orderCount[index]++;
            orderValue[index] += total;
            if (order.getDiscountAmount() != null && order.getDiscountAmount().signum() > 0) {
                discountedOrders[index]++;
                discountTotal[index] += order.getDiscountAmount().longValue();
            }
            if (order.getStatus() == OrderStatus.COMPLETED) {
                revenue[index] += total;
            }
        }
        for (PaidAmount paid : dashboardRepository.findPaidSince(PaymentStatus.PAID, from)) {
            int index = bucketIndex(bucketStarts, period, toVietnamDate(paid.paidAt()));
            if (index >= 0) {
                collected[index] += paid.amount().longValue();
            }
        }

        int current = bucketCount - 1;
        int previous = bucketCount - 2;
        List<KpiView> kpis = new ArrayList<>();
        kpis.add(kpi("Doanh thu", revenue[current], revenue[previous], true, period));
        kpis.add(kpi("Đơn hàng mới", orderCount[current], orderCount[previous], false, period));
        kpis.add(kpi(
            "Giá trị trung bình / đơn",
            average(orderValue[current], orderCount[current]),
            average(orderValue[previous], orderCount[previous]),
            true,
            period
        ));
        kpis.add(kpi("Đã thu", collected[current], collected[previous], true, period));

        List<String> labels = new ArrayList<>();
        List<Long> revenueSeries = new ArrayList<>();
        List<Long> orderSeries = new ArrayList<>();
        for (int i = 0; i < bucketCount; i++) {
            labels.add(bucketLabel(period, bucketStarts.get(i)));
            revenueSeries.add(revenue[i]);
            orderSeries.add(orderCount[i]);
        }

        return new DashboardView(
            period,
            kpis,
            labels,
            revenueSeries,
            orderSeries,
            tasks(today),
            recentOrders(),
            topProducts(today),
            discounts(period, discountedOrders[current], discountTotal[current])
        );
    }

    private DiscountSummaryView discounts(DashboardPeriod period, long orderCount, long totalDiscount) {
        List<DiscountSummaryView.Order> recent = new ArrayList<>();
        List<SalesOrder> orders = dashboardRepository
            .findTop5ByDiscountAmountGreaterThanAndStatusNotOrderByCreatedAtDesc(BigDecimal.ZERO, OrderStatus.CANCELLED);
        for (SalesOrder order : orders) {
            recent.add(new DiscountSummaryView.Order(
                order.getId(),
                order.getOrderCode(),
                order.getRecipientName(),
                formatMoney(order.getDiscountAmount()),
                formatMoney(order.getTotalAmount()),
                order.getCreatedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE_TIME)
            ));
        }
        return new DiscountSummaryView(currentLabel(period), orderCount, totalDiscount, recent);
    }

    private String currentLabel(DashboardPeriod period) {
        if (period == DashboardPeriod.DAY) {
            return "Hôm nay";
        }
        if (period == DashboardPeriod.MONTH) {
            return "Tháng này";
        }
        if (period == DashboardPeriod.QUARTER) {
            return "Quý này";
        }
        return "Năm nay";
    }

    private List<DashboardTaskView> tasks(LocalDate today) {
        List<DashboardTaskView> tasks = new ArrayList<>();
        tasks.add(new DashboardTaskView(
            "Đơn chờ xử lý",
            dashboardRepository.countByStatus(OrderStatus.PLACED),
            "/admin/orders?status=PLACED",
            "warning"
        ));
        tasks.add(new DashboardTaskView(
            "Đơn đã hủy cần hoàn tiền",
            dashboardRepository.countPaidCancelledOrders(OrderStatus.CANCELLED, PaymentStatus.PAID),
            "/admin/orders?status=CANCELLED",
            "danger"
        ));
        tasks.add(new DashboardTaskView(
            "SKU sắp hết hàng",
            dashboardRepository.countLowStockVariants(),
            "/admin/inventory",
            "warning"
        ));
        tasks.add(new DashboardTaskView(
            "Lô sắp hết hạn (" + EXPIRY_WARNING_DAYS + " ngày)",
            dashboardRepository.countBatchesExpiringBy(today.plusDays(EXPIRY_WARNING_DAYS)),
            "/admin/batches",
            "danger"
        ));
        tasks.add(new DashboardTaskView(
            "Đánh giá chờ duyệt",
            dashboardRepository.countReviewsWaitingForApproval(),
            "/admin/feedback",
            "info"
        ));
        return tasks;
    }

    private List<RecentOrderView> recentOrders() {
        List<RecentOrderView> views = new ArrayList<>();
        for (SalesOrder order : dashboardRepository.findTop5ByOrderByCreatedAtDesc()) {
            views.add(new RecentOrderView(
                order.getId(),
                order.getOrderCode(),
                order.getRecipientName(),
                formatMoney(order.getTotalAmount()),
                statusLabel(order.getStatus()),
                statusTone(order.getStatus()),
                order.getCreatedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).format(DATE_TIME)
            ));
        }
        return views;
    }

    private List<TopProductView> topProducts(LocalDate today) {
        LocalDateTime from = toUtc(today.minusDays(TOP_PRODUCT_DAYS - 1L));
        return dashboardRepository.findTopProducts(from, OrderStatus.CANCELLED, PageRequest.of(0, 5));
    }

    private KpiView kpi(String label, long current, long previous, boolean money, DashboardPeriod period) {
        Double change = null;
        if (previous != 0) {
            change = Math.round((current - previous) * 1000.0 / previous) / 10.0;
        } else if (current == 0) {
            change = 0.0;
        }
        return new KpiView(label, current, money, change, period.getCompareLabel());
    }

    private long average(long total, long count) {
        if (count == 0) {
            return 0;
        }
        return Math.round((double) total / count);
    }

    private List<LocalDate> bucketStarts(DashboardPeriod period, LocalDate today) {
        LocalDate current = bucketStart(period, today);
        List<LocalDate> starts = new ArrayList<>();
        for (int i = period.getChartBuckets() - 1; i >= 0; i--) {
            starts.add(shift(period, current, -i));
        }
        return starts;
    }

    private int bucketIndex(List<LocalDate> bucketStarts, DashboardPeriod period, LocalDate date) {
        return bucketStarts.indexOf(bucketStart(period, date));
    }

    private LocalDate bucketStart(DashboardPeriod period, LocalDate date) {
        if (period == DashboardPeriod.DAY) {
            return date;
        }
        if (period == DashboardPeriod.MONTH) {
            return date.withDayOfMonth(1);
        }
        if (period == DashboardPeriod.QUARTER) {
            int firstMonth = (date.getMonthValue() - 1) / 3 * 3 + 1;
            return LocalDate.of(date.getYear(), firstMonth, 1);
        }
        return LocalDate.of(date.getYear(), 1, 1);
    }

    private LocalDate shift(DashboardPeriod period, LocalDate start, int steps) {
        if (period == DashboardPeriod.DAY) {
            return start.plusDays(steps);
        }
        if (period == DashboardPeriod.MONTH) {
            return start.plusMonths(steps);
        }
        if (period == DashboardPeriod.QUARTER) {
            return start.plusMonths(3L * steps);
        }
        return start.plusYears(steps);
    }

    private String bucketLabel(DashboardPeriod period, LocalDate start) {
        if (period == DashboardPeriod.DAY) {
            return start.format(DAY_LABEL);
        }
        if (period == DashboardPeriod.MONTH) {
            return start.format(MONTH_LABEL);
        }
        if (period == DashboardPeriod.QUARTER) {
            return "Q" + ((start.getMonthValue() - 1) / 3 + 1) + "/" + start.getYear();
        }
        return String.valueOf(start.getYear());
    }

    private LocalDateTime toUtc(LocalDate vietnamDate) {
        return vietnamDate.atStartOfDay(VIETNAM).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDate toVietnamDate(LocalDateTime utcTime) {
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(VIETNAM).toLocalDate();
    }

    private String formatMoney(BigDecimal amount) {
        return NumberFormat.getNumberInstance(VIETNAMESE).format(amount) + "đ";
    }

    private String statusLabel(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "Chờ xử lý";
        }
        if (status == OrderStatus.PROCESSING) {
            return "Đang chuẩn bị";
        }
        if (status == OrderStatus.SHIPPING) {
            return "Đang giao";
        }
        if (status == OrderStatus.COMPLETED) {
            return "Hoàn tất";
        }
        return "Đã hủy";
    }

    private String statusTone(OrderStatus status) {
        if (status == OrderStatus.PLACED) {
            return "warning";
        }
        if (status == OrderStatus.COMPLETED) {
            return "success";
        }
        if (status == OrderStatus.CANCELLED) {
            return "danger";
        }
        return "info";
    }
}
