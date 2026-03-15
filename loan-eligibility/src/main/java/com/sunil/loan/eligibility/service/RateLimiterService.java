package com.sunil.loan.eligibility.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimiterService {

    private final Map<Long, Bucket> bucketCache = new ConcurrentHashMap<>();

    public boolean isAllowed(Long userId) {
        Bucket bucket = bucketCache.computeIfAbsent(userId, this::createNewBucket);
        boolean allowed = bucket.tryConsume(1);
        if (!allowed) {
            log.warn("Rate limit exceeded for userId: {}", userId);
        }
        return allowed;
    }

    private Bucket createNewBucket(Long userId) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofHours(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}