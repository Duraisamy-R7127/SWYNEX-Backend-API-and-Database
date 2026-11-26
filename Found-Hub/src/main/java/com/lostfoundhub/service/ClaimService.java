package com.lostfoundhub.service;

import com.lostfoundhub.dto.ClaimRequest;
import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.exception.DuplicateResourceException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.Claim;
import com.lostfoundhub.model.Item;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.ClaimRepository;
import com.lostfoundhub.repository.ItemRepository;
import com.lostfoundhub.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** User-side claim operations. Approving/rejecting is done by AdminService. */
@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ClaimService(ClaimRepository claimRepository,
                        ItemRepository itemRepository,
                        UserRepository userRepository,
                        NotificationService notificationService) {
        this.claimRepository = claimRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * The item row is locked first, so two simultaneous requests cannot both pass the
     * "no duplicate pending claim" check or claim an item that is being approved at that moment.
     */
    @Transactional
    public ClaimResponse submitClaim(ClaimRequest request, Long userId) {
        User claimant = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
        Item item = itemRepository.findByIdForUpdate(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + request.itemId()));

        if (item.getUser().getId().equals(userId)) {
            throw new BadRequestException("You cannot claim your own item");
        }
        // Only officially registered FOUND items (posted by admin) can be claimed by students
        if (item.getItemType() != com.lostfoundhub.enums.ItemType.FOUND) {
            throw new BadRequestException("Claims can only be submitted for officially registered FOUND items");
        }
        if (item.getStatus() == ItemStatus.RETURNED) {
            throw new BadRequestException("This item has already been returned and cannot be claimed");
        }
        if (item.getStatus() != ItemStatus.OPEN) {
            throw new BadRequestException("This item has already been claimed and is awaiting return");
        }
        if (claimRepository.existsByItemIdAndUserIdAndStatus(item.getId(), userId, ClaimStatus.PENDING)) {
            throw new DuplicateResourceException("You already have a pending claim for this item");
        }

        Claim claim = new Claim();
        claim.setItem(item);
        claim.setUser(claimant);
        claim.setProofDescription(request.proofDescription().trim());
        claim.setAdditionalDetails(request.additionalDetails() != null && !request.additionalDetails().isBlank()
                ? request.additionalDetails().trim() : null);
        claim.setStatus(ClaimStatus.PENDING);
        Claim saved = claimRepository.save(claim);

        notificationService.notifyUser(claimant,
                "Your claim for '" + item.getItemName() + "' was submitted and is waiting for admin verification.");
        notificationService.notifyUser(item.getUser(),
                "A claim was submitted for your item '" + item.getItemName() + "'.");
        notificationService.notifyAdmins(
                "New claim from " + claimant.getName() + " for item '" + item.getItemName() + "' needs verification.");

        return ClaimResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getMyClaims(Long userId) {
        return claimRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ClaimResponse::from)
                .toList();
    }

    /** Visible to the claimant, the owner of the claimed item, and admins. */
    @Transactional(readOnly = true)
    public ClaimResponse getClaim(Long claimId, Long currentUserId, boolean isAdmin) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim not found with id " + claimId));
        boolean isClaimant = claim.getUser().getId().equals(currentUserId);
        boolean isItemOwner = claim.getItem().getUser().getId().equals(currentUserId);
        if (!isAdmin && !isClaimant && !isItemOwner) {
            throw new AccessDeniedException("You are not allowed to view this claim");
        }
        return ClaimResponse.from(claim);
    }
}
