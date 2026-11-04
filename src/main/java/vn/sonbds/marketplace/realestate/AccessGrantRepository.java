package vn.sonbds.marketplace.realestate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {
    boolean existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
            ResourceType resourceType, Long resourceId, Long accountId, ResourcePermission permission);
    List<AccessGrant> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
    List<AccessGrant> findByResourceTypeAndPermissionOrderByIdDesc(
            ResourceType resourceType, ResourcePermission permission);
}
