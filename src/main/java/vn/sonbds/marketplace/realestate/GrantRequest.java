package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GrantRequest(
        @NotNull ResourceType resourceType,
        @NotNull @Positive Long resourceId,
        @NotNull @Positive Long accountId,
        @NotNull ResourcePermission permission
) {
}
