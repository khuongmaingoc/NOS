package vn.sonbds.marketplace.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.sonbds.marketplace.account.AccountRepository;
import vn.sonbds.marketplace.realestate.CustomerLeadRepository;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminDashboardServiceTest {
    private AccountRepository accounts;
    private PropertyListingRepository listings;
    private CustomerLeadRepository leads;
    private AdminDashboardService dashboard;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        listings = mock(PropertyListingRepository.class);
        leads = mock(CustomerLeadRepository.class);
        dashboard = new AdminDashboardService(accounts, listings, leads);
    }

    @Test
    void rejectsReversedDateRangeBeforeQueryingData() {
        AdminDashboardFilter filter = new AdminDashboardFilter();
        filter.setFrom(LocalDate.of(2026, 2, 1));
        filter.setTo(LocalDate.of(2026, 1, 1));

        assertThrows(IllegalArgumentException.class, () -> dashboard.build(filter));
        verifyNoInteractions(accounts, listings, leads);
    }

    @Test
    void requiresCurrencyWhenFilteringPriceRange() {
        AdminDashboardFilter filter = new AdminDashboardFilter();
        filter.setMinPrice(BigDecimal.valueOf(1_000_000));
        filter.setPriceUnit("ALL");

        assertThrows(IllegalArgumentException.class, () -> dashboard.build(filter));
        verifyNoInteractions(accounts, listings, leads);
    }

    @Test
    void requiresCurrencyWhenGroupingByPriceRange() {
        AdminDashboardFilter filter = new AdminDashboardFilter();
        filter.setGroupBy("priceRange");

        assertThrows(IllegalArgumentException.class, () -> dashboard.build(filter));
        verifyNoInteractions(accounts, listings, leads);
    }
}
