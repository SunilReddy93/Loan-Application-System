package com.sunil.loan.eligibility.dto;

import com.sunil.loan.eligibility.enums.LoanStatus;
import com.sunil.loan.eligibility.enums.LoanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    private Long id;
    private Long userId;
    private LoanType loanType;
    private BigDecimal requestedAmount;
    private Integer tenureMonths;
    private BigDecimal interestRate;
    private LoanStatus status;
    private String rejectionReason;
    private LoanApplicantProfileResponse applicantProfile;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}