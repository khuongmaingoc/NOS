package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.AssertTrue;

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
        List<@NotBlank String> imageUrls,
        @Size(max = 80) String propertyFeature,
        @Size(max = 80) String direction,
        @Size(max = 80) String position,
        @Size(max = 100) String interior,
        @Size(max = 100) String legalStatus,
        @Size(max = 20) String priceUnit,
        @Size(max = 80) String propertyStatus,
        @jakarta.validation.constraints.DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @jakarta.validation.constraints.DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
    @AssertTrue(message = "Vĩ độ và kinh độ phải được nhập cùng nhau.")
    public boolean isLocationCoordinatesComplete() {
        return MapCoordinates.areComplete(latitude, longitude);
    }
}
