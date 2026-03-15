package com.sunil.loan.eligibility.entity;

import com.sunil.loan.eligibility.enums.IncomeSource;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "loan_applicant_profile",
        indexes = {
                @Index(name = "idx_profile_user_id", columnList = "user_id")
        }
)
public class LoanApplicantProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    @Column(name = "location", nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_source", nullable = false)
    private IncomeSource incomeSource;

    @Column(name = "monthly_income", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "cibil_score", nullable = false)
    private Integer cibilScore;

    @Column(name = "existing_loan_count", nullable = false)
    private Integer existingLoanCount;

    @Column(name = "total_existing_emi", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExistingEmi;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}