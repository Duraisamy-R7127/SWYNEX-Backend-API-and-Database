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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    @Mock
    private ClaimRepository claimRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    private ClaimService claimService;
    private User owner;
    private User claimant;
    private Item item;

    @BeforeEach
    void setUp() {
        claimService = new ClaimService(claimRepository, itemRepository, userRepository, notificationService);
        owner = user(1L, "Owner");
        claimant = user(2L, "Claimant");
        item = new Item();
        item.setId(10L);
        item.setItemName("Blue Wallet");
        item.setItemType(ItemType.FOUND);
        item.setUser(owner);
        item.setStatus(ItemStatus.OPEN);
    }

    private User user(Long id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private ClaimRequest request() {
        return new ClaimRequest(10L, "It has my college ID card inside");
    }

    @Test
    void submitClaim_onOwnItem_isRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> claimService.submitClaim(request(), 1L));

        assertTrue(ex.getMessage().contains("own item"));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitClaim_onReturnedItem_isRejected() {
        item.setStatus(ItemStatus.RETURNED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> claimService.submitClaim(request(), 2L));

        assertTrue(ex.getMessage().contains("returned"));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitClaim_onAlreadyClaimedItem_isRejected() {
        item.setStatus(ItemStatus.CLAIMED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> claimService.submitClaim(request(), 2L));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitClaim_duplicatePending_isRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(claimRepository.existsByItemIdAndUserIdAndStatus(10L, 2L, ClaimStatus.PENDING)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> claimService.submitClaim(request(), 2L));
        verify(claimRepository, never()).save(any());
    }

    @Test
    void submitClaim_unknownItem_isNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> claimService.submitClaim(request(), 2L));
    }

    @Test
    void submitClaim_success_savesPendingClaimAndNotifiesEveryone() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(claimant));
        when(itemRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(item));
        when(claimRepository.existsByItemIdAndUserIdAndStatus(10L, 2L, ClaimStatus.PENDING)).thenReturn(false);
        when(claimRepository.save(any(Claim.class))).thenAnswer(inv -> {
            Claim c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        ClaimResponse response = claimService.submitClaim(request(), 2L);

        assertEquals(99L, response.id());
        assertEquals(ClaimStatus.PENDING, response.status());
        assertEquals("Blue Wallet", response.itemName());
        verify(notificationService, times(2)).notifyUser(any(User.class), anyString()); // claimant + item owner
        verify(notificationService).notifyAdmins(anyString());
    }
}
