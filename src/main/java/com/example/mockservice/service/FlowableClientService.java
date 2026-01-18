package com.example.mockservice.service;

import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.ServiceDefinitionRepository;
import tools.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlowableClientService {

    private final ServiceDefinitionRepository serviceDefinitionRepository;
    private final WebClient.Builder webClientBuilder;
    private final com.example.mockservice.util.RandomDataGenerator randomDataGenerator;

    @Value("${flowable.host}")
    private String flowableHost;

    @Value("${flowable.username}")
    private String flowableUsername;

    @Value("${flowable.password}")
    private String flowablePassword;

    @Transactional
    public void syncDefinitions() {
        log.info("Starting synchronization with Flowable host: {}", flowableHost);

        // Encode basic auth header
        String basicAuth = "Basic "
                + java.util.Base64.getEncoder().encodeToString((flowableUsername + ":" + flowablePassword).getBytes());

        WebClient webClient = webClientBuilder
                .baseUrl(flowableHost)
                .defaultHeader(org.springframework.http.HttpHeaders.AUTHORIZATION, basicAuth)
                .build();

        // 1. Fetch Request
        // Assuming the list endpoint returns a structure with "data": [ ... ]
        // or just a list. Based on standard Flowable, it's often a paged response.
        // For now, I'll attempt to fetch and handle the response as JsonNode to inspect
        // structure.

        try {
            JsonNode listResponse = webClient.get()
                    .uri("/service-registry-api/service-repository/service-definitions?latest=true")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(); // Blocking for simplicity in this sync task

            if (listResponse != null && listResponse.has("data")) {
                JsonNode data = listResponse.get("data");
                if (data.isArray()) {
                    for (JsonNode summary : data) {
                        System.out.println("DEBUG: Processing summary: " + summary.toString());
                        String deploymentId = summary.has("deploymentId") ? summary.get("deploymentId").asString()
                                : null;
                        String resourceName = summary.has("resourceName") ? summary.get("resourceName").asString()
                                : null;

                        if (deploymentId != null && resourceName != null) {
                            fetchAndSaveDefinition(webClient, deploymentId, resourceName);
                        } else {
                            System.out.println("DEBUG: Missing deploymentId or resourceName in summary: " + summary);
                        }
                    }
                }
            } else if (listResponse != null && listResponse.isArray()) {
                for (JsonNode summary : listResponse) {
                    System.out.println("DEBUG: Processing summary (array): " + summary.toString());
                    String deploymentId = summary.has("deploymentId") ? summary.get("deploymentId").asString() : null;
                    String resourceName = summary.has("resourceName") ? summary.get("resourceName").asString() : null;

                    if (deploymentId != null && resourceName != null) {
                        fetchAndSaveDefinition(webClient, deploymentId, resourceName);
                    } else {
                        System.out.println("DEBUG: Missing deploymentId or resourceName in summary: " + summary);
                    }
                }
            }

            log.info("Synchronization completed.");

        } catch (Exception e) {
            log.error("Failed to sync definitions", e);
            throw new RuntimeException("Sync failed", e);
        }
    }

    private void fetchAndSaveDefinition(WebClient webClient, String deploymentId, String resourceName) {
        log.info("Fetching details for deploymentId: {}, resourceName: {}", deploymentId, resourceName);
        try {
            JsonNode detail = webClient.get()
                    .uri("/service-registry-api/service-repository/deployments/{deploymentId}/resourcedata/{resourceName}",
                            deploymentId, resourceName)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (detail != null) {
                System.out.println("DEBUG: Detail response keys: " + detail.properties());
                if (detail.has("operations")) {
                    System.out.println("DEBUG: Found operations: " + detail.get("operations").size());
                } else {
                    System.out
                            .println("DEBUG: No 'operations' key found in detail response. JSON: " + detail.toString());
                }
                saveDefinition(detail);
            }
        } catch (Exception e) {
            log.error("Error fetching definition for deploymentId {} resourceName {}", deploymentId, resourceName, e);
        }
    }

    private void saveDefinition(JsonNode json) {
        String key = json.get("key").asString();
        ServiceDefinition def = serviceDefinitionRepository.findByKey(key)
                .orElse(new ServiceDefinition());

        def.setKey(key);
        if (json.has("name"))
            def.setName(json.get("name").asString());
        if (json.has("description"))
            def.setDescription(json.get("description").asString());

        // Track keys to remove orphans later
        java.util.List<String> incomingKeys = new java.util.ArrayList<>();

        if (json.has("operations") && json.get("operations").isArray()) {
            for (JsonNode opNode : json.get("operations")) {
                String opKey = opNode.get("key").asString();
                incomingKeys.add(opKey);

                ServiceOperation op = def.getOperations().stream()
                        .filter(o -> o.getKey().equals(opKey))
                        .findFirst()
                        .orElse(new ServiceOperation());

                if (op.getId() == null) {
                    op.setServiceDefinition(def);
                    def.getOperations().add(op);
                }

                op.setKey(opKey);
                if (opNode.has("name"))
                    op.setName(opNode.get("name").asString());

                // Config usually holds method and url
                if (opNode.has("config")) {
                    JsonNode config = opNode.get("config");
                    if (config.has("method"))
                        op.setMethod(config.get("method").asString());
                    if (config.has("url"))
                        op.setUrl(config.get("url").asString());
                }

                // Store inputs/outputs as JSON string
                if (opNode.has("inputParameters")) {
                    op.setInputParametersJson(opNode.get("inputParameters").toString());
                }
                if (opNode.has("outputParameters")) {
                    String outputsJson = opNode.get("outputParameters").toString();
                    op.setOutputParametersJson(outputsJson);

                    // Generate static random data for this operation
                    tools.jackson.databind.JsonNode generated = randomDataGenerator
                            .generateReflectedOutput(outputsJson);
                    op.setDefaultResponseBody(generated.toString());
                }
            }
        }

        // Remove operations that are not in the incoming list
        def.getOperations().removeIf(op -> !incomingKeys.contains(op.getKey()));

        serviceDefinitionRepository.save(def);
        log.info("Saved service definition: {}", key);
    }
}
