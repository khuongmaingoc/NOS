package vn.sonbds.marketplace.admin;

import vn.sonbds.marketplace.realestate.CustomerLead;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record AdminDashboardData(
        long listingCount,
        BigDecimal averageArea,
        Double minArea,
        Double maxArea,
        List<PriceStats> priceStats,
        List<GroupStats> groupStats,
        List<AccountStats> sellers,
        List<AccountStats> customers,
        List<LeadStats> leads
) {
    public record PriceStats(String unit, BigDecimal average, BigDecimal min, BigDecimal max) { }
    public record GroupStats(String label, long count, BigDecimal averageArea) { }
    public record AccountStats(Long id, String name, String email, String phone, Instant createdAt,
                               Set<String> roles, long listingCount, long leadCount) { }
    public record LeadStats(CustomerLead lead, String ownerName) { }
}
