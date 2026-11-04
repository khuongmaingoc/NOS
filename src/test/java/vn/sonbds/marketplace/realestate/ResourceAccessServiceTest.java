package vn.sonbds.marketplace.realestate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(access.canEdit(listing, 20L, false));
    }

    @Test
    void viewGrantDoesNotAllowEditing() {
        PropertyListing listing = listing(10L, Visibility.PRIVATE);
        when(grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                ResourceType.LISTING, 1L, 20L, ResourcePermission.VIEW)).thenReturn(true);

        assertTrue(access.canView(listing, 20L, false));
        assertFalse(access.canEdit(listing, 20L, false));
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

    @Test
    void leadKeepsCustomSpreadsheetValues() {
        LeadRequest request = new LeadRequest("  Khách A  ", "0900000000", null, null,
                "Nguồn hội thảo", "Nhà đầu tư", "Mua để ở", "Khác", "Điện thoại",
                "Đang xem xét", "Đã hẹn tư vấn", "3PN", "Nam", "Góc", "Nội thất cao cấp",
                "Sổ riêng", "VND", "Sẵn sàng", "Đang thương lượng", "  Gọi buổi chiều  ");

        CustomerLead lead = new CustomerLead(7L, null, request);

        assertEquals("Khách A", lead.getName());
        assertEquals("Nguồn hội thảo", lead.getSource());
        assertEquals("3PN", lead.getPropertyFeature());
        assertEquals("Gọi buổi chiều", lead.getNotes());
    }

    @Test
    void leadStoresMapCoordinatesAndCustomerPropertyArea() {
        LeadRequest request = new LeadRequest("Khách A", "0900000000", null, null,
                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                "Quận 1, TP.HCM", 10.7769, 106.7009);

        CustomerLead lead = new CustomerLead(7L, null, request);

        assertEquals("Quận 1, TP.HCM", lead.getPropertyAddress());
        assertEquals(10.7769, lead.getLatitude());
        assertEquals(106.7009, lead.getLongitude());
    }

    @Test
    void rejectsIncompleteMapCoordinates() {
        assertThrows(IllegalArgumentException.class,
                () -> MapCoordinates.validate(10.7769, null));
    }

    @Test
    void rejectsMapCoordinatesOutsideEarthBounds() {
        assertThrows(IllegalArgumentException.class,
                () -> MapCoordinates.validate(91d, 106.7009));
    }

    @Test
    void listingStoresCoordinatesForItsMapLocation() {
        PropertyListing listing = new PropertyListing(10L, "Tin thử", "Quận 7", "Mô tả",
                BigDecimal.valueOf(1_000_000), 80d, "Bán", "Nhà", Visibility.PUBLIC, List.of(),
                null, null, null, null, null, "VND", null, 10.7291, 106.7197);

        assertEquals(10.7291, listing.getLatitude());
        assertEquals(106.7197, listing.getLongitude());
    }

    @Test
    void listingPriceRoundsHalfUpToNearestThousandAndRemainsAnInteger() {
        PropertyListing down = listingWithPrice(new BigDecimal("1234499"));
        PropertyListing up = listingWithPrice(new BigDecimal("1234500"));

        assertEquals(new BigDecimal("1234000"), down.getPrice());
        assertEquals(new BigDecimal("1235000"), up.getPrice());
        assertEquals(0, up.getPrice().scale());
    }

    @Test
    void listingPriceMustRemainPositiveAfterRounding() {
        assertThrows(IllegalArgumentException.class,
                () -> listingWithPrice(new BigDecimal("499")));
    }

    private static PropertyListing listing(Long ownerId, Visibility visibility) {
        return listingWithPrice(ownerId, BigDecimal.valueOf(100_000), visibility);
    }

    private static PropertyListing listingWithPrice(BigDecimal price) {
        return listingWithPrice(10L, price, Visibility.PUBLIC);
    }

    private static PropertyListing listingWithPrice(Long ownerId, BigDecimal price, Visibility visibility) {
        PropertyListing listing = new PropertyListing(ownerId, "Tin thử", "Địa chỉ thử", "Mô tả",
                price, 80d, "Bán", "Nhà", visibility, List.of(),
                null, null, null, null, null, "VND", null);
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
