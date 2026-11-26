package com.lostfoundhub.controller;

import com.lostfoundhub.dto.ApiResponse;
import com.lostfoundhub.dto.ClaimResponse;
import com.lostfoundhub.dto.ClaimVerificationRequest;
import com.lostfoundhub.dto.DashboardStatsResponse;
import com.lostfoundhub.dto.ItemRequest;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.ItemStatusRequest;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.dto.UserResponse;
import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.service.AdminService;
import com.lostfoundhub.service.ItemService;
import com.lostfoundhub.service.UserService;
import com.lostfoundhub.util.FileStorageUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Every endpoint here requires the ADMIN role (checked by SecurityConfig AND @PreAuthorize). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final ItemService itemService;
    private final FileStorageUtil fileStorageUtil;

    public AdminController(AdminService adminService, UserService userService,
                           ItemService itemService, FileStorageUtil fileStorageUtil) {
        this.adminService = adminService;
        this.userService = userService;
        this.itemService = itemService;
        this.fileStorageUtil = fileStorageUtil;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard statistics fetched", adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", userService.getAllUsers(page, size)));
    }

    /** View ALL items (both LOST and FOUND) for admin overview. */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(ApiResponse.ok("Items fetched",
                itemService.searchItems(keyword, category, location, type, status, page, size, sortBy, direction)));
    }

    /** View all student LOST reports. */
    @GetMapping("/lost-reports")
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getLostReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Lost reports fetched",
                itemService.searchItems(keyword, category, location, ItemType.LOST, status, page, size, "createdAt", "desc")));
    }

    /** View all admin-uploaded FOUND items. */
    @GetMapping("/found-items")
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getFoundItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Found items fetched",
                itemService.searchItems(keyword, category, location, ItemType.FOUND, status, page, size, "createdAt", "desc")));
    }

    /**
     * Admin uploads a new FOUND item (multipart/form-data).
     * itemType is forced to FOUND server-side regardless of what is sent.
     */
    @PostMapping(value = "/found-items", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> addFoundItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute ItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        // Force type = FOUND, admin is posting
        request.setItemType(ItemType.FOUND);
        ItemResponse created = itemService.createItem(request, image, principal.getId(), true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Found item added successfully", created));
    }

    /** Admin updates a FOUND item. */
    @PutMapping(value = "/found-items/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> updateFoundItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @ModelAttribute ItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) {
        request.setItemType(ItemType.FOUND);
        ItemResponse updated = itemService.updateItem(id, request, image, removeImage,
                principal.getId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Found item updated successfully", updated));
    }

    /** Admin deletes a FOUND item listing. */
    @DeleteMapping("/found-items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFoundItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        itemService.deleteItem(id, principal.getId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Found item deleted successfully"));
    }

    @GetMapping("/claims")
    public ResponseEntity<ApiResponse<PageResponse<ClaimResponse>>> getClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Claims fetched", adminService.getClaims(status, page, size)));
    }

    @GetMapping("/claims/pending")
    public ResponseEntity<ApiResponse<PageResponse<ClaimResponse>>> getPendingClaims(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Pending claims fetched",
                adminService.getClaims(ClaimStatus.PENDING, page, size)));
    }

    /** Approve or reject: body {"status":"APPROVED"|"REJECTED","adminRemarks":"..."} */
    @PatchMapping("/claims/{id}/verify")
    public ResponseEntity<ApiResponse<ClaimResponse>> verifyClaim(@PathVariable Long id,
                                                                  @Valid @RequestBody ClaimVerificationRequest request) {
        ClaimResponse result = adminService.verifyClaim(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Claim " + result.status().name().toLowerCase() + " successfully", result));
    }

    @PatchMapping("/items/{id}/returned")
    public ResponseEntity<ApiResponse<ItemResponse>> markReturned(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Item marked as RETURNED", adminService.markItemReturned(id)));
    }

    @PatchMapping("/items/{id}/status")
    public ResponseEntity<ApiResponse<ItemResponse>> updateItemStatus(@PathVariable Long id,
                                                                      @Valid @RequestBody ItemStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Item status updated", adminService.updateItemStatus(id, request.status())));
    }
}
