package com.portfolio.warehouse.dashboard.api;

import com.portfolio.warehouse.dashboard.api.dto.AdminDashboardResponse;
import com.portfolio.warehouse.dashboard.service.AdminDashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService service;

    public AdminDashboardController(AdminDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public AdminDashboardResponse dashboard() {
        return service.dashboard();
    }
}
