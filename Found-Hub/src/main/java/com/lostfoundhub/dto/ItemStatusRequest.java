package com.lostfoundhub.dto;

import com.lostfoundhub.enums.ItemStatus;
import jakarta.validation.constraints.NotNull;

public record ItemStatusRequest(
        @NotNull(message = "Status is required (OPEN, CLAIMED or RETURNED)")
        ItemStatus status
) {
}
