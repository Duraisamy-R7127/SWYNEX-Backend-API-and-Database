package com.lostfoundhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
        String name,

        @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Phone must be 7-15 digits (may include + - and spaces)")
        String phone
) {
}
