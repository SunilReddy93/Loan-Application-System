package com.sunil.loan.eligibility.controller;

import com.sunil.loan.eligibility.dto.LoanApplicationRequest;
import com.sunil.loan.eligibility.dto.LoanApplicationResponse;
import com.sunil.loan.eligibility.dto.LoanStatusHistoryResponse;
import com.sunil.loan.eligibility.security.model.UserPrincipal;
import com.sunil.loan.eligibility.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping("/apply")
    public ResponseEntity<LoanApplicationResponse> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.applyForLoan(request, principal.getUserId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponse> getLoanById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.getLoanById(id, principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<LoanApplicationResponse>> getMyLoans(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LoanApplicationResponse> response = loanService.getMyLoans(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanApplicationResponse> approveLoan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.approveLoan(id, principal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanApplicationResponse> rejectLoan(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.rejectLoan(id, reason, principal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/disburse")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanApplicationResponse> disburseLoan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.disburseLoan(id, principal.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<LoanApplicationResponse> closeLoan(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        LoanApplicationResponse response = loanService.closeLoan(id, principal.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<LoanStatusHistoryResponse>> getLoanHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LoanStatusHistoryResponse> response = loanService.getLoanHistory(id, principal.getUserId());
        return ResponseEntity.ok(response);
    }
}