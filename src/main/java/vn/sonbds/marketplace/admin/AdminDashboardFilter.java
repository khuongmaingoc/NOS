package vn.sonbds.marketplace.admin;

import vn.sonbds.marketplace.realestate.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdminDashboardFilter {
    private LocalDate from;
    private LocalDate to;
    private String location;
    private String transactionType;
    private String propertyType;
    private ListingStatus status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minArea;
    private Double maxArea;
    private String priceUnit = "ALL";
    private String groupBy = "transactionType";

    public LocalDate getFrom() { return from; }
    public void setFrom(LocalDate from) { this.from = from; }
    public LocalDate getTo() { return to; }
    public void setTo(LocalDate to) { this.to = to; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public Double getMinArea() { return minArea; }
    public void setMinArea(Double minArea) { this.minArea = minArea; }
    public Double getMaxArea() { return maxArea; }
    public void setMaxArea(Double maxArea) { this.maxArea = maxArea; }
    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = priceUnit; }
    public String getGroupBy() { return groupBy; }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
}
