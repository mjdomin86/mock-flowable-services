package com.example.mockservice.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class ServiceOperation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "`key`")
    private String key;
    private String name;
    private String method; // GET, POST, etc.
    private String url; // /data, /create, etc.

    @Column(columnDefinition = "TEXT")
    private String inputParametersJson; // Storing as JSON string for simplicity

    @Column(columnDefinition = "TEXT")
    private String outputParametersJson; // Storing as JSON string for simplicity

    @Column(columnDefinition = "TEXT")
    private String defaultResponseBody; // Generated at sync time for consistency

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_definition_id")
    @lombok.ToString.Exclude
    private ServiceDefinition serviceDefinition;

    @OneToMany(mappedBy = "serviceOperation", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<MockRule> rules = new java.util.ArrayList<>();

    @Transient
    private static final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

    public tools.jackson.databind.JsonNode getInputs() {
        if (inputParametersJson == null || inputParametersJson.isEmpty())
            return mapper.createArrayNode();
        try {
            return mapper.readTree(inputParametersJson);
        } catch (Exception e) {
            return mapper.createArrayNode();
        }
    }

    public tools.jackson.databind.JsonNode getOutputs() {
        if (outputParametersJson == null || outputParametersJson.isEmpty())
            return mapper.createArrayNode();
        try {
            return mapper.readTree(outputParametersJson);
        } catch (Exception e) {
            return mapper.createArrayNode();
        }
    }
}
