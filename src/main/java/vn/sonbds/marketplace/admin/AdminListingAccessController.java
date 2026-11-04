package vn.sonbds.marketplace.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminListingAccessController {
    private final AdminListingAccessService listingAccess;

    public AdminListingAccessController(AdminListingAccessService listingAccess) {
        this.listingAccess = listingAccess;
    }

    @GetMapping("/admin/permissions")
    public String permissions(Model model) {
        model.addAttribute("pageData", listingAccess.pageData());
        return "admin-permissions";
    }

    @PostMapping("/admin/permissions")
    public String grantView(@RequestParam Long listingId, @RequestParam Long accountId,
                            RedirectAttributes redirect) {
        try {
            listingAccess.grantView(listingId, accountId);
            redirect.addFlashAttribute("notice", "Đã cấp quyền xem tin cho đầu khách.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/permissions";
    }

    @PostMapping("/admin/permissions/{grantId}/revoke")
    public String revokeView(@PathVariable Long grantId, RedirectAttributes redirect) {
        try {
            listingAccess.revokeView(grantId);
            redirect.addFlashAttribute("notice", "Đã thu hồi quyền xem tin.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/admin/permissions";
    }
}
