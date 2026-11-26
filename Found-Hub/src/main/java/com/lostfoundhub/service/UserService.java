package com.lostfoundhub.service;

import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.dto.UpdateProfileRequest;
import com.lostfoundhub.dto.UserResponse;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.ClaimRepository;
import com.lostfoundhub.repository.ItemRepository;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.util.PageUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ClaimRepository claimRepository;

    public UserService(UserRepository userRepository, ItemRepository itemRepository, ClaimRepository claimRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.claimRepository = claimRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    /** A user may view only their own details; an admin may view anyone. */
    @Transactional(readOnly = true)
    public UserResponse getUserDetails(Long targetUserId, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !targetUserId.equals(currentUserId)) {
            throw new AccessDeniedException("You can only view your own details");
        }
        return UserResponse.from(findUser(targetUserId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());
        user.setPhone(request.phone());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> getMyItems(Long userId, int page, int size) {
        return PageResponse.from(itemRepository.findByUserId(userId, PageUtil.newestFirst(page, size))
                .map(ItemResponse::from));
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getMyClaims(Long userId) {
        return claimRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(ClaimResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(int page, int size) {
        return PageResponse.from(userRepository.findAll(PageUtil.newestFirst(page, size)).map(UserResponse::from));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
    }
}
