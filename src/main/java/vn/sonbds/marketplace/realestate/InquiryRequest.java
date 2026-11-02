package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryRequest(@NotBlank @Size(max = 2000) String message) {
}
