package com.lostfoundhub.repository;

import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.model.Item;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

/** Small reusable filters that are combined for item search. A null/blank value means "no filter". */
public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    /**
     * Case-insensitive keyword search across item name, description, and additional details.
     */
    public static Specification<Item> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("itemName")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("additionalDetails")), pattern)
            );
        };
    }

    public static Specification<Item> categoryEquals(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
                ? cb.conjunction()
                : cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase(Locale.ROOT));
    }

    public static Specification<Item> locationContains(String location) {
        return (root, query, cb) -> location == null || location.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<Item> hasType(ItemType type) {
        return (root, query, cb) -> type == null ? cb.conjunction() : cb.equal(root.get("itemType"), type);
    }

    public static Specification<Item> hasStatus(ItemStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Item> hasEventDate(LocalDate eventDate) {
        return (root, query, cb) -> eventDate == null ? cb.conjunction() : cb.equal(root.get("eventDate"), eventDate);
    }
}
