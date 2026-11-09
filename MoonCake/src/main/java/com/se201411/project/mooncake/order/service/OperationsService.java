package com.se201411.project.mooncake.order.service;

import com.se201411.project.mooncake.order.dto.BusinessSummaryView;
import com.se201411.project.mooncake.order.dto.OverviewStatsView;

import java.util.List;

public interface OperationsService {

    OverviewStatsView statistics();

    List<BusinessSummaryView> businesses();
}
