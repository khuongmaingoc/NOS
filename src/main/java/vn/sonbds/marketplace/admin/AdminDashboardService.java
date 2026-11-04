package vn.sonbds.marketplace.admin;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.sonbds.marketplace.account.Account;
import vn.sonbds.marketplace.account.AccountRepository;
import vn.sonbds.marketplace.account.Role;
import vn.sonbds.marketplace.realestate.CustomerLead;
import vn.sonbds.marketplace.realestate.CustomerLeadRepository;
import vn.sonbds.marketplace.realestate.PropertyListing;
import vn.sonbds.marketplace.realestate.PropertyListingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {
    private static final List<String> GROUP_OPTIONS = List.of(
            "transactionType", "propertyType", "status", "direction", "location", "month", "areaRange", "priceRange");

    private final AccountRepository accounts;
    private final PropertyListingRepository listings;
    private final CustomerLeadRepository leads;

    public AdminDashboardService(AccountRepository accounts, PropertyListingRepository listings,
                                 CustomerLeadRepository leads) {
        this.accounts = accounts;
        this.listings = listings;
        this.leads = leads;
    }

    @Transactional(readOnly = true)
    public AdminDashboardData build(AdminDashboardFilter filter) {
        if (filter.getFrom() != null && filter.getTo() != null && filter.getFrom().isAfter(filter.getTo())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        }
        validateRange(filter.getMinPrice(), filter.getMaxPrice(), "Giá");
        validateRange(filter.getMinArea(), filter.getMaxArea(), "Diện tích");
        if (hasText(filter.getPriceUnit()) && !Set.of("ALL", "VND", "$").contains(filter.getPriceUnit().trim())) {
            throw new IllegalArgumentException("Đơn vị giá không được hỗ trợ.");
        }
        if ((filter.getMinPrice() != null || filter.getMaxPrice() != null || "priceRange".equals(filter.getGroupBy()))
                && (!hasText(filter.getPriceUnit()) || "ALL".equalsIgnoreCase(filter.getPriceUnit()))) {
            throw new IllegalArgumentException("Hãy chọn đơn vị giá khi thống kê theo khoảng giá.");
        }
        String groupBy = GROUP_OPTIONS.contains(filter.getGroupBy()) ? filter.getGroupBy() : "transactionType";
        List<PropertyListing> filtered = listings.findAll(specification(filter));
        List<Account> allAccounts = accounts.findAll();
        List<CustomerLead> allLeads = leads.findAll();
        List<PropertyListing> allListings = listings.findAll();
        Map<Long, Long> listingCounts = allListings.stream()
                .collect(Collectors.groupingBy(PropertyListing::getOwnerId, Collectors.counting()));
        Map<Long, Long> leadCounts = allLeads.stream()
                .collect(Collectors.groupingBy(CustomerLead::getOwnerId, Collectors.counting()));
        List<AdminDashboardData.AccountStats> sellers = accountStats(allAccounts, Role.SELLER, listingCounts, leadCounts);
        List<AdminDashboardData.AccountStats> customers = accountStats(allAccounts, Role.CUSTOMER, listingCounts, leadCounts);
        Map<Long, String> accountNames = allAccounts.stream()
                .collect(Collectors.toMap(Account::getId, Account::getName));
        List<AdminDashboardData.LeadStats> leadStats = allLeads.stream()
                .map(lead -> new AdminDashboardData.LeadStats(lead,
                        accountNames.getOrDefault(lead.getOwnerId(), "Tài khoản đã xóa")))
                .toList();

        BigDecimal averageArea = average(filtered.stream().map(item -> BigDecimal.valueOf(item.getArea())).toList());
        Double minArea = filtered.stream().map(PropertyListing::getArea).min(Double::compareTo).orElse(null);
        Double maxArea = filtered.stream().map(PropertyListing::getArea).max(Double::compareTo).orElse(null);
        return new AdminDashboardData(filtered.size(), averageArea, minArea, maxArea,
                priceStats(filtered), groupStats(filtered, groupBy), sellers, customers, leadStats);
    }

    public static List<String> groupOptions() {
        return GROUP_OPTIONS;
    }

    private Specification<PropertyListing> specification(AdminDashboardFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.getFrom() != null) {
                var start = filter.getFrom().atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (filter.getTo() != null) {
                var end = filter.getTo().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                predicates.add(builder.lessThan(root.get("createdAt"), end));
            }
            if (hasText(filter.getLocation())) {
                predicates.add(builder.like(builder.lower(root.get("address")),
                        "%" + escapeLike(filter.getLocation().trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (hasText(filter.getTransactionType())) {
                predicates.add(builder.equal(builder.lower(root.get("transactionType")),
                        filter.getTransactionType().trim().toLowerCase(Locale.ROOT)));
            }
            if (hasText(filter.getPropertyType())) {
                predicates.add(builder.like(builder.lower(root.get("propertyType")),
                        "%" + escapeLike(filter.getPropertyType().trim().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (filter.getStatus() != null) predicates.add(builder.equal(root.get("status"), filter.getStatus()));
            if (filter.getMinPrice() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            if (filter.getMaxPrice() != null) predicates.add(builder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            if (filter.getMinArea() != null) predicates.add(builder.greaterThanOrEqualTo(root.get("area"), filter.getMinArea()));
            if (filter.getMaxArea() != null) predicates.add(builder.lessThanOrEqualTo(root.get("area"), filter.getMaxArea()));
            if (hasText(filter.getPriceUnit()) && !"ALL".equalsIgnoreCase(filter.getPriceUnit())) {
                predicates.add(builder.equal(builder.lower(builder.coalesce(root.<String>get("priceUnit"), "VND")),
                        filter.getPriceUnit().trim().toLowerCase(Locale.ROOT)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private List<AdminDashboardData.AccountStats> accountStats(List<Account> allAccounts, Role role,
                                                                 Map<Long, Long> listingCounts,
                                                                 Map<Long, Long> leadCounts) {
        return allAccounts.stream().filter(account -> account.getRoles().contains(role))
        .map(account -> new AdminDashboardData.AccountStats(account.getId(), account.getName(),
                account.getEmail(), account.getPhone(), account.getCreatedAt(),
                account.getRoles().stream().map(Enum::name).collect(Collectors.toSet()),
                listingCounts.getOrDefault(account.getId(), 0L),
                leadCounts.getOrDefault(account.getId(), 0L)))
        .sorted(Comparator.comparing(AdminDashboardData.AccountStats::createdAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<AdminDashboardData.PriceStats> priceStats(List<PropertyListing> filtered) {
        return filtered.stream().collect(Collectors.groupingBy(item ->
                        hasText(item.getPriceUnit()) ? item.getPriceUnit() : "VND"))
                .entrySet().stream().map(entry -> {
                    List<BigDecimal> values = entry.getValue().stream().map(PropertyListing::getPrice).toList();
                    return new AdminDashboardData.PriceStats(entry.getKey(), average(values),
                            values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                            values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                }).sorted(Comparator.comparing(AdminDashboardData.PriceStats::unit)).toList();
    }

    private List<AdminDashboardData.GroupStats> groupStats(List<PropertyListing> filtered, String groupBy) {
        Map<String, List<PropertyListing>> groups = filtered.stream()
                .collect(Collectors.groupingBy(item -> groupLabel(item, groupBy), Collectors.toList()));
        return groups.entrySet().stream().map(entry -> new AdminDashboardData.GroupStats(entry.getKey(),
                        entry.getValue().size(), average(entry.getValue().stream()
                        .map(item -> BigDecimal.valueOf(item.getArea())).toList())))
                .sorted(Comparator.comparingLong(AdminDashboardData.GroupStats::count).reversed()
                        .thenComparing(AdminDashboardData.GroupStats::label))
                .toList();
    }

    private String groupLabel(PropertyListing listing, String groupBy) {
        return switch (groupBy) {
            case "propertyType" -> label(listing.getPropertyType());
            case "status" -> listing.getStatus().name();
            case "direction" -> label(listing.getDirection());
            case "location" -> locationLabel(listing.getAddress());
            case "month" -> listing.getCreatedAt() == null ? "Chưa xác định"
                    : listing.getCreatedAt().toString().substring(0, 7);
            case "areaRange" -> areaRange(listing.getArea());
            case "priceRange" -> priceRange(listing.getPrice(),
                    hasText(listing.getPriceUnit()) ? listing.getPriceUnit() : "VND");
            default -> label(listing.getTransactionType());
        };
    }

    private static String locationLabel(String address) {
        if (address == null || address.isBlank()) return "Chưa cập nhật";
        String[] parts = address.split(",");
        return parts[parts.length - 1].trim();
    }

    private static String areaRange(Double area) {
        if (area < 50) return "Dưới 50 m²";
        if (area < 100) return "50–99 m²";
        if (area < 200) return "100–199 m²";
        return "Từ 200 m²";
    }

    private static String priceRange(BigDecimal price, String unit) {
        BigDecimal first = "VND".equalsIgnoreCase(unit) ? BigDecimal.valueOf(1_000_000_000L) : BigDecimal.valueOf(1_000);
        BigDecimal second = "VND".equalsIgnoreCase(unit) ? BigDecimal.valueOf(3_000_000_000L) : BigDecimal.valueOf(3_000);
        BigDecimal third = "VND".equalsIgnoreCase(unit) ? BigDecimal.valueOf(5_000_000_000L) : BigDecimal.valueOf(5_000);
        if (price.compareTo(first) < 0) return "Dưới " + first + " " + unit;
        if (price.compareTo(second) < 0) return first + "–" + second + " " + unit;
        if (price.compareTo(third) < 0) return second + "–" + third + " " + unit;
        return "Từ " + third + " " + unit;
    }

    private static String label(String value) {
        return value == null || value.isBlank() ? "Chưa cập nhật" : value;
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 0, RoundingMode.HALF_UP);
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static <T extends Comparable<? super T>> void validateRange(T min, T max, String field) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(field + " tối thiểu phải nhỏ hơn hoặc bằng tối đa.");
        }
    }
}
