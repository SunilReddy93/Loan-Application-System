package com.sunil.loan.eligibility.repository;

import com.sunil.loan.eligibility.entity.LoanApplication;
import com.sunil.loan.eligibility.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByUserId(Long userId);

    Optional<LoanApplication> findByIdempotencyKey(String idempotencyKey);

    boolean existsByUserIdAndStatus(Long userId, LoanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoanApplication l WHERE l.id = :id")
    Optional<LoanApplication> findByIdWithLock(@Param("id") Long id);
}