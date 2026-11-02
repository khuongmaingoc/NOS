package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ListingRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 300) String address,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Positive Double area,
        @NotBlank String transactionType,
        @NotBlank String propertyType,
        Visibility visibility,
        List<@NotBlank String> imageUrls
) {
}
