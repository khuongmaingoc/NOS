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
                           List<String> imageUrls) {
        this.ownerId = ownerId;
        update(title, address, description, price, area, transactionType, propertyType, visibility, imageUrls);
    }

    public void update(String title, String address, String description, BigDecimal price, Double area,
                       String transactionType, String propertyType, Visibility visibility, List<String> imageUrls) {
        this.title = title;
        this.address = address;
        this.description = description;
        this.price = price;
        this.area = area;
        this.transactionType = transactionType;
        this.propertyType = propertyType;
        this.visibility = visibility;
        this.imageUrls.clear();
        if (imageUrls != null) this.imageUrls.addAll(imageUrls);
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public String getAddress() { return address; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Double getArea() { return area; }
    public String getTransactionType() { return transactionType; }
    public String getPropertyType() { return propertyType; }
    public Visibility getVisibility() { return visibility; }
    public ListingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<String> getImageUrls() { return List.copyOf(imageUrls); }
    public void setStatus(ListingStatus status) { this.status = status; this.updatedAt = Instant.now(); }
}
