package vn.sonbds.marketplace.admin;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AdminDashboardController {
    private final AdminDashboardService dashboard;

    public AdminDashboardController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/admin")
    public String adminDashboard(@Valid @ModelAttribute("filter") AdminDashboardFilter filter, Model model) {
        AdminDashboardData data = dashboard.build(filter);
        model.addAttribute("data", data);
        model.addAttribute("groupOptions", AdminDashboardService.groupOptions());
        model.addAttribute("listingStatuses", vn.sonbds.marketplace.realestate.ListingStatus.values());
        model.addAttribute("priceUnits", new String[]{"ALL", "VND", "$"});
        return "admin-dashboard";
    }
}
