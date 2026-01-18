package com.example.mockservice.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Random;

@Component
public class RandomDataGenerator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    public JsonNode generateReflectedOutput(String outputParamsJson) {
        if (outputParamsJson == null || outputParamsJson.isEmpty()) {
            return objectMapper.createObjectNode();
        }

        try {
            JsonNode params = objectMapper.readTree(outputParamsJson);
            ObjectNode output = objectMapper.createObjectNode();

            if (params.isArray()) {
                for (JsonNode param : params) {
                    processParam(output, param);
                }
            }
            return output;
        } catch (Exception e) {
            return objectMapper.createObjectNode().put("error", "Failed to generate data");
        }
    }

    private void processParam(ObjectNode output, JsonNode param) {
        String name = param.get("name").asString();
        String type = param.has("type") ? param.get("type").asString() : "string";

        // Simple random generation based on type
        switch (type.toLowerCase()) {
            case "string":
                output.put(name, generateRandomString());
                break;
            case "integer":
                output.put(name, random.nextInt(100));
                break;
            case "boolean":
                output.put(name, random.nextBoolean());
                break;
            case "date":
                output.put(name, LocalDate.now().toString());
                break;
            default:
                output.put(name, "mock-value");
        }
    }

    private String generateRandomString() {
        String[] words = { "Test", "Mock", "Data", "Flowable", "Service", "Demo" };
        return words[random.nextInt(words.length)] + "-" + random.nextInt(1000);
    }
}
