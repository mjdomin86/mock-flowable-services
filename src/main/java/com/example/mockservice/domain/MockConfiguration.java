package com.example.mockservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class MockConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String operationId; // Links to ServiceOperation.id

    private int httpStatus = 200;

    @Column(columnDefinition = "TEXT")
    private String customResponseBody; // If null, use random generation

    private String contentType = "application/json";
}
