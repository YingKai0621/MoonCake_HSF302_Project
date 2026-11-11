package com.se201411.project.mooncake.dashboard.service.impl;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardTaskView;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;
import com.se201411.project.mooncake.dashboard.dto.DiscountSummaryView;
import com.se201411.project.mooncake.dashboard.dto.KpiView;
import com.se201411.project.mooncake.dashboard.dto.RecentOrderView;
import com.se201411.project.mooncake.dashboard.dto.TopProductView;
import com.se201411.project.mooncake.dashboard.service.DashboardService;
import com.se201411.project.mooncake.entity.SalesOrder;
import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.feedback.service.FeedbackService;
import com.se201411.project.mooncake.inventory.service.InventoryAdminService;
import com.se201411.project.mooncake.order.dto.PaidAmount;
import com.se201411.project.mooncake.order.dto.ProductSalesSummary;
import com.se201411.project.mooncake.order.service.OrderStatisticsService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final int TOP_PRODUCT_DAYS = 30;
    private static final int EXPIRY_WARNING_DAYS = 14;

    private final OrderStatisticsService orderStatistics;
    private final InventoryAdminService inventory;
    private final FeedbackService feedback;
    private final AdminAccessService adminAccessService;
    private final Clock clock;

    public DashboardServiceImpl(
        OrderStatisticsService orderStatistics,
        InventoryAdminService inventory,
        FeedbackService feedback,
        AdminAccessService adminAccessService,
        Clock clock
    ) {
        this.orderStatistics = orderStatistics;
        this.inventory = inventory;
        this.feedback = feedback;
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
        for (SalesOrder order : orderStatistics.ordersCreatedSince(from)) {
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
        for (PaidAmount paid : orderStatistics.paymentsPaidSince(from)) {
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
        List<SalesOrder> orders = orderStatistics.latestDiscountedOrders();
        for (SalesOrder order : orders) {
            String formattedCreatedAt = formatVietnamTime(order.getCreatedAt());
            recent.add(DiscountSummaryView.Order.entityToDto(order, formattedCreatedAt));
        }
        return new DiscountSummaryView(currentLabel(period), orderCount, totalDiscount, recent);
    }

    private String formatVietnamTime(LocalDateTime utcTime) {
        return utcTime
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(VIETNAM)
            .format(DATE_TIME);
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
            orderStatistics.countPlacedOrders(),
            "/admin/orders?status=PLACED",
            "warning"
        ));
        tasks.add(new DashboardTaskView(
            "Đơn đã hủy cần hoàn tiền",
            orderStatistics.countPaidCancelledOrders(),
            "/admin/orders?status=CANCELLED",
            "danger"
        ));
        tasks.add(new DashboardTaskView(
            "SKU sắp hết hàng",
            inventory.countLowStockVariants(),
            "/admin/inventory",
            "warning"
        ));
        tasks.add(new DashboardTaskView(
            "Lô sắp hết hạn (" + EXPIRY_WARNING_DAYS + " ngày)",
            inventory.countBatchesExpiringBy(today.plusDays(EXPIRY_WARNING_DAYS)),
            "/admin/batches",
            "danger"
        ));
        tasks.add(new DashboardTaskView(
            "Đánh giá chờ duyệt",
            feedback.countReviewsWaitingForApproval(),
            "/admin/feedback",
            "info"
        ));
        return tasks;
    }

    private List<RecentOrderView> recentOrders() {
        List<RecentOrderView> views = new ArrayList<>();
        for (SalesOrder order : orderStatistics.latestOrders()) {
            String statusLabel = statusLabel(order.getStatus());
            String statusTone = statusTone(order.getStatus());
            String formattedCreatedAt = formatVietnamTime(order.getCreatedAt());
            views.add(RecentOrderView.entityToDto(order, statusLabel, statusTone, formattedCreatedAt));
        }
        return views;
    }

    private List<TopProductView> topProducts(LocalDate today) {
        LocalDateTime from = toUtc(today.minusDays(TOP_PRODUCT_DAYS - 1L));
        List<TopProductView> views = new ArrayList<>();
        for (ProductSalesSummary summary : orderStatistics.topCompletedProducts(from, 5)) {
            views.add(TopProductView.fromSummary(summary));
        }
        return views;
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
