package com.se201411.project.mooncake.order.service.impl;

import com.se201411.project.mooncake.enums.OrderStatus;
import com.se201411.project.mooncake.order.dto.BusinessSummaryView;
import com.se201411.project.mooncake.order.dto.OverviewStatsView;
import com.se201411.project.mooncake.order.repository.AdminOverviewRepository;
import com.se201411.project.mooncake.order.service.OperationsService;
import com.se201411.project.mooncake.security.service.AdminAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OperationsServiceImpl implements OperationsService {

    private final AdminOverviewRepository adminOverviewRepository;
    private final AdminAccessService adminAccessService;

    public OperationsServiceImpl(
        AdminOverviewRepository adminOverviewRepository,
        AdminAccessService adminAccessService
    ) {
        this.adminOverviewRepository = adminOverviewRepository;
        this.adminAccessService = adminAccessService;
    }

    @Override
    @Transactional(readOnly = true)
    public OverviewStatsView statistics() {
        adminAccessService.requireAdmin();
        return adminOverviewRepository.statistics(
            OrderStatus.PLACED,
            OrderStatus.COMPLETED,
            OrderStatus.CANCELLED
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BusinessSummaryView> businesses() {
        adminAccessService.requireAdmin();
        return adminOverviewRepository.businessSummaries();
    }
}
