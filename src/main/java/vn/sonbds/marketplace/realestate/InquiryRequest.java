package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public record InquiryRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 120) String contactName,
        @Size(max = 30) String contactPhone,
        @Email @Size(max = 180) String contactEmail,
        @Size(max = 80) String source,
        @Size(max = 80) String subject,
        @Size(max = 80) String need,
        @Size(max = 40) String gender,
        @Size(max = 80) String contactChannel,
        @Size(max = 80) String interestLevel,
        @Size(max = 80) String contactStatus,
        @Size(max = 80) String dealStatus
) {
}
