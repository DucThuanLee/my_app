package de.thfamily18.restaurant_backend.notification.mail;


import org.springframework.stereotype.Component;

@Component
public class SesEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        // TODO: implement Amazon SES
        throw new UnsupportedOperationException("SES sender not implemented yet");
    }
}