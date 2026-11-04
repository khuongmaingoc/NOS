package vn.sonbds.marketplace.realestate;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "property_listings")
public class PropertyListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ownerId;
    @NotBlank @Size(max = 180)
    private String title;
    @NotBlank @Size(max = 300)
    private String address;
    private Double latitude;
    private Double longitude;
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
    private String propertyFeature;
    private String direction;
    private String position;
    private String interior;
    private String legalStatus;
    private String priceUnit = "VND";
    private String propertyStatus;
    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PUBLIC;
    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    private List<String> imageUrls = new ArrayList<>();

    protected PropertyListing() {
    }

    public PropertyListing(Long ownerId, String title, String address, String description, BigDecimal price,
                           Double area, String transactionType, String propertyType, Visibility visibility,
                           List<String> imageUrls, String propertyFeature, String direction, String position,
                           String interior, String legalStatus, String priceUnit, String propertyStatus) {
        this(ownerId, title, address, description, price, area, transactionType, propertyType, visibility,
                imageUrls, propertyFeature, direction, position, interior, legalStatus, priceUnit, propertyStatus,
                null, null);
    }

    public PropertyListing(Long ownerId, String title, String address, String description, BigDecimal price,
                           Double area, String transactionType, String propertyType, Visibility visibility,
                           List<String> imageUrls, String propertyFeature, String direction, String position,
                           String interior, String legalStatus, String priceUnit, String propertyStatus,
                           Double latitude, Double longitude) {
        this.ownerId = ownerId;
        update(title, address, description, price, area, transactionType, propertyType, visibility, imageUrls,
                propertyFeature, direction, position, interior, legalStatus, priceUnit, propertyStatus,
                latitude, longitude);
    }

    public void update(String title, String address, String description, BigDecimal price, Double area,
                       String transactionType, String propertyType, Visibility visibility, List<String> imageUrls,
                       String propertyFeature, String direction, String position, String interior,
                       String legalStatus, String priceUnit, String propertyStatus) {
        update(title, address, description, price, area, transactionType, propertyType, visibility, imageUrls,
                propertyFeature, direction, position, interior, legalStatus, priceUnit, propertyStatus, null, null);
    }

    public void update(String title, String address, String description, BigDecimal price, Double area,
                       String transactionType, String propertyType, Visibility visibility, List<String> imageUrls,
                       String propertyFeature, String direction, String position, String interior,
                       String legalStatus, String priceUnit, String propertyStatus, Double latitude, Double longitude) {
        MapCoordinates.validate(latitude, longitude);
        this.title = title;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
        this.price = roundPrice(price);
        this.area = area;
        this.transactionType = transactionType;
        this.propertyType = propertyType;
        this.visibility = visibility;
        this.propertyFeature = propertyFeature;
        this.direction = direction;
        this.position = position;
        this.interior = interior;
        this.legalStatus = legalStatus;
        this.priceUnit = priceUnit == null || priceUnit.isBlank() ? "VND" : priceUnit.trim();
        this.propertyStatus = propertyStatus;
        this.imageUrls.clear();
        if (imageUrls != null) this.imageUrls.addAll(imageUrls);
        this.updatedAt = Instant.now();
    }

    static BigDecimal roundPrice(BigDecimal price) {
        BigDecimal rounded = price.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1000));
        if (rounded.signum() <= 0) {
            throw new IllegalArgumentException("Giá sau khi làm tròn phải lớn hơn 0.");
        }
        return rounded;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Double getArea() { return area; }
    public String getTransactionType() { return transactionType; }
    public String getPropertyType() { return propertyType; }
    public String getPropertyFeature() { return propertyFeature; }
    public String getDirection() { return direction; }
    public String getPosition() { return position; }
    public String getInterior() { return interior; }
    public String getLegalStatus() { return legalStatus; }
    public String getPriceUnit() { return priceUnit; }
    public String getPropertyStatus() { return propertyStatus; }
    public Visibility getVisibility() { return visibility; }
    public ListingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<String> getImageUrls() { return List.copyOf(imageUrls); }
    public void setStatus(ListingStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}
