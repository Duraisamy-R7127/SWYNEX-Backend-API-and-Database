package com.lostfoundhub.controller;

import com.lostfoundhub.dto.ApiResponse;
import com.lostfoundhub.dto.ItemRequest;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.security.CustomUserDetails;
import com.lostfoundhub.service.ItemService;
import com.lostfoundhub.util.FileStorageUtil;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;
    private final FileStorageUtil fileStorageUtil;

    public ItemController(ItemService itemService, FileStorageUtil fileStorageUtil) {
        this.itemService = itemService;
        this.fileStorageUtil = fileStorageUtil;
    }

    /** Report a LOST item (users) or post a FOUND item (admin via admin panel). multipart/form-data. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @ModelAttribute ItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        ItemResponse created = itemService.createItem(request, image, principal.getId(), principal.isAdmin());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Item posted successfully", created));
    }

    /** Public search / filter / sort / pagination. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ItemResponse>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) ItemType type,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        PageResponse<ItemResponse> result =
                itemService.searchItems(keyword, category, location, type, status, eventDate, page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.ok("Items fetched", result));
    }

    /** Possible matching FOUND items for the logged-in student's LOST reports. */
    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getMyMatches(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.ok("Matching found items fetched",
                itemService.findMatchesForUser(principal.getId())));
    }

    /** Possible matching FOUND items for a specific LOST report. */
    @GetMapping("/{id}/matches")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getMatchesForItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Matching found items fetched",
                itemService.findMatchesForLostItem(id, principal.getId(), principal.isAdmin())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Item fetched", itemService.getItem(id)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> updateItem(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @ModelAttribute ItemRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) {
        ItemResponse updated = itemService.updateItem(id, request, image, removeImage,
                principal.getId(), principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("Item updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @PathVariable Long id) {
        itemService.deleteItem(id, principal.getId(), principal.isAdmin());
        return ResponseEntity.ok(ApiResponse.ok("Item deleted successfully"));
    }

    /** Returns the image bytes (public) - use the imageUrl from an item response. */
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        Resource resource = fileStorageUtil.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileStorageUtil.contentTypeOf(filename)))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(resource);
    }
}
