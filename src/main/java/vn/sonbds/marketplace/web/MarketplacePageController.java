package vn.sonbds.marketplace.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountService;
import vn.sonbds.marketplace.account.CurrentAccount;
import vn.sonbds.marketplace.account.RegistrationRequest;
import vn.sonbds.marketplace.account.Role;
import vn.sonbds.marketplace.realestate.InquiryRepository;
import vn.sonbds.marketplace.realestate.CustomerLead;
import vn.sonbds.marketplace.realestate.CustomerLeadRepository;
import vn.sonbds.marketplace.realestate.LeadExcelImportService;
import vn.sonbds.marketplace.realestate.LeadOptions;
import vn.sonbds.marketplace.realestate.LeadRequest;
import vn.sonbds.marketplace.realestate.LandProject;
import vn.sonbds.marketplace.realestate.LandProjectRepository;
import vn.sonbds.marketplace.realestate.ListingStatus;
import vn.sonbds.marketplace.realestate.ListingRequest;
import vn.sonbds.marketplace.realestate.PropertyListing;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;
import vn.sonbds.marketplace.realestate.ResourceAccessService;
import vn.sonbds.marketplace.realestate.Visibility;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Controller
public class MarketplacePageController {
    private final AccountService accounts;
    private final PropertyListingRepository listings;
    private final LandProjectRepository projects;
    private final InquiryRepository inquiries;
    private final ResourceAccessService access;
    private final CustomerLeadRepository leads;
    private final LeadExcelImportService leadExcelImport;

    public MarketplacePageController(AccountService accounts, PropertyListingRepository listings,
                                     LandProjectRepository projects, InquiryRepository inquiries,
                                     ResourceAccessService access, CustomerLeadRepository leads,
                                     LeadExcelImportService leadExcelImport) {
        this.accounts = accounts;
        this.listings = listings;
        this.projects = projects;
        this.inquiries = inquiries;
        this.access = access;
        this.leads = leads;
        this.leadExcelImport = leadExcelImport;
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

    @GetMapping("/listings/{id}")
    public String listingDetails(@org.springframework.web.bind.annotation.PathVariable Long id,
                                 Authentication authentication, Model model) {
        PropertyListing listing = listings.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy tin bất động sản"));
        Account account = CurrentAccount.from(authentication);
        if (!access.canView(listing, id(account), CurrentAccount.isAdmin(account))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy tin bất động sản");
        }
        model.addAttribute("account", account);
        model.addAttribute("seller", account != null && (CurrentAccount.isAdmin(account)
                || account.getRoles().contains(Role.SELLER)));
        model.addAttribute("canEdit", access.canEdit(listing, id(account), CurrentAccount.isAdmin(account)));
        model.addAttribute("listing", listing);
        return "listing-detail";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegistrationRequest("", "", "", "", Role.CUSTOMER));
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
            accounts.register(form.name(), form.email(), form.password(), form.phone(), role);
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
        model.addAttribute("leadCount", leads.findByOwnerIdOrderByUpdatedAtDesc(account.getId()).size());
        return "dashboard";
    }

    @GetMapping("/listings/new")
    public String newListing(Authentication authentication, Model model) {
        Account seller = requireSeller(authentication);
        prepareListingForm(model, seller, new ListingForm());
        return "listing-form";
    }

    @PostMapping("/listings/new")
    public String createListing(@Valid @ModelAttribute("form") ListingForm form, BindingResult errors,
                                Authentication authentication, Model model, RedirectAttributes redirect) {
        Account seller = requireSeller(authentication);
        if (errors.hasErrors()) {
            prepareListingForm(model, seller, form);
            model.addAttribute("errors", errors.getFieldErrors());
            return "listing-form";
        }
        PropertyListing listing = listings.save(new PropertyListing(seller.getId(), form.getTitle(), form.getAddress(),
                form.getDescription(), form.getPrice(), form.getArea(), form.getTransactionType(), form.getPropertyType(),
                visibility(form.getVisibility()), imageUrls(form.getImageUrls()), form.getPropertyFeature(),
                form.getDirection(), form.getPosition(), form.getInterior(), form.getLegalStatus(),
                form.getPriceUnit(), form.getPropertyStatus(), form.getLatitude(), form.getLongitude()));
        redirect.addFlashAttribute("notice", "Đã đăng tin bất động sản.");
        return "redirect:/dashboard";
    }

    @GetMapping("/listings/{id}/edit")
    public String editListing(@org.springframework.web.bind.annotation.PathVariable Long id,
                              Authentication authentication, Model model) {
        Account account = requireAccount(authentication);
        PropertyListing listing = editableListing(id, account);
        ListingForm form = ListingForm.from(listing);
        prepareListingForm(model, account, form);
        model.addAttribute("listingId", id);
        model.addAttribute("listingAction", "/listings/" + id + "/edit");
        return "listing-form";
    }

    @PostMapping("/listings/{id}/edit")
    public String updateListing(@org.springframework.web.bind.annotation.PathVariable Long id,
                                @Valid @ModelAttribute("form") ListingForm form, BindingResult errors,
                                Authentication authentication, Model model, RedirectAttributes redirect) {
        Account account = requireAccount(authentication);
        PropertyListing listing = editableListing(id, account);
        if (errors.hasErrors()) {
            prepareListingForm(model, account, form);
            model.addAttribute("listingId", id);
            model.addAttribute("listingAction", "/listings/" + id + "/edit");
            model.addAttribute("errors", errors.getFieldErrors());
            return "listing-form";
        }
        listing.update(form.getTitle(), form.getAddress(), form.getDescription(), form.getPrice(), form.getArea(),
                form.getTransactionType(), form.getPropertyType(), visibility(form.getVisibility()),
                imageUrls(form.getImageUrls()), form.getPropertyFeature(), form.getDirection(), form.getPosition(),
                form.getInterior(), form.getLegalStatus(), form.getPriceUnit(), form.getPropertyStatus(),
                form.getLatitude(), form.getLongitude());
        listings.save(listing);
        redirect.addFlashAttribute("notice", "Đã cập nhật tin bất động sản.");
        return "redirect:/dashboard";
    }

    private PropertyListing editableListing(Long id, Account account) {
        PropertyListing listing = listings.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy tin"));
        access.requireListing(listing, account.getId(), CurrentAccount.isAdmin(account),
                vn.sonbds.marketplace.realestate.ResourcePermission.EDIT);
        return listing;
    }

    private void prepareListingForm(Model model, Account account, ListingForm form) {
        model.addAttribute("account", account);
        model.addAttribute("form", form);
        model.addAttribute("listingAction", "/listings/new");
        model.addAttribute("propertyFeatures", vn.sonbds.marketplace.realestate.LeadOptions.PROPERTY_FEATURES);
        model.addAttribute("directions", vn.sonbds.marketplace.realestate.LeadOptions.DIRECTIONS);
        model.addAttribute("positions", vn.sonbds.marketplace.realestate.LeadOptions.POSITIONS);
        model.addAttribute("interiors", vn.sonbds.marketplace.realestate.LeadOptions.INTERIORS);
        model.addAttribute("legalStatuses", vn.sonbds.marketplace.realestate.LeadOptions.LEGAL_STATUSES);
        model.addAttribute("priceUnits", vn.sonbds.marketplace.realestate.LeadOptions.PRICE_UNITS);
        model.addAttribute("propertyStatuses", vn.sonbds.marketplace.realestate.LeadOptions.PROPERTY_STATUSES);
    }

    private static Visibility visibility(Visibility visibility) {
        return visibility == null ? Visibility.PUBLIC : visibility;
    }

    private static List<String> imageUrls(String imageUrls) {
        if (imageUrls == null || imageUrls.isBlank()) return List.of();
        return Arrays.stream(imageUrls.split("\\R")).map(String::trim).filter(value -> !value.isEmpty()).toList();
    }

    @GetMapping("/leads")
    public String leadManagement(Authentication authentication, Model model) {
        Account account = requireSeller(authentication);
        List<CustomerLead> accountLeads = leads.findByOwnerIdOrderByUpdatedAtDesc(account.getId());
        Map<Long, String> listingTitles = listings.findByOwnerIdOrderByCreatedAtDesc(account.getId()).stream()
                .collect(Collectors.toMap(PropertyListing::getId, PropertyListing::getTitle));
        model.addAttribute("account", account);
        model.addAttribute("leads", accountLeads);
        model.addAttribute("listingTitles", listingTitles);
        return "leads";
    }

    @GetMapping("/leads/import-template")
    public ResponseEntity<byte[]> leadImportTemplate(Authentication authentication) throws IOException {
        requireSeller(authentication);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Khach hang CRM");
            Row header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            List<String> headers = LeadExcelImportService.templateHeaders();
            for (int index = 0; index < headers.size(); index++) {
                var cell = header.createCell(index);
                cell.setCellValue(headers.get(index));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(index, 20 * 256);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.size() - 1));
            workbook.write(output);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("mau-khach-hang-crm.xlsx").build().toString())
                    .body(output.toByteArray());
        }
    }

    @PostMapping("/leads/import")
    public String importLeads(@RequestParam("file") MultipartFile file, Authentication authentication,
                              RedirectAttributes redirect) {
        Account account = requireSeller(authentication);
        try {
            LeadExcelImportService.ImportResult result = leadExcelImport.importFile(account.getId(), file);
            redirect.addFlashAttribute("notice", "Đã nhập " + result.imported() + " khách hàng; bỏ qua "
                    + result.duplicatesSkipped() + " hồ sơ trùng; " + result.errors().size() + " dòng lỗi.");
            redirect.addFlashAttribute("importErrors", result.errors());
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("importError", exception.getMessage());
        }
        return "redirect:/leads";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleOversizedLeadImport(RedirectAttributes redirect) {
        redirect.addFlashAttribute("importError", "File Excel không được vượt quá 5 MB.");
        return "redirect:/leads";
    }

    @GetMapping("/leads/new")
    public String newLead(Authentication authentication, Model model) {
        Account account = requireSeller(authentication);
        model.addAttribute("account", account);
        model.addAttribute("leadForm", blankLeadRequest());
        model.addAttribute("leadAction", "/leads/new");
        model.addAttribute("ownedListings", listings.findByOwnerIdOrderByCreatedAtDesc(account.getId()));
        addLeadOptions(model);
        return "lead-form";
    }

    @PostMapping("/leads/new")
    public String createLead(@Valid @ModelAttribute("leadForm") LeadRequest form, BindingResult errors,
                             Authentication authentication, Model model, RedirectAttributes redirect) {
        Account account = requireSeller(authentication);
        if (errors.hasErrors()) {
            prepareLeadForm(account, form, model, errors);
            return "lead-form";
        }
        validateLeadListing(form.listingId(), account);
        leads.save(new CustomerLead(account.getId(), form.listingId(), form));
        redirect.addFlashAttribute("notice", "Đã thêm khách hàng tiềm năng.");
        return "redirect:/leads";
    }

    @GetMapping("/leads/{id}/edit")
    public String editLead(@org.springframework.web.bind.annotation.PathVariable Long id,
                           Authentication authentication, Model model) {
        Account account = requireSeller(authentication);
        CustomerLead lead = ownedLead(id, account);
        model.addAttribute("account", account);
        model.addAttribute("leadId", id);
        model.addAttribute("leadAction", "/leads/" + id + "/edit");
        model.addAttribute("leadForm", toRequest(lead));
        model.addAttribute("ownedListings", listings.findByOwnerIdOrderByCreatedAtDesc(account.getId()));
        addLeadOptions(model);
        return "lead-form";
    }

    @PostMapping("/leads/{id}/edit")
    public String updateLead(@org.springframework.web.bind.annotation.PathVariable Long id,
                             @Valid @ModelAttribute("leadForm") LeadRequest form, BindingResult errors,
                             Authentication authentication, Model model, RedirectAttributes redirect) {
        Account account = requireSeller(authentication);
        CustomerLead lead = ownedLead(id, account);
        if (errors.hasErrors()) {
            model.addAttribute("leadId", id);
            prepareLeadForm(account, form, model, errors);
            return "lead-form";
        }
        validateLeadListing(form.listingId(), account);
        lead.update(form.listingId(), form);
        leads.save(lead);
        redirect.addFlashAttribute("notice", "Đã cập nhật khách hàng.");
        return "redirect:/leads";
    }

    @PostMapping("/leads/{id}/delete")
    public String deleteLead(@org.springframework.web.bind.annotation.PathVariable Long id,
                             Authentication authentication, RedirectAttributes redirect) {
        leads.delete(ownedLead(id, requireSeller(authentication)));
        redirect.addFlashAttribute("notice", "Đã xóa khách hàng.");
        return "redirect:/leads";
    }

    private void prepareLeadForm(Account account, LeadRequest form, Model model, BindingResult errors) {
        model.addAttribute("account", account);
        model.addAttribute("leadForm", form);
        Object leadId = model.getAttribute("leadId");
        model.addAttribute("leadAction", leadId == null ? "/leads/new" : "/leads/" + leadId + "/edit");
        model.addAttribute("ownedListings", listings.findByOwnerIdOrderByCreatedAtDesc(account.getId()));
        model.addAttribute("formErrors", errors.getFieldErrors());
        addLeadOptions(model);
    }

    private void addLeadOptions(Model model) {
        model.addAttribute("sources", LeadOptions.SOURCES);
        model.addAttribute("subjects", LeadOptions.SUBJECTS);
        model.addAttribute("needs", LeadOptions.NEEDS);
        model.addAttribute("genders", LeadOptions.GENDERS);
        model.addAttribute("channels", LeadOptions.CHANNELS);
        model.addAttribute("interestLevels", LeadOptions.INTEREST_LEVELS);
        model.addAttribute("contactStatuses", LeadOptions.CONTACT_STATUSES);
        model.addAttribute("propertyFeatures", LeadOptions.PROPERTY_FEATURES);
        model.addAttribute("directions", LeadOptions.DIRECTIONS);
        model.addAttribute("positions", LeadOptions.POSITIONS);
        model.addAttribute("interiors", LeadOptions.INTERIORS);
        model.addAttribute("legalStatuses", LeadOptions.LEGAL_STATUSES);
        model.addAttribute("priceUnits", LeadOptions.PRICE_UNITS);
        model.addAttribute("propertyStatuses", LeadOptions.PROPERTY_STATUSES);
        model.addAttribute("dealStatuses", LeadOptions.DEAL_STATUSES);
    }

    private CustomerLead ownedLead(Long id, Account account) {
        CustomerLead lead = leads.findById(id).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"));
        if (!CurrentAccount.isAdmin(account) && !lead.getOwnerId().equals(account.getId())) {
            throw new AccessDeniedException("Bạn không có quyền truy cập khách hàng này.");
        }
        return lead;
    }

    private void validateLeadListing(Long listingId, Account account) {
        if (listingId == null) return;
        PropertyListing listing = listings.findById(listingId).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Không tìm thấy tin liên quan"));
        if (!CurrentAccount.isAdmin(account) && !listing.getOwnerId().equals(account.getId())) {
            throw new AccessDeniedException("Chỉ có thể liên kết tin bất động sản của bạn.");
        }
    }

    private static LeadRequest blankLeadRequest() {
        return new LeadRequest("", "", "", null, "", "", "", "", "", "",
                "", "", "", "", "", "", "", "", "", "", null, null, null);
    }

    private static LeadRequest toRequest(CustomerLead lead) {
        return new LeadRequest(lead.getName(), lead.getPhone(), lead.getEmail(), lead.getListingId(),
                lead.getSource(), lead.getSubject(), lead.getNeed(), lead.getGender(), lead.getContactChannel(),
                lead.getInterestLevel(), lead.getContactStatus(), lead.getPropertyFeature(), lead.getDirection(),
                lead.getPosition(), lead.getInterior(), lead.getLegalStatus(), lead.getPriceUnit(),
                lead.getPropertyStatus(), lead.getDealStatus(), lead.getNotes(), lead.getPropertyAddress(),
                lead.getLatitude(), lead.getLongitude());
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

    public static class ListingForm {
        @NotBlank @Size(max = 180)
        private String title;
        @NotBlank @Size(max = 300)
        private String address;
        @NotBlank
        private String description;
        @NotNull @DecimalMin("0.01")
        private BigDecimal price;
        @NotNull @Positive
        private Double area;
        @NotBlank
        private String transactionType;
        @NotBlank
        private String propertyType;
        private Visibility visibility = Visibility.PUBLIC;
        @Size(max = 5000)
        private String imageUrls;
        @Size(max = 80)
        private String propertyFeature;
        @Size(max = 80)
        private String direction;
        @Size(max = 80)
        private String position;
        @Size(max = 100)
        private String interior;
        @Size(max = 100)
        private String legalStatus;
        @Size(max = 20)
        private String priceUnit = "VND";
        @Size(max = 80)
        private String propertyStatus;
        @DecimalMin("-90.0") @DecimalMax("90.0")
        private Double latitude;
        @DecimalMin("-180.0") @DecimalMax("180.0")
        private Double longitude;

        static ListingForm from(PropertyListing listing) {
            ListingForm form = new ListingForm();
            form.title = listing.getTitle();
            form.address = listing.getAddress();
            form.description = listing.getDescription();
            form.price = listing.getPrice();
            form.area = listing.getArea();
            form.transactionType = listing.getTransactionType();
            form.propertyType = listing.getPropertyType();
            form.visibility = listing.getVisibility();
            form.imageUrls = String.join(System.lineSeparator(), listing.getImageUrls());
            form.propertyFeature = listing.getPropertyFeature();
            form.direction = listing.getDirection();
            form.position = listing.getPosition();
            form.interior = listing.getInterior();
            form.legalStatus = listing.getLegalStatus();
            form.priceUnit = listing.getPriceUnit();
            form.propertyStatus = listing.getPropertyStatus();
            form.latitude = listing.getLatitude();
            form.longitude = listing.getLongitude();
            return form;
        }

        @AssertTrue(message = "Vĩ độ và kinh độ phải được chọn cùng nhau.")
        public boolean isLocationCoordinatesComplete() {
            return vn.sonbds.marketplace.realestate.MapCoordinates.areComplete(latitude, longitude);
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public Double getArea() { return area; }
        public void setArea(Double area) { this.area = area; }
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public String getPropertyType() { return propertyType; }
        public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
        public Visibility getVisibility() { return visibility; }
        public void setVisibility(Visibility visibility) { this.visibility = visibility; }
        public String getImageUrls() { return imageUrls; }
        public void setImageUrls(String imageUrls) { this.imageUrls = imageUrls; }
        public String getPropertyFeature() { return propertyFeature; }
        public void setPropertyFeature(String propertyFeature) { this.propertyFeature = propertyFeature; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        public String getInterior() { return interior; }
        public void setInterior(String interior) { this.interior = interior; }
        public String getLegalStatus() { return legalStatus; }
        public void setLegalStatus(String legalStatus) { this.legalStatus = legalStatus; }
        public String getPriceUnit() { return priceUnit; }
        public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
        public String getPropertyStatus() { return propertyStatus; }
        public void setPropertyStatus(String propertyStatus) { this.propertyStatus = propertyStatus; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

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
