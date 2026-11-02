package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotNull;

public record InquiryStatusRequest(@NotNull InquiryStatus status) {
}
