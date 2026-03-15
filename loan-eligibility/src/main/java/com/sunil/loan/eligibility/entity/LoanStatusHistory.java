package com.sunil.loan.eligibility.entity;

import com.sunil.loan.eligibility.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_status_history")
public class LoanStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private LoanStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private LoanStatus toStatus;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}