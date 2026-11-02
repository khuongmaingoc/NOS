package vn.sonbds.marketplace.realestate;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountRepository;
import vn.sonbds.marketplace.account.AccountService;
import vn.sonbds.marketplace.account.CurrentAccount;
import vn.sonbds.marketplace.account.RegistrationRequest;
import vn.sonbds.marketplace.account.Role;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MarketplaceApiController {
    private final AccountService accountService;
    private final AccountRepository accounts;
    private final PropertyListingRepository listings;
    private final LandProjectRepository projects;
    private final AccessGrantRepository grants;
    private final ResourceAccessService access;
    private final FavoriteRepository favorites;
    private final InquiryRepository inquiries;

    public MarketplaceApiController(AccountService accountService, AccountRepository accounts,
                                    PropertyListingRepository listings, LandProjectRepository projects,
                                    AccessGrantRepository grants, ResourceAccessService access,
                                    FavoriteRepository favorites, InquiryRepository inquiries) {
        this.accountService = accountService;
        this.accounts = accounts;
        this.listings = listings;
        this.projects = projects;
        this.grants = grants;
        this.access = access;
        this.favorites = favorites;
        this.inquiries = inquiries;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegistrationRequest request) {
        Role role = request.role() == null ? Role.CUSTOMER : request.role();
        if (role == Role.ADMIN) throw new IllegalArgumentException("Không thể tự đăng ký quyền quản trị");
        Account account = accountService.register(request.name(), request.email(), request.password(), role);
        return Map.of("id", account.getId(), "name", account.getName(), "email", account.getEmail(),
                "roles", account.getRoles());
    }

    @GetMapping("/listings")
    public List<PropertyListing> allListings(Authentication authentication,
                                             @RequestParam(required = false) String transactionType) {
        Account account = CurrentAccount.from(authentication);
        return listings.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> access.canView(item, id(account), CurrentAccount.isAdmin(account)))
                .filter(item -> transactionType == null || item.getTransactionType().equalsIgnoreCase(transactionType))
                .toList();
    }

    @GetMapping("/listings/{id}")
    public PropertyListing listing(@PathVariable Long id, Authentication authentication) {
        PropertyListing listing = listings.findById(id).orElseThrow(() -> notFound("Không tìm thấy tin"));
        Account account = CurrentAccount.from(authentication);
        if (!access.canView(listing, id(account), CurrentAccount.isAdmin(account))) throw notFound("Không tìm thấy tin");
        return listing;
    }

    @GetMapping("/my/listings")
    public List<PropertyListing> myListings(Authentication authentication) {
        return listings.findByOwnerIdOrderByCreatedAtDesc(requireAccount(authentication).getId());
    }

    @PostMapping("/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyListing createListing(@Valid @RequestBody ListingRequest request, Authentication authentication) {
        Account account = requireSeller(authentication);
        return listings.save(new PropertyListing(account.getId(), request.title(), request.address(),
                request.description(), request.price(), request.area(), request.transactionType(),
                request.propertyType(), visibility(request.visibility()), request.imageUrls()));
    }

    @PutMapping("/listings/{id}")
    public PropertyListing updateListing(@PathVariable Long id, @Valid @RequestBody ListingRequest request,
                                         Authentication authentication) {
        PropertyListing item = listingForEdit(id, authentication, ResourcePermission.EDIT);
        item.update(request.title(), request.address(), request.description(), request.price(), request.area(),
                request.transactionType(), request.propertyType(), visibility(request.visibility()), request.imageUrls());
        return listings.save(item);
    }

    @PatchMapping("/listings/{id}/status")
    public PropertyListing updateListingStatus(@PathVariable Long id,
                                               @Valid @RequestBody ListingStatusRequest request,
                                               Authentication authentication) {
        PropertyListing item = listingForEdit(id, authentication, ResourcePermission.EDIT);
        item.setStatus(request.status());
        return listings.save(item);
    }

    @DeleteMapping("/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(@PathVariable Long id, Authentication authentication) {
        PropertyListing item = listingForEdit(id, authentication, ResourcePermission.DELETE);
        listings.delete(item);
    }

    @GetMapping("/projects")
    public List<LandProject> allProjects(Authentication authentication) {
        Account account = CurrentAccount.from(authentication);
        return projects.findAllByOrderByCreatedAtDesc().stream()
                .filter(item -> access.canView(item, id(account), CurrentAccount.isAdmin(account))).toList();
    }

    @GetMapping("/projects/{id}")
    public LandProject project(@PathVariable Long id, Authentication authentication) {
        LandProject project = projects.findById(id).orElseThrow(() -> notFound("Không tìm thấy dự án"));
        Account account = CurrentAccount.from(authentication);
        if (!access.canView(project, id(account), CurrentAccount.isAdmin(account))) throw notFound("Không tìm thấy dự án");
        return project;
    }

    @GetMapping("/my/projects")
    public List<LandProject> myProjects(Authentication authentication) {
        return projects.findByOwnerIdOrderByCreatedAtDesc(requireAccount(authentication).getId());
    }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public LandProject createProject(@Valid @RequestBody ProjectRequest request, Authentication authentication) {
        Account account = requireSeller(authentication);
        return projects.save(new LandProject(account.getId(), request.name(), request.address(),
                request.description(), request.plotCount(), visibility(request.visibility())));
    }

    @PutMapping("/projects/{id}")
    public LandProject updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest request,
                                     Authentication authentication) {
        LandProject item = projectForEdit(id, authentication, ResourcePermission.EDIT);
        item.update(request.name(), request.address(), request.description(), request.plotCount(),
                visibility(request.visibility()));
        return projects.save(item);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id, Authentication authentication) {
        LandProject item = projectForEdit(id, authentication, ResourcePermission.DELETE);
        projects.delete(item);
    }

    @PostMapping("/access-grants")
    @ResponseStatus(HttpStatus.CREATED)
    public AccessGrant grant(@Valid @RequestBody GrantRequest request, Authentication authentication) {
        Account actor = requireAccount(authentication);
        requireResourceOwner(request.resourceType(), request.resourceId(), actor);
        if (actor.getId().equals(request.accountId())) throw new IllegalArgumentException("Không cần cấp quyền cho chính mình");
        if (!accounts.existsById(request.accountId())) throw notFound("Không tìm thấy tài khoản được cấp quyền");
        if (grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                request.resourceType(), request.resourceId(), request.accountId(), request.permission())) {
            throw new IllegalArgumentException("Quyền này đã được cấp");
        }
        return grants.save(new AccessGrant(request.resourceType(), request.resourceId(),
                request.accountId(), request.permission()));
    }

    @GetMapping("/access-grants")
    public List<AccessGrant> resourceGrants(@RequestParam ResourceType resourceType,
                                            @RequestParam Long resourceId, Authentication authentication) {
        Account actor = requireAccount(authentication);
        requireResourceOwner(resourceType, resourceId, actor);
        return grants.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @DeleteMapping("/access-grants/{grantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeGrant(@PathVariable Long grantId, Authentication authentication) {
        AccessGrant grant = grants.findById(grantId).orElseThrow(() -> notFound("Không tìm thấy quyền"));
        requireResourceOwner(grant.getResourceType(), grant.getResourceId(), requireAccount(authentication));
        grants.delete(grant);
    }

    @PostMapping("/listings/{id}/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public void favorite(@PathVariable Long id, Authentication authentication) {
        Account customer = requireRole(authentication, Role.CUSTOMER);
        PropertyListing item = listings.findById(id).orElseThrow(() -> notFound("Không tìm thấy tin"));
        if (!access.canView(item, customer.getId(), CurrentAccount.isAdmin(customer))) throw notFound("Không tìm thấy tin");
        if (!favorites.existsByAccountIdAndListingId(customer.getId(), id)) {
            favorites.save(new Favorite(customer.getId(), id));
        }
    }

    @DeleteMapping("/listings/{id}/favorites")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@PathVariable Long id, Authentication authentication) {
        Favorite favorite = favorites.findByAccountIdAndListingId(requireAccount(authentication).getId(), id)
                .orElseThrow(() -> notFound("Tin chưa được lưu"));
        favorites.delete(favorite);
    }

    @GetMapping("/my/favorites")
    public List<PropertyListing> myFavorites(Authentication authentication) {
        return favorites.findByAccountIdOrderByCreatedAtDesc(requireAccount(authentication).getId()).stream()
                .map(favorite -> listings.findById(favorite.getListingId()).orElse(null))
                .filter(item -> item != null).toList();
    }

    @PostMapping("/listings/{id}/inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    public Inquiry inquire(@PathVariable Long id, @Valid @RequestBody InquiryRequest request,
                           Authentication authentication) {
        Account customer = requireRole(authentication, Role.CUSTOMER);
        PropertyListing listing = listings.findById(id).orElseThrow(() -> notFound("Không tìm thấy tin"));
        if (listing.getStatus() != ListingStatus.ACTIVE) throw new IllegalArgumentException("Tin không còn nhận liên hệ");
        if (!access.canView(listing, customer.getId(), false)) throw notFound("Không tìm thấy tin");
        if (listing.getOwnerId().equals(customer.getId())) throw new IllegalArgumentException("Không thể tự gửi liên hệ cho tin của bạn");
        return inquiries.save(new Inquiry(id, customer.getId(), listing.getOwnerId(), request.message().trim()));
    }

    @GetMapping("/my/inquiries")
    public List<Inquiry> myInquiries(Authentication authentication) {
        return inquiries.findByCustomerIdOrderByCreatedAtDesc(requireAccount(authentication).getId());
    }

    @GetMapping("/seller/inquiries")
    public List<Inquiry> sellerInquiries(Authentication authentication) {
        return inquiries.findBySellerIdOrderByCreatedAtDesc(requireSeller(authentication).getId());
    }

    @PatchMapping("/inquiries/{id}/status")
    public Inquiry updateInquiryStatus(@PathVariable Long id, @Valid @RequestBody InquiryStatusRequest request,
                                       Authentication authentication) {
        Account seller = requireSeller(authentication);
        Inquiry inquiry = inquiries.findById(id).orElseThrow(() -> notFound("Không tìm thấy liên hệ"));
        if (!seller.getId().equals(inquiry.getSellerId()) && !CurrentAccount.isAdmin(seller)) {
            throw new AccessDeniedException("Chỉ người bán của tin mới được cập nhật liên hệ");
        }
        inquiry.setStatus(request.status());
        return inquiries.save(inquiry);
    }

    private PropertyListing listingForEdit(Long id, Authentication authentication, ResourcePermission permission) {
        PropertyListing listing = listings.findById(id).orElseThrow(() -> notFound("Không tìm thấy tin"));
        Account account = requireAccount(authentication);
        access.requireListing(listing, account.getId(), CurrentAccount.isAdmin(account), permission);
        return listing;
    }

    private LandProject projectForEdit(Long id, Authentication authentication, ResourcePermission permission) {
        LandProject project = projects.findById(id).orElseThrow(() -> notFound("Không tìm thấy dự án"));
        Account account = requireAccount(authentication);
        access.requireProject(project, account.getId(), CurrentAccount.isAdmin(account), permission);
        return project;
    }

    private void requireResourceOwner(ResourceType type, Long resourceId, Account actor) {
        if (type == ResourceType.LISTING) {
            PropertyListing item = listings.findById(resourceId).orElseThrow(() -> notFound("Không tìm thấy tin"));
            if (!CurrentAccount.isAdmin(actor) && !item.getOwnerId().equals(actor.getId())) {
                throw new AccessDeniedException("Chỉ chủ tin mới được cấp hoặc thu hồi quyền");
            }
        } else {
            LandProject item = projects.findById(resourceId).orElseThrow(() -> notFound("Không tìm thấy dự án"));
            if (!CurrentAccount.isAdmin(actor) && !item.getOwnerId().equals(actor.getId())) {
                throw new AccessDeniedException("Chỉ chủ dự án mới được cấp hoặc thu hồi quyền");
            }
        }
    }

    private Account requireSeller(Authentication authentication) {
        return requireRole(authentication, Role.SELLER);
    }

    private Account requireRole(Authentication authentication, Role role) {
        Account account = requireAccount(authentication);
        if (!CurrentAccount.isAdmin(account) && !account.getRoles().contains(role)) {
            throw new AccessDeniedException("Tài khoản cần có quyền " + role);
        }
        return account;
    }

    private Account requireAccount(Authentication authentication) {
        Account account = CurrentAccount.from(authentication);
        if (account == null) throw new AccessDeniedException("Yêu cầu đăng nhập");
        return account;
    }

    private static Long id(Account account) { return account == null ? null : account.getId(); }

    private static Visibility visibility(Visibility visibility) {
        return visibility == null ? Visibility.PUBLIC : visibility;
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
