package com.lostfoundhub.dto;

public record DashboardStatsResponse(
        long totalUsers,
        long totalItems,
        long lostItems,
        long foundItems,
        long openItems,
        long claimedItems,
        long returnedItems,
        long totalClaims,
        long pendingClaims,
        long approvedClaims,
        long rejectedClaims
) {
}
