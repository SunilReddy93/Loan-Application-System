package com.sunil.loan.eligibility.dto;

import com.sunil.loan.eligibility.enums.LoanType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicationRequest {

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "1000.0", message = "Loan amount must be at least 1000")
    private BigDecimal requestedAmount;

    @NotNull(message = "Tenure is required")
    @Min(value = 3, message = "Tenure must be at least 3 months")
    @Max(value = 360, message = "Tenure cannot exceed 360 months")
    private Integer tenureMonths;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    @Valid
    @NotNull(message = "Applicant profile is required")
    private LoanApplicantProfileRequest applicantProfile;
}