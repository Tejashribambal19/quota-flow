package com.tejashri.quota;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@SpringBootTest
class QuotaConcurrencyTest {

    private static final String TEST_KEY =
            "quota:concurrency-test:API_REQUEST";

    private static final String SCRIPT = """
            local current = tonumber(
                redis.call('GET', KEYS[1]) or '0'
            )

            local amount = tonumber(ARGV[1])
            local quotaLimit = tonumber(ARGV[2])
            local nextValue = current + amount

            if nextValue > quotaLimit then
                return -(current + 1)
            end

            return redis.call(
                'INCRBY',
                KEYS[1],
                amount
            )
            """;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanup() {
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    void shouldNeverExceedQuotaUnderConcurrentRequests()
            throws Exception {

        redisTemplate.delete(TEST_KEY);

        int requestCount = 200;
        int requestAmount = 100;
        long quotaLimit = 10_000;

        AtomicInteger acceptedRequests =
                new AtomicInteger();

        ExecutorService executor =
                Executors.newFixedThreadPool(20);

        List<Future<?>> futures = new ArrayList<>();

        DefaultRedisScript<Long> redisScript =
                new DefaultRedisScript<>(
                        SCRIPT,
                        Long.class
                );

        try {
            for (int index = 0;
                 index < requestCount;
                 index++) {

                futures.add(executor.submit(() -> {
                    Long result = redisTemplate.execute(
                            redisScript,
                            List.of(TEST_KEY),
                            String.valueOf(requestAmount),
                            String.valueOf(quotaLimit)
                    );

                    if (result != null && result >= 0) {
                        acceptedRequests.incrementAndGet();
                    }
                }));
            }

            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        String storedValue =
                redisTemplate.opsForValue().get(TEST_KEY);

        assertEquals(100, acceptedRequests.get());
        assertEquals("10000", storedValue);
    }
}