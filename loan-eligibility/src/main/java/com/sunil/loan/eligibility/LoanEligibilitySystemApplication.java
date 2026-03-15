package com.sunil.loan.eligibility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class LoanEligibilitySystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanEligibilitySystemApplication.class, args);
    }

}
