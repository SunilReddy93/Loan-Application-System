package com.sunil.loan.eligibility.service;

import com.sunil.loan.eligibility.client.UserManagementClient;
import com.sunil.loan.eligibility.dto.*;
import com.sunil.loan.eligibility.entity.LoanApplicantProfile;
import com.sunil.loan.eligibility.entity.LoanApplication;
import com.sunil.loan.eligibility.entity.LoanStatusHistory;
import com.sunil.loan.eligibility.enums.LoanStatus;
import com.sunil.loan.eligibility.exception.CustomException;
import com.sunil.loan.eligibility.repository.LoanApplicantProfileRepository;
import com.sunil.loan.eligibility.repository.LoanApplicationRepository;
import com.sunil.loan.eligibility.repository.LoanStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanApplicantProfileRepository loanApplicantProfileRepository;
    private final LoanStatusHistoryRepository loanStatusHistoryRepository;
    private final UserManagementClient userManagementClient;

    // Apply for a loan - main entry point for loan application
    // @Transactional ensures all DB operations succeed or all roll back
    @Transactional
    public LoanApplicationResponse applyForLoan(LoanApplicationRequest request, Long userId) {

        // Step 1 - Check idempotency key to prevent duplicate applications
        loanApplicationRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new CustomException(
                            "Loan application already exists with this idempotency key",
                            HttpStatus.CONFLICT);
                });

        // Step 2 - Verify user exists and is active in user-management service
        UserResponse user = userManagementClient.getUserById(userId);
        if (!user.getStatus().equals("ACTIVE")) {
            throw new CustomException("User account is inactive", HttpStatus.FORBIDDEN);
        }

        // Step 3 - Run eligibility rules engine
        checkEligibility(request.getApplicantProfile(), userId);

        // Step 4 - Build and save applicant financial profile
        LoanApplicantProfile profile = buildApplicantProfile(request, userId);
        LoanApplicantProfile savedProfile = loanApplicantProfileRepository.save(profile);

        // Step 5 - Build and save loan application
        LoanApplication loanApplication = LoanApplication.builder()
                .userId(userId)
                .applicantProfile(savedProfile)
                .loanType(request.getLoanType())
                .requestedAmount(request.getRequestedAmount())
                .tenureMonths(request.getTenureMonths())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        LoanApplication savedApplication = loanApplicationRepository.save(loanApplication);

        // Step 6 - Record initial status in history for audit trail
        saveStatusHistory(savedApplication, null, LoanStatus.APPLIED, "system", "Loan application submitted");

        log.info("Loan application created successfully for user: {}", userId);

        return mapToLoanApplicationResponse(savedApplication);
    }

    // Get a specific loan by id - user can only view their own loans
    public LoanApplicationResponse getLoanById(Long loanId, Long userId) {

        LoanApplication loan = loanApplicationRepository.findById(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Ensure user can only access their own loan
        if (!loan.getUserId().equals(userId)) {
            throw new CustomException(
                    "You are not authorized to view this loan", HttpStatus.FORBIDDEN);
        }

        return mapToLoanApplicationResponse(loan);
    }

    // Get all loans for the currently logged-in user
    public List<LoanApplicationResponse> getMyLoans(Long userId) {
        return loanApplicationRepository.findByUserId(userId)
                .stream()
                .map(this::mapToLoanApplicationResponse)
                .collect(Collectors.toList());
    }

    // Approve a loan - admin only
    // @Transactional ensures status update and history save are atomic
    @Transactional
    public LoanApplicationResponse approveLoan(Long loanId, String adminUsername) {

        LoanApplication loan = loanApplicationRepository.findByIdWithLock(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Only APPLIED loans can be approved
        if (loan.getStatus() != LoanStatus.APPLIED) {
            throw new CustomException(
                    "Only APPLIED loans can be approved", HttpStatus.BAD_REQUEST);
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.APPROVED);

        // Calculate and assign interest rate based on CIBIL score
        loan.setInterestRate(calculateInterestRate(loan));

        LoanApplication updatedLoan = loanApplicationRepository.save(loan);

        // Record status change in history
        saveStatusHistory(updatedLoan, previousStatus, LoanStatus.APPROVED, adminUsername, "Loan approved");

        log.info("Loan {} approved by {}", loanId, adminUsername);

        return mapToLoanApplicationResponse(updatedLoan);
    }

    // Reject a loan with a reason - admin only
    @Transactional
    public LoanApplicationResponse rejectLoan(Long loanId, String reason, String adminUsername) {

        LoanApplication loan = loanApplicationRepository.findByIdWithLock(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Only APPLIED loans can be rejected
        if (loan.getStatus() != LoanStatus.APPLIED) {
            throw new CustomException(
                    "Only APPLIED loans can be rejected", HttpStatus.BAD_REQUEST);
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.REJECTED);
        loan.setRejectionReason(reason);

        LoanApplication updatedLoan = loanApplicationRepository.save(loan);

        // Record rejection reason in history
        saveStatusHistory(updatedLoan, previousStatus, LoanStatus.REJECTED, adminUsername, reason);

        log.info("Loan {} rejected by {}", loanId, adminUsername);

        return mapToLoanApplicationResponse(updatedLoan);
    }

    // Disburse an approved loan - admin only
    @Transactional
    public LoanApplicationResponse disburseLoan(Long loanId, String adminUsername) {

        LoanApplication loan = loanApplicationRepository.findByIdWithLock(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Only APPROVED loans can be disbursed
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new CustomException(
                    "Only APPROVED loans can be disbursed", HttpStatus.BAD_REQUEST);
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.DISBURSED);

        LoanApplication updatedLoan = loanApplicationRepository.save(loan);

        saveStatusHistory(updatedLoan, previousStatus, LoanStatus.DISBURSED, adminUsername, "Loan disbursed");

        log.info("Loan {} disbursed by {}", loanId, adminUsername);

        return mapToLoanApplicationResponse(updatedLoan);
    }

    // Close a disbursed loan - admin only
    @Transactional
    public LoanApplicationResponse closeLoan(Long loanId, String adminUsername) {

        LoanApplication loan = loanApplicationRepository.findByIdWithLock(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Only DISBURSED loans can be closed
        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new CustomException(
                    "Only DISBURSED loans can be closed", HttpStatus.BAD_REQUEST);
        }

        LoanStatus previousStatus = loan.getStatus();
        loan.setStatus(LoanStatus.CLOSED);

        LoanApplication updatedLoan = loanApplicationRepository.save(loan);

        saveStatusHistory(updatedLoan, previousStatus, LoanStatus.CLOSED, adminUsername, "Loan closed");

        log.info("Loan {} closed by {}", loanId, adminUsername);

        return mapToLoanApplicationResponse(updatedLoan);
    }

    // Get full status history for a loan - user can only view their own loan history
    public List<LoanStatusHistoryResponse> getLoanHistory(Long loanId, Long userId, String role) {

        LoanApplication loan = loanApplicationRepository.findById(loanId)
                .orElseThrow(() -> new CustomException(
                        "Loan application not found", HttpStatus.NOT_FOUND));

        // Ensure user can only access their own loan history
        if (!role.equals("ROLE_ADMIN") && !loan.getUserId().equals(userId)) {
            throw new CustomException(
                    "You are not authorized to view this loan history", HttpStatus.FORBIDDEN);
        }

        return loanStatusHistoryRepository
                .findByLoanApplicationIdOrderByChangedAtAsc(loanId)
                .stream()
                .map(this::mapToStatusHistoryResponse)
                .collect(Collectors.toList());
    }

    // Runs all eligibility rules - throws exception on first failure
    private void checkEligibility(LoanApplicantProfileRequest profile, Long userId) {

        if (!isCibilScoreValid(profile)) {
            throw new CustomException(
                    "Loan rejected: CIBIL score must be at least 650",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!isIncomeValid(profile)) {
            throw new CustomException(
                    "Loan rejected: Monthly income must be at least 25,000",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!isExistingLoanCountValid(profile)) {
            throw new CustomException(
                    "Loan rejected: Cannot have more than 3 existing loans",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (isEmiRatioExceeded(profile)) {
            throw new CustomException(
                    "Loan rejected: Total existing EMI exceeds 50% of monthly income",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (hasActiveLoanApplication(userId)) {
            throw new CustomException(
                    "Loan rejected: You already have an active loan application",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // Rule 1 - CIBIL score must be at least 650
    private boolean isCibilScoreValid(LoanApplicantProfileRequest profile) {
        return profile.getCibilScore() >= 650;
    }

    // Rule 2 - Monthly income must be at least 25,000
    private boolean isIncomeValid(LoanApplicantProfileRequest profile) {
        return profile.getMonthlyIncome().compareTo(new BigDecimal("25000")) >= 0;
    }

    // Rule 3 - Cannot have more than 3 existing loans
    private boolean isExistingLoanCountValid(LoanApplicantProfileRequest profile) {
        return profile.getExistingLoanCount() <= 3;
    }

    // Rule 4 - Total existing EMI must not exceed 50% of monthly income
    private boolean isEmiRatioExceeded(LoanApplicantProfileRequest profile) {
        BigDecimal emiRatio = profile.getTotalExistingEmi()
                .divide(profile.getMonthlyIncome(), 2, RoundingMode.HALF_UP);
        return emiRatio.compareTo(new BigDecimal("0.50")) > 0;
    }

    // Rule 5 - Must not have an active loan application already
    private boolean hasActiveLoanApplication(Long userId) {
        return loanApplicationRepository.existsByUserIdAndStatus(userId, LoanStatus.APPLIED) ||
                loanApplicationRepository.existsByUserIdAndStatus(userId, LoanStatus.UNDER_REVIEW);
    }

    // Builds LoanApplicantProfile entity from request
    private LoanApplicantProfile buildApplicantProfile(LoanApplicationRequest request, Long userId) {
        LoanApplicantProfileRequest profileRequest = request.getApplicantProfile();
        return LoanApplicantProfile.builder()
                .userId(userId)
                .nationality(profileRequest.getNationality())
                .location(profileRequest.getLocation())
                .incomeSource(profileRequest.getIncomeSource())
                .monthlyIncome(profileRequest.getMonthlyIncome())
                .cibilScore(profileRequest.getCibilScore())
                .existingLoanCount(profileRequest.getExistingLoanCount())
                .totalExistingEmi(profileRequest.getTotalExistingEmi())
                .build();
    }

    // Calculates interest rate based on CIBIL score - higher score = lower rate
    private BigDecimal calculateInterestRate(LoanApplication loan) {
        int cibilScore = loan.getApplicantProfile().getCibilScore();
        if (cibilScore >= 800) return new BigDecimal("8.5");
        if (cibilScore >= 750) return new BigDecimal("10.0");
        if (cibilScore >= 700) return new BigDecimal("12.0");
        return new BigDecimal("14.5");
    }

    // Saves a status change record for audit trail
    private void saveStatusHistory(LoanApplication loan, LoanStatus fromStatus,
                                   LoanStatus toStatus, String changedBy, String remarks) {
        LoanStatusHistory history = LoanStatusHistory.builder()
                .loanApplication(loan)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(changedBy)
                .remarks(remarks)
                .build();
        loanStatusHistoryRepository.save(history);
    }

    // Maps LoanApplication entity to response DTO
    private LoanApplicationResponse mapToLoanApplicationResponse(LoanApplication loan) {
        return LoanApplicationResponse.builder()
                .id(loan.getId())
                .userId(loan.getUserId())
                .loanType(loan.getLoanType())
                .requestedAmount(loan.getRequestedAmount())
                .tenureMonths(loan.getTenureMonths())
                .interestRate(loan.getInterestRate())
                .status(loan.getStatus())
                .rejectionReason(loan.getRejectionReason())
                .applicantProfile(mapToProfileResponse(loan.getApplicantProfile()))
                .appliedAt(loan.getAppliedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    // Maps LoanApplicantProfile entity to response DTO
    private LoanApplicantProfileResponse mapToProfileResponse(LoanApplicantProfile profile) {
        return LoanApplicantProfileResponse.builder()
                .id(profile.getId())
                .nationality(profile.getNationality())
                .location(profile.getLocation())
                .incomeSource(profile.getIncomeSource())
                .monthlyIncome(profile.getMonthlyIncome())
                .cibilScore(profile.getCibilScore())
                .existingLoanCount(profile.getExistingLoanCount())
                .totalExistingEmi(profile.getTotalExistingEmi())
                .build();
    }

    // Maps LoanStatusHistory entity to response DTO
    private LoanStatusHistoryResponse mapToStatusHistoryResponse(LoanStatusHistory history) {
        return LoanStatusHistoryResponse.builder()
                .id(history.getId())
                .fromStatus(history.getFromStatus())
                .toStatus(history.getToStatus())
                .changedBy(history.getChangedBy())
                .remarks(history.getRemarks())
                .changedAt(history.getChangedAt())
                .build();
    }
}