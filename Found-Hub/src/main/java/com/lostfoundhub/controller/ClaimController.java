package com.lostfoundhub.controller;

import com.lostfoundhub.dto.ApiResponse;
import com.lostfoundhub.dto.ClaimRequest;
import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClaimResponse>> submitClaim(@AuthenticationPrincipal CustomUserDetails principal,
                                                                  @Valid @RequestBody ClaimRequest request) {
        ClaimResponse claim = claimService.submitClaim(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Claim submitted successfully", claim));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getMyClaims(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok("Your claims fetched", claimService.getMyClaims(principal.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaim(@AuthenticationPrincipal CustomUserDetails principal,
                                                               @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Claim fetched",
                claimService.getClaim(id, principal.getId(), principal.isAdmin())));
    }

    /** Lightweight status tracking for the claim screen. */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getClaimStatus(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        ClaimResponse claim = claimService.getClaim(id, principal.getId(), principal.isAdmin());
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("claimId", claim.id());
        status.put("status", claim.status());
        status.put("itemStatus", claim.itemStatus());
        status.put("adminRemarks", claim.adminRemarks());
        status.put("reviewedAt", claim.reviewedAt());
        return ResponseEntity.ok(ApiResponse.ok("Claim status fetched", status));
    }
}
