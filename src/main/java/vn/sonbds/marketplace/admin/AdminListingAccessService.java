package vn.sonbds.marketplace.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountRepository;
import vn.sonbds.marketplace.account.Role;
import vn.sonbds.marketplace.realestate.AccessGrant;
import vn.sonbds.marketplace.realestate.AccessGrantRepository;
import vn.sonbds.marketplace.realestate.PropertyListing;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;
import vn.sonbds.marketplace.realestate.ResourcePermission;
import vn.sonbds.marketplace.realestate.ResourceType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminListingAccessService {
    private final AccountRepository accounts;
    private final PropertyListingRepository listings;
    private final AccessGrantRepository grants;

    public AdminListingAccessService(AccountRepository accounts, PropertyListingRepository listings,
                                     AccessGrantRepository grants) {
        this.accounts = accounts;
        this.listings = listings;
        this.grants = grants;
    }

    @Transactional(readOnly = true)
    public PageData pageData() {
        List<Account> allAccounts = accounts.findAll();
        List<PropertyListing> allListings = listings.findAllByOrderByCreatedAtDesc();
        Map<Long, String> accountNames = allAccounts.stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));
        Map<Long, PropertyListing> listingsById = allListings.stream()
                .collect(Collectors.toMap(PropertyListing::getId, listing -> listing));
        Map<Long, Account> accountsById = allAccounts.stream()
                .collect(Collectors.toMap(Account::getId, account -> account));
        List<ListingOption> listingOptions = allListings.stream()
                .map(listing -> new ListingOption(listing.getId(), listing.getTitle(), listing.getAddress(),
                        accountNames.getOrDefault(listing.getOwnerId(), "Tài khoản đã xóa")))
                .toList();
        List<CustomerOption> customerOptions = allAccounts.stream()
                .filter(account -> account.getRoles().contains(Role.CUSTOMER)
                        && !account.getRoles().contains(Role.ADMIN))
                .map(account -> new CustomerOption(account.getId(), account.getName(), account.getEmail()))
                .toList();
        List<GrantRow> currentGrants = grants.findByResourceTypeAndPermissionOrderByIdDesc(
                        ResourceType.LISTING, ResourcePermission.VIEW).stream()
                .map(grant -> {
                    PropertyListing listing = listingsById.get(grant.getResourceId());
                    Account customer = accountsById.get(grant.getAccountId());
                    return new GrantRow(grant.getId(),
                            listing == null ? "Tin đã xóa" : listing.getTitle(),
                            customer == null ? "Tài khoản đã xóa" : customer.getName(),
                            customer == null ? "" : customer.getEmail());
                })
                .toList();
        return new PageData(listingOptions, customerOptions, currentGrants);
    }

    @Transactional
    public void grantView(Long listingId, Long accountId) {
        PropertyListing listing = listings.findById(listingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tin bất động sản."));
        Account customer = accounts.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản đầu khách."));
        if (!customer.getRoles().contains(Role.CUSTOMER) || customer.getRoles().contains(Role.ADMIN)) {
            throw new IllegalArgumentException("Chỉ có thể cấp quyền xem tin cho tài khoản đầu khách.");
        }
        if (grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                ResourceType.LISTING, listing.getId(), customer.getId(), ResourcePermission.VIEW)) {
            throw new IllegalArgumentException("Đầu khách này đã được cấp quyền xem tin.");
        }
        grants.save(new AccessGrant(ResourceType.LISTING, listing.getId(), customer.getId(), ResourcePermission.VIEW));
    }

    @Transactional
    public void revokeView(Long grantId) {
        AccessGrant grant = grants.findById(grantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quyền đã cấp."));
        if (grant.getResourceType() != ResourceType.LISTING || grant.getPermission() != ResourcePermission.VIEW) {
            throw new IllegalArgumentException("Chỉ có thể thu hồi quyền xem tin bất động sản tại đây.");
        }
        grants.delete(grant);
    }

    public record PageData(List<ListingOption> listings, List<CustomerOption> customers, List<GrantRow> grants) { }
    public record ListingOption(Long id, String title, String address, String ownerName) { }
    public record CustomerOption(Long id, String name, String email) { }
    public record GrantRow(Long id, String listingTitle, String customerName, String customerEmail) { }
}
