package vn.sonbds.marketplace.realestate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LeadRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 30) String phone,
        @Email @Size(max = 180) String email,
        @Positive Long listingId,
        @Size(max = 80) String source,
        @Size(max = 80) String subject,
        @Size(max = 80) String need,
        @Size(max = 40) String gender,
        @Size(max = 80) String contactChannel,
        @Size(max = 80) String interestLevel,
        @Size(max = 80) String contactStatus,
        @Size(max = 80) String propertyFeature,
        @Size(max = 80) String direction,
        @Size(max = 80) String position,
        @Size(max = 100) String interior,
        @Size(max = 100) String legalStatus,
        @Size(max = 20) String priceUnit,
        @Size(max = 80) String propertyStatus,
        @Size(max = 80) String dealStatus,
        @Size(max = 2000) String notes,
        @Size(max = 300) String propertyAddress,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
) {
    public LeadRequest(String name, String phone, String email, Long listingId, String source, String subject,
                       String need, String gender, String contactChannel, String interestLevel, String contactStatus,
                       String propertyFeature, String direction, String position, String interior, String legalStatus,
                       String priceUnit, String propertyStatus, String dealStatus, String notes) {
        this(name, phone, email, listingId, source, subject, need, gender, contactChannel, interestLevel,
                contactStatus, propertyFeature, direction, position, interior, legalStatus, priceUnit,
                propertyStatus, dealStatus, notes, null, null, null);
    }

    @AssertTrue(message = "Vĩ độ và kinh độ phải được nhập cùng nhau.")
    public boolean isLocationCoordinatesComplete() {
        return MapCoordinates.areComplete(latitude, longitude);
    }
}
