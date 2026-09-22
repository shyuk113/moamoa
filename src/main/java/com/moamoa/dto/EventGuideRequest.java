package com.moamoa.dto;

import jakarta.validation.constraints.*;

public record EventGuideRequest(
    @Size(max = 1000) String title,
    @Size(max = 12000) String description,
    @Size(max = 1000) String place,
    @Size(max = 1000) String fee,
    @Size(max = 1000) String eligibility,
    @Size(max = 3000) String transit,
    @Size(max = 3000) String parking,
    @Size(max = 3000) String booking,
    @Size(max = 3000) String accessibility,
    @Size(max = 255) String contactPhone,
    @Email @Size(max = 254) String contactEmail,
    @Size(max = 1000) String address,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    @Size(max = 20000) String gallery,
    @Pattern(regexp = "^$|[A-Za-z0-9_-]{11}") String videoId,
    @Size(max = 1000) String attribution,
    @NotBlank @Size(max = 2000) String sourceUrl) {}
