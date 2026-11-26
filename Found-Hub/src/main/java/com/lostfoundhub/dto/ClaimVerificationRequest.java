package com.lostfoundhub.dto;

import com.lostfoundhub.enums.ClaimStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin decision: status must be APPROVED or REJECTED. Remarks are required when rejecting. */
public record ClaimVerificationRequest(
        @NotNull(message = "Status is required (APPROVED or REJECTED)")
        ClaimStatus status,

        @Size(max = 1000, message = "Admin remarks must be at most 1000 characters")
        String adminRemarks
) {
}
