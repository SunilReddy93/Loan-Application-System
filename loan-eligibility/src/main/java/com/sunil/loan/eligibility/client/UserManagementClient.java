package com.sunil.loan.eligibility.client;


import com.sunil.loan.eligibility.dto.UserResponse;
import com.sunil.loan.eligibility.exception.ServiceNotAvailable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementClient {

    private final WebClient userManagementWebClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Value(("${internal.api.key}"))
    private String internalAPIKey;

    public UserResponse getUserById(Long userId) {
        return circuitBreakerFactory
                .create("user-management-cb")
                .run(
                        () -> userManagementWebClient.get()
                                .uri("/api/internal/users/{id}", userId)
                                .header("X-Internal-Api-Key", internalAPIKey)
                                .retrieve()
                                .bodyToMono(UserResponse.class)
                                .block(),
                        throwable -> fallback(throwable)
                );
    }

    private UserResponse fallback(Throwable throwable) {
        log.error("user-management is unavailable: {}", throwable.getMessage());
        throw new ServiceNotAvailable(
                "User verification service is currently unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
