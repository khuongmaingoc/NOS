package vn.sonbds.marketplace.realestate;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ResourceAccessService {
    private final AccessGrantRepository grants;

    public ResourceAccessService(AccessGrantRepository grants) {
        this.grants = grants;
    }

    public boolean canView(PropertyListing listing, Long accountId, boolean admin) {
        return admin || listing.getVisibility() == Visibility.PUBLIC
                || listing.getOwnerId().equals(accountId)
                || hasGrant(ResourceType.LISTING, listing.getId(), accountId, ResourcePermission.VIEW)
                || hasGrant(ResourceType.LISTING, listing.getId(), accountId, ResourcePermission.EDIT)
                || hasGrant(ResourceType.LISTING, listing.getId(), accountId, ResourcePermission.DELETE);
    }

    public boolean canView(LandProject project, Long accountId, boolean admin) {
        return admin || project.getVisibility() == Visibility.PUBLIC
                || project.getOwnerId().equals(accountId)
                || hasGrant(ResourceType.PROJECT, project.getId(), accountId, ResourcePermission.VIEW)
                || hasGrant(ResourceType.PROJECT, project.getId(), accountId, ResourcePermission.EDIT)
                || hasGrant(ResourceType.PROJECT, project.getId(), accountId, ResourcePermission.DELETE);
    }

    public void requireListing(PropertyListing listing, Long accountId, boolean admin, ResourcePermission permission) {
        if (!admin && !listing.getOwnerId().equals(accountId)
                && !hasGrant(ResourceType.LISTING, listing.getId(), accountId, permission)) {
            throw new AccessDeniedException("Bạn không có quyền " + permission + " trên tin này");
        }
    }

    public void requireProject(LandProject project, Long accountId, boolean admin, ResourcePermission permission) {
        if (!admin && !project.getOwnerId().equals(accountId)
                && !hasGrant(ResourceType.PROJECT, project.getId(), accountId, permission)) {
            throw new AccessDeniedException("Bạn không có quyền " + permission + " trên dự án này");
        }
    }

    private boolean hasGrant(ResourceType type, Long resourceId, Long accountId, ResourcePermission permission) {
        return accountId != null && grants.existsByResourceTypeAndResourceIdAndAccountIdAndPermission(
                type, resourceId, accountId, permission);
    }
}
