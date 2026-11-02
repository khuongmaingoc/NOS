package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotNull;

public record ListingStatusRequest(@NotNull ListingStatus status) {
}
