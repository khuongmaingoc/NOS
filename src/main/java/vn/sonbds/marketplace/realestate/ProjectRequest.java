package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 300) String address,
        @NotBlank String description,
        @PositiveOrZero Integer plotCount,
        Visibility visibility
) {
}
