package de.thfamily18.restaurant_backend.notification.mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEmailSender implements EmailSender {
    @Override
    public void send(String to, String subject, String body) {
        log.info("[MAIL][LOG] to={} subject={}\n{}", to, subject, body);
    }
}
