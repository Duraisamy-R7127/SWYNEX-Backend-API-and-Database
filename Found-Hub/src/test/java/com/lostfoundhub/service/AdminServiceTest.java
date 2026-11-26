package com.lostfoundhub.service;

import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.dto.ClaimVerificationRequest;
import com.lostfoundhub.dto.DashboardStatsResponse;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.model.Claim;
import com.lostfoundhub.model.Item;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.ClaimRepository;
import com.lostfoundhub.repository.ItemRepository;
import com.lostfoundhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private AdminService adminService;
    private Item item;
    private Claim claimA;
    private Claim claimB;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(claimRepository, itemRepository, userRepository, notificationService);
        User owner = user(1L, "Owner");
        item = new Item();
        item.setId(10L);
        item.setItemName("Blue Wallet");
        item.setUser(owner);
        item.setStatus(ItemStatus.OPEN);
        claimA = claim(1L, user(2L, "A"));
        claimB = claim(2L, user(3L, "B"));
    }

    private User user(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private Claim claim(Long id, User claimant) {
        Claim c = new Claim();
        c.setId(id);
        c.setItem(item);
        c.setUser(claimant);
        c.setProofDescription("proof of ownership");
        c.setStatus(ClaimStatus.PENDING);
        return c;
    }

    @Test
    void approve_marksItemClaimedAndRejectsOtherPendingClaims() {
        when(claimRepository.findItemIdByClaimId(1L)).thenReturn(Optional.of(10L));
        when(claimRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(claimA));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(claimRepository.findByItemIdAndStatus(10L, ClaimStatus.PENDING)).thenReturn(List.of(claimA, claimB));

        ClaimResponse response = adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.APPROVED, "ID card matches"));

        assertEquals(ClaimStatus.APPROVED, response.status());
        assertEquals(ClaimStatus.APPROVED, claimA.getStatus());
        assertEquals("ID card matches", claimA.getAdminRemarks());
        assertNotNull(claimA.getReviewedAt());
        assertEquals(ItemStatus.CLAIMED, item.getStatus());
        assertEquals(ClaimStatus.REJECTED, claimB.getStatus());
        // approved claimant + item owner + the auto-rejected claimant
        verify(notificationService, times(3)).notifyUser(org.mockito.ArgumentMatchers.any(User.class), anyString());
    }

    @Test
    void reject_withRemarks_keepsItemOpenAndOtherClaimsUntouched() {
        when(claimRepository.findItemIdByClaimId(1L)).thenReturn(Optional.of(10L));
        when(claimRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(claimA));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        ClaimResponse response = adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.REJECTED, "Proof does not match"));

        assertEquals(ClaimStatus.REJECTED, response.status());
        assertEquals(ItemStatus.OPEN, item.getStatus());
        assertEquals(ClaimStatus.PENDING, claimB.getStatus());
        verify(notificationService).notifyUser(org.mockito.ArgumentMatchers.any(User.class), anyString());
    }

    @Test
    void reject_withoutRemarks_isBadRequest() {
        assertThrows(BadRequestException.class,
                () -> adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.REJECTED, "  ")));
        verifyNoInteractions(claimRepository);
    }

    @Test
    void verify_withPendingStatus_isBadRequest() {
        assertThrows(BadRequestException.class,
                () -> adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.PENDING, null)));
    }

    @Test
    void verify_alreadyDecidedClaim_isBadRequest() {
        claimA.setStatus(ClaimStatus.APPROVED);
        when(claimRepository.findItemIdByClaimId(1L)).thenReturn(Optional.of(10L));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(claimRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(claimA));

        assertThrows(BadRequestException.class,
                () -> adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.APPROVED, null)));
    }

    @Test
    void approve_whenItemNoLongerOpen_isBadRequest() {
        item.setStatus(ItemStatus.RETURNED);
        when(claimRepository.findItemIdByClaimId(1L)).thenReturn(Optional.of(10L));
        when(claimRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(claimA));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class,
                () -> adminService.verifyClaim(1L, new ClaimVerificationRequest(ClaimStatus.APPROVED, null)));
    }

    @Test
    void markReturned_setsReturnedAndClosesPendingClaims() {
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(claimRepository.findByItemIdAndStatus(10L, ClaimStatus.PENDING)).thenReturn(List.of(claimB));

        ItemResponse response = adminService.markItemReturned(10L);

        assertEquals(ItemStatus.RETURNED, response.status());
        assertEquals(ClaimStatus.REJECTED, claimB.getStatus());
    }

    @Test
    void markReturned_twice_isBadRequest() {
        item.setStatus(ItemStatus.RETURNED);
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> adminService.markItemReturned(10L));
    }

    @Test
    void updateStatus_returnedItemCannotBeReopened() {
        item.setStatus(ItemStatus.RETURNED);
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> adminService.updateItemStatus(10L, ItemStatus.OPEN));
    }

    @Test
    void dashboard_collectsCountsFromRepositories() {
        when(userRepository.count()).thenReturn(4L);
        when(itemRepository.count()).thenReturn(7L);
        when(claimRepository.countByStatus(ClaimStatus.PENDING)).thenReturn(2L);

        DashboardStatsResponse stats = adminService.getDashboardStats();

        assertEquals(4L, stats.totalUsers());
        assertEquals(7L, stats.totalItems());
        assertEquals(2L, stats.pendingClaims());
    }
}
