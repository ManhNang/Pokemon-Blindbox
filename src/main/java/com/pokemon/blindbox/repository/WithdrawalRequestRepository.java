package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.WithdrawalRequest;
import com.pokemon.blindbox.entity.enums.WithdrawalStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WithdrawalRequest w WHERE w.id = :id")
    Optional<WithdrawalRequest> findByIdForUpdate(@Param("id") Long id);

    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WithdrawalRequest> findAllByOrderByCreatedAtDesc();

    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalStatus status);

    long countByStatus(WithdrawalStatus status);
}
