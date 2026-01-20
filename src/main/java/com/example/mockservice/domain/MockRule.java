package com.example.mockservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.UUID; // Not strictly needed for UUID type in class, but JPA uses it. Actually, field type is String.

@Entity
@Data
public class MockRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_operation_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ServiceOperation serviceOperation;

    @Column(columnDefinition = "TEXT")
    private String conditions; // JSON string: {"inputName": "value", "amount": ">100"}

    private int responseStatus;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private int priority;
}
