package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    @NotBlank @Size(max = 2000)
    private String message;
    @Enumerated(EnumType.STRING)
    private InquiryStatus status = InquiryStatus.NEW;
    private Instant createdAt = Instant.now();

    protected Inquiry() {
    }

    public Inquiry(Long listingId, Long customerId, Long sellerId, String message) {
        this.listingId = listingId;
        this.customerId = customerId;
        this.sellerId = sellerId;
        this.message = message;
    }

    public Long getId() { return id; }
    public Long getListingId() { return listingId; }
    public Long getCustomerId() { return customerId; }
    public Long getSellerId() { return sellerId; }
    public String getMessage() { return message; }
    public InquiryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(InquiryStatus status) { this.status = status; }
}
