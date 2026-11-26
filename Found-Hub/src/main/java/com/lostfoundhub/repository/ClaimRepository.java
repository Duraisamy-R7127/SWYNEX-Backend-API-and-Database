package com.lostfoundhub.repository;

import com.lostfoundhub.enums.ClaimStatus;
import com.lostfoundhub.model.Claim;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Override
    @EntityGraph(attributePaths = {"item", "user"})
    Page<Claim> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"item", "user"})
    Page<Claim> findByStatus(ClaimStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"item", "user"})
    List<Claim> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Claim> findByItemIdAndStatus(Long itemId, ClaimStatus status);

    boolean existsByItemIdAndUserIdAndStatus(Long itemId, Long userId, ClaimStatus status);

    long countByStatus(ClaimStatus status);

    /** Id of the item a claim belongs to (does not load the item, so the later item lock reads fresh data). */
    @Query("select c.item.id from Claim c where c.id = :id")
    Optional<Long> findItemIdByClaimId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Claim c where c.id = :id")
    Optional<Claim> findByIdForUpdate(@Param("id") Long id);
}
