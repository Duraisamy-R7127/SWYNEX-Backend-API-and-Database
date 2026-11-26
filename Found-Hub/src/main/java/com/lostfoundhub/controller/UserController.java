package com.lostfoundhub.controller;

import com.lostfoundhub.dto.ApiResponse;
import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.dto.UpdateProfileRequest;
import com.lostfoundhub.dto.UserResponse;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok("Profile fetched", userService.getProfile(principal.getId())));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateProfile(principal.getId(), request)));
    }

    @GetMapping("/me/items")
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getMyItems(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Your items fetched", userService.getMyItems(principal.getId(), page, size)));
    }

    @GetMapping("/me/claims")
    public ResponseEntity<ApiResponse<List<ClaimResponse>>> getMyClaims(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok("Your claims fetched", userService.getMyClaims(principal.getId())));
    }

    /** A user can open only their own record; admins can open any. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User fetched",
                userService.getUserDetails(id, principal.getId(), principal.isAdmin())));
    }
}
