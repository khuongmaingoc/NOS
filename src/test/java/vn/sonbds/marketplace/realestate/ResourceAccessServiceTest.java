package vn.sonbds.marketplace.realestate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceAccessServiceTest {
    private AccessGrantRepository grants;
    private ResourceAccessService access;

    @BeforeEach
    void setUp() {
        grants = mock(AccessGrantRepository.class);
        access = new ResourceAccessService(grants);
    }

    @Test
    void publicListingCanBeViewedByAnyone() {
        PropertyListing listing = listing(10L, Visibility.PUBLIC);

        assertTrue(access.canView(listing, null, false));
    }

    @Test
    void privateListingIsVisibleToItsOwnerOnlyByDefault() {
        PropertyListing listing = listing(10L, Visibility.PRIVATE);

        assertTrue(access.canView(listing, 10L, false));
        assertFalse(access.canView(listing, 20L, false));
    }

    @Test
    void explicitEditGrantAllowsEditingAndViewing() {
        PropertyListing listing = listing(10L, Visibility.PRIVATE);
        when(grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                ResourceType.LISTING, 1L, 20L, ResourcePermission.EDIT)).thenReturn(true);

        assertTrue(access.canView(listing, 20L, false));
    }

    @Test
    void deletePermissionDoesNotGrantEditPermission() {
        PropertyListing listing = listing(10L, Visibility.PRIVATE);
        when(grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                ResourceType.LISTING, 1L, 20L, ResourcePermission.DELETE)).thenReturn(true);

        assertTrue(access.canView(listing, 20L, false));
        assertThrows(AccessDeniedException.class,
                () -> access.requireListing(listing, 20L, false, ResourcePermission.EDIT));
    }

    private static PropertyListing listing(Long ownerId, Visibility visibility) {
        PropertyListing listing = new PropertyListing(ownerId, "Tin thử", "Địa chỉ thử", "Mô tả",
                BigDecimal.valueOf(100_000), 80d, "Bán", "Nhà", visibility, List.of());
        try {
            var id = PropertyListing.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(listing, 1L);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return listing;
    }
}
