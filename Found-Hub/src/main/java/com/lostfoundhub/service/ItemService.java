package com.lostfoundhub.service;

import com.lostfoundhub.dto.ItemRequest;
import com.lostfoundhub.dto.ItemResponse;
import com.lostfoundhub.dto.PageResponse;
import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.exception.BadRequestException;
import com.lostfoundhub.exception.ResourceNotFoundException;
import com.lostfoundhub.model.Claim;
import com.lostfoundhub.model.Item;
import com.lostfoundhub.model.User;
import com.lostfoundhub.repository.ClaimRepository;
import com.lostfoundhub.repository.ItemRepository;
import com.lostfoundhub.repository.ItemSpecifications;
import com.lostfoundhub.repository.UserRepository;
import com.lostfoundhub.util.FileStorageUtil;
import com.lostfoundhub.util.PageUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "itemName", "eventDate", "category", "location", "status", "itemType");

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final NotificationService notificationService;
    private final FileStorageUtil fileStorageUtil;

    public ItemService(ItemRepository itemRepository,
                       UserRepository userRepository,
                       ClaimRepository claimRepository,
                       NotificationService notificationService,
                       FileStorageUtil fileStorageUtil) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.notificationService = notificationService;
        this.fileStorageUtil = fileStorageUtil;
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request, MultipartFile image, Long userId, boolean isAdmin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        // Enforce role-based item type: only ADMIN can post FOUND items
        if (request.getItemType() == ItemType.FOUND && !isAdmin) {
            throw new AccessDeniedException("Only the college administrator can post FOUND items. Students may only report LOST items.");
        }
        // Enforce role-based item type: only USER can post LOST items (admin manages FOUND)
        if (request.getItemType() == ItemType.LOST && isAdmin) {
            throw new BadRequestException("Administrators post FOUND items. Use the Admin panel to add a found item.");
        }

        Item item = new Item();
        item.setUser(user);
        item.setStatus(ItemStatus.OPEN);
        applyRequest(item, request);

        if (hasFile(image)) {
            String stored = fileStorageUtil.store(image);
            deleteFileIfTransactionRollsBack(stored);
            item.setImagePath(stored);
        }
        return ItemResponse.from(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> searchItems(String keyword, String category, String location,
                                                  ItemType type, ItemStatus status,
                                                  int page, int size, String sortBy, String direction) {
        return searchItems(keyword, category, location, type, status, null, page, size, sortBy, direction);
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> searchItems(String keyword, String category, String location,
                                                  ItemType type, ItemStatus status, LocalDate eventDate,
                                                  int page, int size, String sortBy, String direction) {
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sortBy '" + sortBy + "'. Allowed: " + SORTABLE_FIELDS);
        }
        if (!"asc".equalsIgnoreCase(direction) && !"desc".equalsIgnoreCase(direction)) {
            throw new BadRequestException("Invalid direction '" + direction + "'. Use asc or desc");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        if (!"createdAt".equals(sortBy)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Pageable pageable = PageUtil.of(page, size, sort);

        Specification<Item> spec = Specification
                .where(ItemSpecifications.nameContains(keyword))
                .and(ItemSpecifications.categoryEquals(category))
                .and(ItemSpecifications.locationContains(location))
                .and(ItemSpecifications.hasType(type))
                .and(ItemSpecifications.hasStatus(status))
                .and(ItemSpecifications.hasEventDate(eventDate));

        return PageResponse.from(itemRepository.findAll(spec, pageable).map(ItemResponse::from));
    }

    /**
     * Finds possible matching FOUND (OPEN) items for a student's active LOST reports.
     * Uses category, item name, description keywords, location, and date proximity.
     */
    @Transactional(readOnly = true)
    public List<ItemResponse> findMatchesForUser(Long userId) {
        List<Item> lostReports = itemRepository.findByUserIdAndItemTypeAndStatus(
                userId, ItemType.LOST, ItemStatus.OPEN);
        if (lostReports.isEmpty()) {
            return List.of();
        }
        List<Item> foundItems = itemRepository.findByItemTypeAndStatusOrderByCreatedAtDesc(
                ItemType.FOUND, ItemStatus.OPEN);
        if (foundItems.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> bestScores = new HashMap<>();
        for (Item lost : lostReports) {
            for (Item found : foundItems) {
                int score = computeMatchScore(lost, found);
                if (score >= 2) {
                    bestScores.merge(found.getId(), score, Math::max);
                }
            }
        }
        return foundItems.stream()
                .filter(f -> bestScores.containsKey(f.getId()))
                .sorted(Comparator.comparingInt((Item f) -> bestScores.get(f.getId())).reversed())
                .limit(12)
                .map(ItemResponse::from)
                .toList();
    }

    /** Finds possible matching FOUND (OPEN) items for a specific LOST report. */
    @Transactional(readOnly = true)
    public List<ItemResponse> findMatchesForLostItem(Long lostItemId, Long userId, boolean isAdmin) {
        Item lost = findItem(lostItemId);
        if (!isAdmin && !lost.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only view matches for your own lost reports");
        }
        if (lost.getItemType() != ItemType.LOST) {
            throw new BadRequestException("Matches are only generated for LOST reports");
        }
        List<Item> foundItems = itemRepository.findByItemTypeAndStatusOrderByCreatedAtDesc(
                ItemType.FOUND, ItemStatus.OPEN);
        return foundItems.stream()
                .map(found -> Map.entry(found, computeMatchScore(lost, found)))
                .filter(e -> e.getValue() >= 2)
                .sorted(Map.Entry.<Item, Integer>comparingByValue().reversed())
                .limit(12)
                .map(e -> ItemResponse.from(e.getKey()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long id) {
        return ItemResponse.from(findItem(id));
    }

    /**
     * Only the owner (or an admin) can edit. Normal users can edit only OPEN LOST items.
     * Admins can edit FOUND items only. Type cannot be changed after creation.
     * If no new image is sent the old one is kept, unless removeImage is true.
     */
    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request, MultipartFile image, boolean removeImage,
                                   Long currentUserId, boolean isAdmin) {
        Item item = findItem(id);
        checkOwnerOrAdmin(item, currentUserId, isAdmin);
        if (!isAdmin && item.getStatus() != ItemStatus.OPEN) {
            throw new BadRequestException("Only OPEN items can be edited");
        }
        // Prevent type switching
        if (item.getItemType() != request.getItemType()) {
            throw new BadRequestException("Item type cannot be changed after creation");
        }
        applyRequest(item, request);

        String oldImage = item.getImagePath();
        if (hasFile(image)) {
            String stored = fileStorageUtil.store(image);
            deleteFileIfTransactionRollsBack(stored);
            item.setImagePath(stored);
            deleteFileAfterCommit(oldImage);
        } else if (removeImage && oldImage != null) {
            item.setImagePath(null);
            deleteFileAfterCommit(oldImage);
        }
        return ItemResponse.from(itemRepository.save(item));
    }

    /** Owner or admin may delete. Normal users cannot delete CLAIMED/RETURNED items (history must stay). */
    @Transactional
    public void deleteItem(Long id, Long currentUserId, boolean isAdmin) {
        Item item = findItem(id);
        checkOwnerOrAdmin(item, currentUserId, isAdmin);
        if (!isAdmin && item.getStatus() != ItemStatus.OPEN) {
            throw new BadRequestException("Only OPEN items can be deleted");
        }
        for (Claim claim : claimRepository.findByItemIdAndStatus(item.getId(), ClaimStatus.PENDING)) {
            notificationService.notifyUser(claim.getUser(),
                    "The item '" + item.getItemName() + "' you claimed was removed, so your claim was closed.");
        }
        String imagePath = item.getImagePath();
        itemRepository.delete(item);   // its claims are deleted with it (cascade)
        deleteFileAfterCommit(imagePath);
    }

    // ---------------- helpers ----------------

    private int computeMatchScore(Item lost, Item found) {
        int score = 0;
        if (lost.getCategory().equalsIgnoreCase(found.getCategory())) {
            score += 3;
        }
        Set<String> lostNameWords = extractKeywords(lost.getItemName());
        Set<String> foundNameWords = extractKeywords(found.getItemName());
        for (String w : lostNameWords) {
            if (foundNameWords.contains(w)) score += 2;
        }
        Set<String> lostDescWords = extractKeywords(lost.getDescription() + " " +
                (lost.getAdditionalDetails() != null ? lost.getAdditionalDetails() : ""));
        Set<String> foundDescWords = extractKeywords(found.getDescription() + " " +
                (found.getAdditionalDetails() != null ? found.getAdditionalDetails() : ""));
        for (String w : lostDescWords) {
            if (foundDescWords.contains(w) || foundNameWords.contains(w)) score += 1;
        }
        Set<String> lostLocWords = extractKeywords(lost.getLocation());
        Set<String> foundLocWords = extractKeywords(found.getLocation());
        for (String w : lostLocWords) {
            if (foundLocWords.contains(w)) {
                score += 2;
                break;
            }
        }
        if (lost.getEventDate() != null && found.getEventDate() != null) {
            long daysBetween = ChronoUnit.DAYS.between(lost.getEventDate(), found.getEventDate());
            if (daysBetween >= 0 && daysBetween <= 14) {
                score += 1;
            }
        }
        return score;
    }

    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(s -> s.length() >= 3)
                .collect(Collectors.toSet());
    }

    private Item findItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id " + id));
    }

    private void checkOwnerOrAdmin(Item item, Long currentUserId, boolean isAdmin) {
        if (!isAdmin && !item.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("You can only modify your own items");
        }
    }

    private void applyRequest(Item item, ItemRequest request) {
        item.setItemName(request.getItemName().trim());
        item.setCategory(request.getCategory().trim());
        item.setDescription(request.getDescription().trim());
        item.setLocation(request.getLocation().trim());
        item.setEventDate(request.getEventDate());
        item.setItemType(request.getItemType());
        item.setAdditionalDetails(request.getAdditionalDetails() != null ? request.getAdditionalDetails().trim() : null);
    }

    private boolean hasFile(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /** If the database transaction fails after the image was saved, remove the orphan file. */
    private void deleteFileIfTransactionRollsBack(String storedName) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        fileStorageUtil.delete(storedName);
                    }
                }
            });
        }
    }

    /** Old/removed images are deleted only after the database change is committed. */
    private void deleteFileAfterCommit(String storedName) {
        if (storedName == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorageUtil.delete(storedName);
                }
            });
        } else {
            fileStorageUtil.delete(storedName);
        }
    }
}
