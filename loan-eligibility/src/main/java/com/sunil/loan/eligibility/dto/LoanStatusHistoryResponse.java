package com.sunil.loan.eligibility.dto;

import com.sunil.loan.eligibility.enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanStatusHistoryResponse {

    private Long id;
    private LoanStatus fromStatus;
    private LoanStatus toStatus;
    private String changedBy;
    private String remarks;
    private LocalDateTime changedAt;
}