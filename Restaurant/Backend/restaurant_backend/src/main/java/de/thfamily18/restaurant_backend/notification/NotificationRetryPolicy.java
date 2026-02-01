package de.thfamily18.restaurant_backend.notification;

public class NotificationRetryPolicy {
    //    private long backoffSeconds(int attempts) {
//        return switch (attempts) {
//            case 1 -> 10;
//            case 2 -> 30;
//            case 3 -> 120;
//            default -> 600;
//        };
//    }

    public static long backoffSeconds(int attempts) {
        // base is the initial waiting time (e.g., 10 seconds)
        long base = 10;

        // Formula: 10 * 2^(attempts - 1)
        // First attempt: 10 * 2^0 = 10s
        // 2: 10 * 2^1 = 20s
        // 3: 10 * 2^2 = 40s
        // 4: 10 * 2^3 = 80s
        double waitTime = base * Math.pow(2, attempts - 1);

        // Limit the maximum waiting time (e.g., no more than 1 hour) to avoid infinite waiting
        long maxWait = 3600;
        return Math.min((long) waitTime, maxWait);
    }

    public static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
