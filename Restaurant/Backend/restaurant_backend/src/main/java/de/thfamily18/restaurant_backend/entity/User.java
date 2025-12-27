package de.thfamily18.restaurant_backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true)
    private String email;

    private String passwordHash;

    private LocalDateTime createdAt;
}

