package de.thfamily18.restaurant_backend;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RestaurantBackendApplication {
    @Value("${app.debugConfig:NOT_FOUND}")
    String debugConfig;
    @PostConstruct
    void check() {
        System.out.println("debugConfig=" + debugConfig);
    }
    public static void main(String[] args) {
        SpringApplication.run(RestaurantBackendApplication.class, args);
    }

}
