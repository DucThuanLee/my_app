package de.thfamily18.restaurant_backend.notification.mail;

public interface EmailSender {
    void send(String to, String subject, String body);
}
