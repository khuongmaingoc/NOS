package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "favorites", uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "listing_id"}))
public class Favorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long accountId;
    private Long listingId;
    private Instant createdAt = Instant.now();

    protected Favorite() {
    }

    public Favorite(Long accountId, Long listingId) {
        this.accountId = accountId;
        this.listingId = listingId;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public Long getListingId() { return listingId; }
    public Instant getCreatedAt() { return createdAt; }
}
