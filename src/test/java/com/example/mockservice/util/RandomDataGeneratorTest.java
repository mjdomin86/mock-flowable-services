package com.example.mockservice.util;

import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomDataGeneratorTest {

    private final RandomDataGenerator generator = new RandomDataGenerator();

    @Test
    void generateReflectedOutput_GeneratesCorrectTypes() {
        String jsonSchema = "[{\"name\":\"age\",\"type\":\"integer\"}, {\"name\":\"active\",\"type\":\"boolean\"}]";

        JsonNode result = generator.generateReflectedOutput(jsonSchema);

        assertTrue(result.has("age"));
        assertTrue(result.get("age").isInt());
        assertTrue(result.has("active"));
        assertTrue(result.get("active").isBoolean());
    }
}
