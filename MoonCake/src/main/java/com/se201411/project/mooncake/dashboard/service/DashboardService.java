package com.se201411.project.mooncake.dashboard.service;

import com.se201411.project.mooncake.dashboard.dto.DashboardPeriod;
import com.se201411.project.mooncake.dashboard.dto.DashboardView;

public interface DashboardService {

    DashboardView dashboard(DashboardPeriod period);
}
