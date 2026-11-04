package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "customer_leads")
public class CustomerLead {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long ownerId;
    private Long listingId;
    @NotBlank @Size(max = 120)
    private String name;
    @Size(max = 30)
    private String phone;
    @Email @Size(max = 180)
    private String email;
    @Size(max = 80)
    private String source;
    @Size(max = 80)
    private String subject;
    @Size(max = 80)
    private String need;
    @Size(max = 40)
    private String gender;
    @Size(max = 80)
    private String contactChannel;
    @Size(max = 80)
    private String interestLevel;
    @Size(max = 80)
    private String contactStatus;
    @Size(max = 80)
    private String propertyFeature;
    @Size(max = 80)
    private String direction;
    @Size(max = 80)
    private String position;
    @Size(max = 100)
    private String interior;
    @Size(max = 100)
    private String legalStatus;
    @Size(max = 20)
    private String priceUnit;
    @Size(max = 80)
    private String propertyStatus;
    @Size(max = 300)
    private String propertyAddress;
    private Double latitude;
    private Double longitude;
    @Size(max = 80)
    private String dealStatus;
    @Size(max = 2000)
    private String notes;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    protected CustomerLead() {
    }

    public CustomerLead(Long ownerId, Long listingId, LeadRequest request) {
        this.ownerId = ownerId;
        update(listingId, request);
    }

    public void update(Long listingId, LeadRequest request) {
        MapCoordinates.validate(request.latitude(), request.longitude());
        this.listingId = listingId;
        this.name = clean(request.name());
        this.phone = clean(request.phone());
        this.email = clean(request.email());
        this.source = clean(request.source());
        this.subject = clean(request.subject());
        this.need = clean(request.need());
        this.gender = clean(request.gender());
        this.contactChannel = clean(request.contactChannel());
        this.interestLevel = clean(request.interestLevel());
        this.contactStatus = clean(request.contactStatus());
        this.propertyFeature = clean(request.propertyFeature());
        this.direction = clean(request.direction());
        this.position = clean(request.position());
        this.interior = clean(request.interior());
        this.legalStatus = clean(request.legalStatus());
        this.priceUnit = clean(request.priceUnit());
        this.propertyStatus = clean(request.propertyStatus());
        this.propertyAddress = clean(request.propertyAddress());
        this.latitude = request.latitude();
        this.longitude = request.longitude();
        this.dealStatus = clean(request.dealStatus());
        this.notes = clean(request.notes());
        this.updatedAt = Instant.now();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public Long getListingId() { return listingId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getSource() { return source; }
    public String getSubject() { return subject; }
    public String getNeed() { return need; }
    public String getGender() { return gender; }
    public String getContactChannel() { return contactChannel; }
    public String getInterestLevel() { return interestLevel; }
    public String getContactStatus() { return contactStatus; }
    public String getPropertyFeature() { return propertyFeature; }
    public String getDirection() { return direction; }
    public String getPosition() { return position; }
    public String getInterior() { return interior; }
    public String getLegalStatus() { return legalStatus; }
    public String getPriceUnit() { return priceUnit; }
    public String getPropertyStatus() { return propertyStatus; }
    public String getPropertyAddress() { return propertyAddress; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getDealStatus() { return dealStatus; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
