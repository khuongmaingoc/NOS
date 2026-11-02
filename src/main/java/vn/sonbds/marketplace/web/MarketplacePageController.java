package vn.sonbds.marketplace.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountService;
import vn.sonbds.marketplace.account.CurrentAccount;
import vn.sonbds.marketplace.account.RegistrationRequest;
import vn.sonbds.marketplace.account.Role;
import vn.sonbds.marketplace.realestate.InquiryRepository;
import vn.sonbds.marketplace.realestate.LandProject;
import vn.sonbds.marketplace.realestate.LandProjectRepository;
import vn.sonbds.marketplace.realestate.ListingRequest;
import vn.sonbds.marketplace.realestate.PropertyListing;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;
import vn.sonbds.marketplace.realestate.ResourceAccessService;
import vn.sonbds.marketplace.realestate.Visibility;

import java.util.List;

@Controller
public class MarketplacePageController {
    private final AccountService accounts;
    private final PropertyListingRepository listings;
    private final LandProjectRepository projects;
    private final InquiryRepository inquiries;
    private final ResourceAccessService access;

    public MarketplacePageController(AccountService accounts, PropertyListingRepository listings,
                                     LandProjectRepository projects, InquiryRepository inquiries,
                                     ResourceAccessService access) {
        this.accounts = accounts;
        this.listings = listings;
        this.projects = projects;
        this.inquiries = inquiries;
        this.access = access;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        Account account = CurrentAccount.from(authentication);
        model.addAttribute("account", account);
        model.addAttribute("seller", account != null && (CurrentAccount.isAdmin(account)
                || account.getRoles().contains(Role.SELLER)));
        model.addAttribute("listings", listings.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> access.canView(item, id(account), CurrentAccount.isAdmin(account))).toList());
        model.addAttribute("projects", projects.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> access.canView(item, id(account), CurrentAccount.isAdmin(account))).toList());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationRequest("", "", "", Role.CUSTOMER));
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationRequest form, BindingResult errors,
                           Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return "register";
        Role role = form.role() == null ? Role.CUSTOMER : form.role();
        if (role == Role.ADMIN) {
            model.addAttribute("formError", "Không thể tự đăng ký quyền quản trị.");
            return "register";
        }
        try {
            accounts.register(form.name(), form.email(), form.password(), role);
        } catch (IllegalArgumentException exception) {
            model.addAttribute("formError", exception.getMessage());
            return "register";
        }
        redirect.addFlashAttribute("notice", "Tạo tài khoản thành công. Hãy đăng nhập để tiếp tục.");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        Account account = requireAccount(authentication);
        model.addAttribute("account", account);
        model.addAttribute("seller", CurrentAccount.isAdmin(account) || account.getRoles().contains(Role.SELLER));
        model.addAttribute("listings", listings.findByOwnerIdOrderByCreatedAtDesc(account.getId()));
        model.addAttribute("projects", projects.findByOwnerIdOrderByCreatedAtDesc(account.getId()));
        model.addAttribute("inquiries", inquiries.findBySellerIdOrderByCreatedAtDesc(account.getId()));
        return "dashboard";
    }

    @GetMapping("/listings/new")
    public String newListing(Authentication authentication) {
        requireSeller(authentication);
        return "listing-form";
    }

    @PostMapping("/listings/new")
    public String createListing(@Valid @ModelAttribute ListingRequest form, BindingResult errors,
                                Authentication authentication, Model model, RedirectAttributes redirect) {
        Account seller = requireSeller(authentication);
        if (errors.hasErrors()) {
            model.addAttribute("errors", errors.getFieldErrors());
            return "listing-form";
        }
        List<String> imageUrls = form.imageUrls() == null ? List.of() : form.imageUrls();
        PropertyListing listing = listings.save(new PropertyListing(seller.getId(), form.title(), form.address(),
                form.description(), form.price(), form.area(), form.transactionType(), form.propertyType(),
                form.visibility() == null ? Visibility.PUBLIC : form.visibility(), imageUrls));
        redirect.addFlashAttribute("notice", "Đã đăng tin bất động sản.");
        return "redirect:/dashboard";
    }

    @GetMapping("/projects/new")
    public String newProject(Authentication authentication, Model model) {
        requireSeller(authentication);
        model.addAttribute("landProjectForm", new LandProjectForm());
        return "project-form";
    }

    @PostMapping("/projects/new")
    public String createProject(@Valid @ModelAttribute LandProjectForm form,
                                BindingResult errors, Authentication authentication, Model model,
                                RedirectAttributes redirect) {
        Account seller = requireSeller(authentication);
        if (errors.hasErrors()) {
            model.addAttribute("landProjectForm", form);
            model.addAttribute("projectErrors", errors.getFieldErrors());
            return "project-form";
        }
        projects.save(new LandProject(seller.getId(), form.name(), form.address(), form.description(),
                form.plotCount(), form.visibility() == null ? Visibility.PUBLIC : form.visibility()));
        redirect.addFlashAttribute("notice", "Đã tạo dự án phân lô.");
        return "redirect:/dashboard";
    }

    private Account requireSeller(Authentication authentication) {
        Account account = requireAccount(authentication);
        if (!CurrentAccount.isAdmin(account) && !account.getRoles().contains(Role.SELLER)) {
            throw new AccessDeniedException("Tài khoản cần có quyền người bán.");
        }
        return account;
    }

    private Account requireAccount(Authentication authentication) {
        Account account = CurrentAccount.from(authentication);
        if (account == null) throw new AccessDeniedException("Yêu cầu đăng nhập.");
        return account;
    }

    private static Long id(Account account) { return account == null ? null : account.getId(); }

    public static class LandProjectForm {
        @NotBlank @Size(max = 180)
        private String name;
        @NotBlank @Size(max = 300)
        private String address;
        @NotBlank
        private String description;
        @PositiveOrZero
        private Integer plotCount;
        private Visibility visibility;

        public String name() { return name; }
        public String address() { return address; }
        public String description() { return description; }
        public Integer plotCount() { return plotCount; }
        public Visibility visibility() { return visibility; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getPlotCount() { return plotCount; }
        public void setPlotCount(Integer plotCount) { this.plotCount = plotCount; }
        public Visibility getVisibility() { return visibility; }
        public void setVisibility(Visibility visibility) { this.visibility = visibility; }
    }
}
