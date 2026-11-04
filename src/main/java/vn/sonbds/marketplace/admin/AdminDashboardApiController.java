package vn.sonbds.marketplace.admin;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardApiController {
    private final AdminDashboardService dashboard;

    public AdminDashboardApiController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public AdminDashboardData dashboard(@Valid @ModelAttribute AdminDashboardFilter filter) {
        return dashboard.build(filter);
    }
}
