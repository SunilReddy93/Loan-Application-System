package com.sunil.loan.eligibility.dto;

import com.sunil.loan.eligibility.enums.IncomeSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicantProfileResponse {

    private Long id;
    private String nationality;
    private String location;
    private IncomeSource incomeSource;
    private BigDecimal monthlyIncome;
    private Integer cibilScore;
    private Integer existingLoanCount;
    private BigDecimal totalExistingEmi;
}