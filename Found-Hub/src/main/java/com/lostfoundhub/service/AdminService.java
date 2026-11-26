package com.lostfoundhub.service;

import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.dto.ClaimVerificationRequest;
import com.lostfoundhub.dto.DashboardStatsResponse;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.Claim;
import com.lostfoundhub.model.Item;
import com.lostfoundhub.repository.ClaimRepository;
import com.lostfoundhub.repository.ItemRepository;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.util.PageUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Admin-only operations. The controller layer guarantees only ADMIN users reach this class. */
@Service
public class AdminService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AdminService(ClaimRepository claimRepository,
                        ItemRepository itemRepository,
                        UserRepository userRepository,
                        NotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> getClaims(ClaimStatus status, int page, int size) {
        Pageable pageable = PageUtil.newestFirst(page, size);
        var result = status == null ? claimRepository.findAll(pageable) : claimRepository.findByStatus(status, pageable);
        return PageResponse.from(result.map(ClaimResponse::from));
    }

    /**
     * Approve or reject one claim in a single transaction:
     *  - claim row and item row are locked so concurrent decisions cannot conflict;
     *  - APPROVED: item becomes CLAIMED and every other PENDING claim for it is REJECTED;
     *  - REJECTED: only this claim changes (remarks are required).
     */
    @Transactional
    public ClaimResponse verifyClaim(Long claimId, ClaimVerificationRequest request) {
        ClaimStatus decision = request.status();
        if (decision == ClaimStatus.PENDING) {
            throw new BadRequestException("Status must be APPROVED or REJECTED");
        }
        String remarks = request.adminRemarks() == null ? null : request.adminRemarks().trim();
        if (decision == ClaimStatus.REJECTED && (remarks == null || remarks.isEmpty())) {
            throw new BadRequestException("Admin remarks are required when rejecting a claim");
        }

        // Lock order is always: item first, then claim. Item is locked before it is loaded anywhere else,
        // so we always see the latest committed status.
        Long itemId = claimRepository.findItemIdByClaimId(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id " + claimId));
        Item item = itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found for this claim"));
        Claim claim = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id " + claimId));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new BadRequestException("This claim was already " + claim.getStatus().name().toLowerCase());
        }

        if (decision == ClaimStatus.APPROVED && item.getStatus() != ItemStatus.OPEN) {
            throw new BadRequestException("Item is not available for approval (status: " + item.getStatus() + ")");
        }

        claim.setStatus(decision);
        claim.setAdminRemarks(remarks);
        claim.setReviewedAt(LocalDateTime.now());

        if (decision == ClaimStatus.APPROVED) {
            item.setStatus(ItemStatus.CLAIMED);
            rejectOtherPendingClaims(item, claim.getId(),
                    "Another claim for this item was approved.");

            notificationService.notifyUser(claim.getUser(),
                    "Your claim for '" + item.getItemName() + "' was APPROVED." + remarksSuffix(remarks));
            notificationService.notifyUser(item.getUser(),
                    "A claim for your item '" + item.getItemName() + "' was approved by the admin.");
        } else {
            notificationService.notifyUser(claim.getUser(),
                    "Your claim for '" + item.getItemName() + "' was REJECTED." + remarksSuffix(remarks));
        }
        return ClaimResponse.from(claim);
    }

    /** Marks the item as RETURNED; any claims still pending are closed (rejected). */
    @Transactional
    public ItemResponse markItemReturned(Long itemId) {
        Item item = lockItem(itemId);
        if (item.getStatus() == ItemStatus.RETURNED) {
            throw new BadRequestException("Item is already marked as RETURNED");
        }
        item.setStatus(ItemStatus.RETURNED);
        rejectOtherPendingClaims(item, null, "The item has been returned to its owner.");

        notificationService.notifyUser(item.getUser(),
                "Your item '" + item.getItemName() + "' has been marked as RETURNED.");
        claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.APPROVED).forEach(approved ->
                notificationService.notifyUser(approved.getUser(),
                        "The item '" + item.getItemName() + "' has been marked as RETURNED to you."));
        return ItemResponse.from(item);
    }

    /** Manual status management. A RETURNED item can never be re-opened. */
    @Transactional
    public ItemResponse updateItemStatus(Long itemId, ItemStatus newStatus) {
        if (newStatus == ItemStatus.RETURNED) {
            return markItemReturned(itemId);
        }
        Item item = lockItem(itemId);
        if (item.getStatus() == ItemStatus.RETURNED) {
            throw new BadRequestException("A RETURNED item cannot change status again");
        }
        item.setStatus(newStatus);
        return ItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        return new DashboardStatsResponse(
                userRepository.count(),
                itemRepository.count(),
                itemRepository.countByItemType(ItemType.LOST),
                itemRepository.countByItemType(ItemType.FOUND),
                itemRepository.countByStatus(ItemStatus.OPEN),
                itemRepository.countByStatus(ItemStatus.CLAIMED),
                itemRepository.countByStatus(ItemStatus.RETURNED),
                claimRepository.count(),
                claimRepository.countByStatus(ClaimStatus.PENDING),
                claimRepository.countByStatus(ClaimStatus.APPROVED),
                claimRepository.countByStatus(ClaimStatus.REJECTED));
    }

    // ---------------- helpers ----------------

    private Item lockItem(Long itemId) {
        return itemRepository.findByIdForUpdate(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + itemId));
    }

    private void rejectOtherPendingClaims(Item item, Long exceptClaimId, String reason) {
        for (Claim other : claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING)) {
            if (other.getId() != null && other.getId().equals(exceptClaimId)) {
                continue;
            }
            other.setStatus(ClaimStatus.REJECTED);
            other.setAdminRemarks(reason);
            other.setReviewedAt(LocalDateTime.now());
            notificationService.notifyUser(other.getUser(),
                    "Your claim for '" + item.getItemName() + "' was REJECTED. " + reason);
        }
    }

    private String remarksSuffix(String remarks) {
        return remarks == null || remarks.isEmpty() ? "" : " Admin remarks: " + remarks;
    }
}
