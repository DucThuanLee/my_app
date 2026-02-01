package de.thfamily18.restaurant_backend.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NotificationRetryPolicyUnitTest {
    @Test
    void backoffSeconds_shouldFollowExpectedSchedule() {
        // Base exponential backoff: 10 * 2^(attempts - 1)
        assertEquals(10, NotificationRetryPolicy.backoffSeconds(1));
        assertEquals(20, NotificationRetryPolicy.backoffSeconds(2));
        assertEquals(40, NotificationRetryPolicy.backoffSeconds(3));
        assertEquals(80, NotificationRetryPolicy.backoffSeconds(4));
        assertEquals(160, NotificationRetryPolicy.backoffSeconds(5));

        // Cap at 3600 seconds (1 hour)
        // attempts=9 -> 10 * 2^8 = 2560
        assertEquals(2560, NotificationRetryPolicy.backoffSeconds(9));

        // attempts=10 -> 10 * 2^9 = 5120 -> capped to 3600
        assertEquals(3600, NotificationRetryPolicy.backoffSeconds(10));

        // Large attempts must still be capped
        assertEquals(3600, NotificationRetryPolicy.backoffSeconds(100));
    }

    @Test
    void trim_shouldReturnNullWhenInputIsNull() {
        assertNull(NotificationRetryPolicy.trim(null, 10));
    }

    @Test
    void trim_shouldReturnSameStringWhenWithinLimit() {
        assertEquals("hello", NotificationRetryPolicy.trim("hello", 10));
        assertEquals("", NotificationRetryPolicy.trim("", 10));
    }

    @Test
    void trim_shouldCutStringWhenExceedsLimit() {
        assertEquals("hel", NotificationRetryPolicy.trim("hello", 3));
    }

    @Test
    void trim_withZeroMax_shouldReturnEmptyStringWhenNonNull() {
        assertEquals("", NotificationRetryPolicy.trim("hello", 0));
    }
}
