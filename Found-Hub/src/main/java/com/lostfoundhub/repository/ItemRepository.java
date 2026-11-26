package com.lostfoundhub.repository;

import com.lostfoundhub.enums.ItemStatus;
import com.lostfoundhub.enums.ItemType;
import com.lostfoundhub.model.Item;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {

    /** Dynamic search/filter; loads the posting user together with the items. */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Item> findAll(Specification<Item> spec, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Item> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    java.util.List<Item> findByUserIdAndItemTypeAndStatus(Long userId, ItemType itemType, ItemStatus status);

    @EntityGraph(attributePaths = "user")
    java.util.List<Item> findByItemTypeAndStatusOrderByCreatedAtDesc(ItemType itemType, ItemStatus status);

    /** Row lock so two requests cannot change the same item at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id = :id")
    Optional<Item> findByIdForUpdate(@Param("id") Long id);

    long countByItemType(ItemType itemType);

    long countByStatus(ItemStatus status);
}
