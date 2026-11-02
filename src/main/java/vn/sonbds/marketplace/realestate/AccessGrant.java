package vn.sonbds.marketplace.realestate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "access_grants", uniqueConstraints =
        @UniqueConstraint(columnNames = {"resource_type", "resource_id", "account_id", "permission"}))
public class AccessGrant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;
    private Long resourceId;
    private Long accountId;
    @Enumerated(EnumType.STRING)
    private ResourcePermission permission;

    protected AccessGrant() {
    }

    public AccessGrant(ResourceType resourceType, Long resourceId, Long accountId, ResourcePermission permission) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.accountId = accountId;
        this.permission = permission;
    }

    public Long getId() { return id; }
    public ResourceType getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public Long getAccountId() { return accountId; }
    public ResourcePermission getPermission() { return permission; }
}
