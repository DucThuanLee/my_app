package de.thfamily18.restaurant_backend.notification;

// Make sure schema exists in tests
// Key goals

// SKIP LOCKED works: two concurrent transactions call lockNextReady() and they must not get the same row.

// Re-claim SENDING timeout works: a row in SENDING with processing_started_at older than timeout should be returned.

// This test uses TransactionTemplate so we can keep a transaction open to hold row locks.
public class NotificationRepositoryLockingIT {

}
