package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(name = "inquiries")
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long listingId;
    private Long customerId;
    private Long sellerId;
    @Size(max = 120)
    private String contactName;
    @Size(max = 30)
    private String contactPhone;
    @Email @Size(max = 180)
    private String contactEmail;
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
    private String dealStatus;
    @NotBlank @Size(max = 2000)
    private String message;
    @Enumerated(EnumType.STRING)
    private InquiryStatus status = InquiryStatus.NEW;
    private Instant createdAt = Instant.now();

    protected Inquiry() {
    }

    public Inquiry(Long listingId, Long customerId, Long sellerId, InquiryRequest request) {
        this.listingId = listingId;
        this.customerId = customerId;
        this.sellerId = sellerId;
        this.message = request.message().trim();
        this.contactName = clean(request.contactName());
        this.contactPhone = clean(request.contactPhone());
        this.contactEmail = clean(request.contactEmail());
        this.source = clean(request.source());
        this.subject = clean(request.subject());
        this.need = clean(request.need());
        this.gender = clean(request.gender());
        this.contactChannel = clean(request.contactChannel());
        this.interestLevel = clean(request.interestLevel());
        this.contactStatus = clean(request.contactStatus());
        this.dealStatus = clean(request.dealStatus());
    }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public Long getCustomerId() { return customerId; }
    public Long getSellerId() { return sellerId; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public String getSource() { return source; }
    public String getSubject() { return subject; }
    public String getNeed() { return need; }
    public String getGender() { return gender; }
    public String getContactChannel() { return contactChannel; }
    public String getInterestLevel() { return interestLevel; }
    public String getContactStatus() { return contactStatus; }
    public String getDealStatus() { return dealStatus; }
    public String getMessage() { return message; }
    public InquiryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(InquiryStatus status) { this.status = status; }
}
