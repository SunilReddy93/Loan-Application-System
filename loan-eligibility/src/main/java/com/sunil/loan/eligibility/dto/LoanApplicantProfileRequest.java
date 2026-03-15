package com.sunil.loan.eligibility.dto;

import com.sunil.loan.eligibility.enums.IncomeSource;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class LoanApplicantProfileRequest {

    @NotBlank(message = "Nationality is required")
    private String nationality;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Income source is required")
    private IncomeSource incomeSource;

    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;

    @NotNull(message = "CIBIL score is required")
    @Min(value = 300, message = "CIBIL score must be at least 300")
    @Max(value = 900, message = "CIBIL score must not exceed 900")
    private Integer cibilScore;

    @NotNull(message = "Existing loan count is required")
    @Min(value = 0, message = "Existing loan count cannot be negative")
    private Integer existingLoanCount;

    @NotNull(message = "Total existing EMI is required")
    @DecimalMin(value = "0.0", message = "Total existing EMI cannot be negative")
    private BigDecimal totalExistingEmi;
}