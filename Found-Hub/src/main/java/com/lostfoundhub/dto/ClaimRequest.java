package com.lostfoundhub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClaimRequest(
        @NotNull(message = "Item id is required")
        Long itemId,

        @NotBlank(message = "Ownership proof description is required")
        @Size(min = 10, max = 2000, message = "Proof description must be 10-2000 characters")
        String proofDescription,

        @Size(max = 1000, message = "Additional identifying details must be at most 1000 characters")
        String additionalDetails
) {
    public ClaimRequest(Long itemId, String proofDescription) {
        this(itemId, proofDescription, null);
    }
}
