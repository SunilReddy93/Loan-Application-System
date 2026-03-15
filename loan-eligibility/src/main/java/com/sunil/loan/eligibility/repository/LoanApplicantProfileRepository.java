package com.sunil.loan.eligibility.repository;

import com.sunil.loan.eligibility.entity.LoanApplicantProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoanApplicantProfileRepository extends JpaRepository<LoanApplicantProfile, Long> {

    Optional<LoanApplicantProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}