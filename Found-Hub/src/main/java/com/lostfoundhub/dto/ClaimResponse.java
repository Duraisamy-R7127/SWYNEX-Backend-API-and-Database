package com.lostfoundhub.dto;

import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.model.Claim;

import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        Long itemId,
        String itemName,
        ItemStatus itemStatus,
        Long claimantId,
        String claimantName,
        String claimantEmail,
        String claimantPhone,
        String proofDescription,
        String additionalDetails,
        ClaimStatus status,
        String adminRemarks,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {

    /** Must be called inside a service transaction (item and user are lazy). */
    public static ClaimResponse from(Claim claim) {
        return new ClaimResponse(claim.getId(), claim.getItem().getId(), claim.getItem().getItemName(),
                claim.getItem().getStatus(), claim.getUser().getId(), claim.getUser().getName(),
                claim.getUser().getEmail(), claim.getUser().getPhone(),
                claim.getProofDescription(), claim.getAdditionalDetails(),
                claim.getStatus(), claim.getAdminRemarks(),
                claim.getCreatedAt(), claim.getReviewedAt());
    }
}
