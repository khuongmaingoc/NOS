package vn.sonbds.marketplace.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountRepository;
import vn.sonbds.marketplace.account.Role;
import vn.sonbds.marketplace.realestate.AccessGrant;
import vn.sonbds.marketplace.realestate.AccessGrantRepository;
import vn.sonbds.marketplace.realestate.PropertyListing;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;
import vn.sonbds.marketplace.realestate.ResourcePermission;
import vn.sonbds.marketplace.realestate.ResourceType;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminListingAccessServiceTest {
    private AccountRepository accounts;
    private PropertyListingRepository listings;
    private AccessGrantRepository grants;
    private AdminListingAccessService service;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        listings = mock(PropertyListingRepository.class);
        grants = mock(AccessGrantRepository.class);
        service = new AdminListingAccessService(accounts, listings, grants);
    }

    @Test
    void grantsOnlyViewPermissionToCustomer() {
        PropertyListing listing = mock(PropertyListing.class);
        Account customer = customer();
        when(listings.findById(12L)).thenReturn(Optional.of(listing));
        when(listing.getId()).thenReturn(12L);
        when(accounts.findById(34L)).thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(34L);

        service.grantView(12L, 34L);

        var grant = org.mockito.ArgumentCaptor.forClass(AccessGrant.class);
        verify(grants).save(grant.capture());
        assertEquals(ResourceType.LISTING, grant.getValue().getResourceType());
        assertEquals(12L, grant.getValue().getResourceId());
        assertEquals(34L, grant.getValue().getAccountId());
        assertEquals(ResourcePermission.VIEW, grant.getValue().getPermission());
    }

    @Test
    void rejectsAccountsWithoutCustomerRole() {
        Account seller = mock(Account.class);
        when(listings.findById(12L)).thenReturn(Optional.of(mock(PropertyListing.class)));
        when(accounts.findById(34L)).thenReturn(Optional.of(seller));
        when(seller.getRoles()).thenReturn(Set.of(Role.SELLER));

        assertThrows(IllegalArgumentException.class, () -> service.grantView(12L, 34L));
        verify(grants, never()).save(any());
    }

    @Test
    void rejectsDuplicateViewGrant() {
        PropertyListing listing = mock(PropertyListing.class);
        Account customer = customer();
        when(listings.findById(12L)).thenReturn(Optional.of(listing));
        when(listing.getId()).thenReturn(12L);
        when(accounts.findById(34L)).thenReturn(Optional.of(customer));
        when(customer.getId()).thenReturn(34L);
        when(grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                ResourceType.LISTING, 12L, 34L, ResourcePermission.VIEW)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.grantView(12L, 34L));
        verify(grants, never()).save(any());
    }

    @Test
    void revokeDeletesOnlyListingViewGrant() {
        AccessGrant grant = new AccessGrant(ResourceType.LISTING, 12L, 34L, ResourcePermission.VIEW);
        when(grants.findById(56L)).thenReturn(Optional.of(grant));

        service.revokeView(56L);

        verify(grants).delete(grant);
    }

    @Test
    void refusesToRevokeOtherPermissionKindsFromThisScreen() {
        AccessGrant grant = new AccessGrant(ResourceType.LISTING, 12L, 34L, ResourcePermission.EDIT);
        when(grants.findById(56L)).thenReturn(Optional.of(grant));

        assertThrows(IllegalArgumentException.class, () -> service.revokeView(56L));
        verify(grants, never()).delete(any(AccessGrant.class));
    }

    private static Account customer() {
        Account customer = mock(Account.class);
        when(customer.getRoles()).thenReturn(Set.of(Role.CUSTOMER));
        return customer;
    }
}
