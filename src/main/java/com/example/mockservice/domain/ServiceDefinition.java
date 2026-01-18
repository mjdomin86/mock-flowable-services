package com.example.mockservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class ServiceDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "`key`", unique = true)
    private String key;

    private String name;

    @Column(length = 2048)
    private String description;

    @OneToMany(mappedBy = "serviceDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceOperation> operations = new ArrayList<>();
}
