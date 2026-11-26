package com.lostfoundhub.dto;

import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.model.Item;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemResponse(
        Long id,
        String itemName,
        String category,
        String description,
        String additionalDetails,
        String imagePath,
        String imageUrl,
        String location,
        LocalDate eventDate,
        ItemType itemType,
        ItemStatus status,
        Long postedById,
        String postedByName,
        LocalDateTime createdAt
) {

    /** Must be called while the item's user is loadable (inside a service transaction). */
    public static ItemResponse from(Item item) {
        String imagePath = item.getImagePath();
        String imageUrl = imagePath == null ? null : "/api/items/images/" + imagePath;
        return new ItemResponse(item.getId(), item.getItemName(), item.getCategory(), item.getDescription(),
                item.getAdditionalDetails(), imagePath, imageUrl, item.getLocation(), item.getEventDate(),
                item.getItemType(), item.getStatus(),
                item.getUser().getId(), item.getUser().getName(), item.getCreatedAt());
    }
}
