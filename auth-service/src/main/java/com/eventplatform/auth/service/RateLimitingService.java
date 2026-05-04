package com.eventplatform.auth.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingService {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, List<Instant>> attemptMap = new ConcurrentHashMap<>();

    public boolean isLoginAllowed(String key) {
        Instant now = Instant.now();
        Instant windowStart = now.minus(WINDOW);

        attemptMap.compute(key, (k, list) -> {
            List<Instant> l = (list != null) ? list : new ArrayList<>();
            l.removeIf(t -> t.isBefore(windowStart));
            l.add(now);
            return l;
        });

        return attemptMap.get(key).size() <= MAX_ATTEMPTS;
    }
}
