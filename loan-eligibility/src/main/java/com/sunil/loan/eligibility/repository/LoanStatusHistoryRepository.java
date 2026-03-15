package com.sunil.loan.eligibility.repository;

import com.sunil.loan.eligibility.entity.LoanStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LoanStatusHistoryRepository extends JpaRepository<LoanStatusHistory, Long> {

    List<LoanStatusHistory> findByLoanApplicationIdOrderByChangedAtAsc(Long loanApplicationId);
}