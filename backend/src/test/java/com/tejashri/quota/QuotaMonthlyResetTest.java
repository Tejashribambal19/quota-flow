package com.tejashri.quota;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.tejashri.quota.domain.ResourceType;

@SpringBootTest
class QuotaMonthlyResetTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String currentMonthKey;
    private String nextMonthKey;

    @AfterEach
    void cleanUp() {
        if (currentMonthKey != null) {
            redisTemplate.delete(currentMonthKey);
        }

        if (nextMonthKey != null) {
            redisTemplate.delete(nextMonthKey);
        }
    }

    @Test
    void newBillingMonthShouldStartWithZeroUsage() {
        UUID tenantId = UUID.randomUUID();
        ResourceType resourceType = ResourceType.API_REQUEST;

        YearMonth currentMonth =
                YearMonth.now(ZoneOffset.UTC);

        YearMonth nextMonth =
                currentMonth.plusMonths(1);

        currentMonthKey = createRedisKey(
                tenantId,
                currentMonth,
                resourceType
        );

        nextMonthKey = createRedisKey(
                tenantId,
                nextMonth,
                resourceType
        );

        redisTemplate.opsForValue().set(
                currentMonthKey,
                "9000"
        );

        String currentUsage =
                redisTemplate.opsForValue().get(currentMonthKey);

        String nextMonthUsage =
                redisTemplate.opsForValue().get(nextMonthKey);

        assertEquals("9000", currentUsage);
        assertNull(nextMonthUsage);
        assertNotEquals(currentMonthKey, nextMonthKey);
    }

    @Test
    void usageFromDifferentMonthsShouldRemainIndependent() {
        UUID tenantId = UUID.randomUUID();
        ResourceType resourceType = ResourceType.STORAGE_MB;

        YearMonth currentMonth =
                YearMonth.now(ZoneOffset.UTC);

        YearMonth nextMonth =
                currentMonth.plusMonths(1);

        currentMonthKey = createRedisKey(
                tenantId,
                currentMonth,
                resourceType
        );

        nextMonthKey = createRedisKey(
                tenantId,
                nextMonth,
                resourceType
        );

        redisTemplate.opsForValue().set(
                currentMonthKey,
                "4500"
        );

        redisTemplate.opsForValue().set(
                nextMonthKey,
                "100"
        );

        assertEquals(
                "4500",
                redisTemplate.opsForValue().get(currentMonthKey)
        );

        assertEquals(
                "100",
                redisTemplate.opsForValue().get(nextMonthKey)
        );
    }

    private String createRedisKey(
            UUID tenantId,
            YearMonth month,
            ResourceType resourceType
    ) {
        return "quota:"
                + tenantId
                + ":"
                + month
                + ":"
                + resourceType.name();
    }
}



