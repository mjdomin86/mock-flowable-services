package com.example.mockservice.service;

import com.example.mockservice.domain.MockConfiguration;
import com.example.mockservice.domain.RequestLog;
import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.MockConfigurationRepository;
import com.example.mockservice.repository.MockRuleRepository;
import com.example.mockservice.repository.RequestLogRepository;
import com.example.mockservice.repository.ServiceOperationRepository;
import com.example.mockservice.util.RandomDataGenerator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockExecutionService {

    private final ServiceOperationRepository serviceOperationRepository;
    private final MockConfigurationRepository mockConfigurationRepository;
    private final MockRuleRepository mockRuleRepository;
    private final RequestLogRepository requestLogRepository;
    private final RandomDataGenerator randomDataGenerator;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResponseEntity<Object> executeMock(String method, String path, String body,
            java.util.Map<String, String[]> queryParams) {
        // 1. Find Operation - Try exact match first, then pattern matching
        List<ServiceOperation> ops = serviceOperationRepository.findByMethodAndUrl(method, path);

        // If no exact match, try pattern matching for path variables
        if (ops.isEmpty()) {
            log.debug("No exact match for path: {}, trying pattern matching", path);
            List<ServiceOperation> allOpsForMethod = serviceOperationRepository.findByMethod(method);

            // Create a mutable list for pattern matching results
            ops = new java.util.ArrayList<>();
            for (ServiceOperation op : allOpsForMethod) {
                if (com.example.mockservice.util.PathMatcher.matches(op.getUrl(), path)) {
                    ops.add(op);
                    log.debug("Pattern matched: {} -> {}", op.getUrl(), path);
                }
            }
        }

        if (ops.isEmpty()) {
            logRequest("UNKNOWN", method + " " + path, body, 404, "Operation not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Operation not found for path: " + path);
        }

        log.debug("Found {} matching operations for method: {}, path: {}", ops.size(), method, path);

        ServiceOperation selectedOp = null;
        MockConfiguration selectedConfig = null;

        // 2. Try to find a configured operation among matches
        for (ServiceOperation op : ops) {
            Optional<MockConfiguration> configOpt = mockConfigurationRepository.findByOperationId(op.getId());
            if (configOpt.isPresent()) {
                selectedOp = op;
                selectedConfig = configOpt.get();
                log.debug("Selected configured operation: {} ({})", op.getName(), op.getId());
                break;
            }
        }

        // 3. Fallback to first available if no configuration found
        if (selectedOp == null) {
            selectedOp = ops.get(0);
            log.debug("No configuration found for any matching operation. defaulting to: {} ({})", selectedOp.getName(),
                    selectedOp.getId());
        }

        ServiceOperation op = selectedOp;
        ServiceDefinition service = op.getServiceDefinition();

        log.debug("Executing mock for method: {}, path: {}", method, path);

        // Extract path variables if the operation URL contains them
        java.util.Map<String, String> pathVariables = com.example.mockservice.util.PathMatcher
                .extractPathVariables(op.getUrl(), path);
        if (!pathVariables.isEmpty()) {
            log.debug("Extracted path variables: {}", pathVariables);
        }

        int status = 200;
        Object responseBody = null;

        // 3. Check for Rule-Based Overrides
        List<com.example.mockservice.domain.MockRule> rules = mockRuleRepository
                .findByServiceOperationIdOrderByPriorityAsc(op.getId());

        if (!rules.isEmpty()) {
            JsonNode requestData = null;

            // Try to parse body first (for POST/PUT/PATCH)
            if (body != null && !body.isEmpty()) {
                try {
                    requestData = objectMapper.readTree(body);
                } catch (Exception e) {
                    log.warn("Failed to parse request body for rule matching", e);
                }
            }

            // If no body, create JSON from query parameters and path variables
            if (requestData == null) {
                try {
                    java.util.Map<String, String> combinedParams = new java.util.HashMap<>();

                    // Add query parameters
                    if (queryParams != null && !queryParams.isEmpty()) {
                        for (java.util.Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                            // Take first value if multiple values exist
                            if (entry.getValue() != null && entry.getValue().length > 0) {
                                combinedParams.put(entry.getKey(), entry.getValue()[0]);
                            }
                        }
                    }

                    // Add path variables
                    if (!pathVariables.isEmpty()) {
                        combinedParams.putAll(pathVariables);
                    }

                    if (!combinedParams.isEmpty()) {
                        requestData = objectMapper.valueToTree(combinedParams);
                    }
                } catch (Exception e) {
                    log.warn("Failed to convert query params and path variables for rule matching", e);
                }
            } else {
                // If we have body data, add path variables to it for rule matching
                if (!pathVariables.isEmpty()) {
                    try {
                        java.util.Map<String, Object> requestDataMap = objectMapper.convertValue(requestData,
                                new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                });
                        requestDataMap.putAll(pathVariables);
                        requestData = objectMapper.valueToTree(requestDataMap);
                    } catch (Exception e) {
                        log.warn("Failed to merge path variables with request body", e);
                    }
                }
            }

            // Match rules against request data
            if (requestData != null) {
                for (com.example.mockservice.domain.MockRule rule : rules) {
                    if (isRuleMatch(rule, requestData)) {
                        log.debug("Matched rule: {} (priority={})", rule.getId(), rule.getPriority());
                        status = rule.getResponseStatus();
                        if (rule.getResponseBody() != null && !rule.getResponseBody().isEmpty()) {
                            try {
                                responseBody = objectMapper.readTree(rule.getResponseBody());
                            } catch (Exception e) {
                                responseBody = rule.getResponseBody();
                            }
                        }
                        selectedConfig = null; // Rule takes precedence
                        break;
                    }
                }
            }
        }

        // 4. Apply Static Configuration (if no rule matched)
        if (responseBody == null && selectedConfig != null) {
            MockConfiguration config = selectedConfig;
            log.debug("Applying config: status={}, body={}", config.getHttpStatus(), config.getCustomResponseBody());
            status = config.getHttpStatus();
            if (config.getCustomResponseBody() != null && !config.getCustomResponseBody().isEmpty()) {
                try {
                    responseBody = objectMapper.readTree(config.getCustomResponseBody());
                } catch (Exception e) {
                    responseBody = config.getCustomResponseBody();
                }
            }
        }

        // 5. Generate Random Data if no custom body
        if (responseBody == null) {
            if (op.getDefaultResponseBody() != null && !op.getDefaultResponseBody().isEmpty()) {
                try {
                    responseBody = objectMapper.readTree(op.getDefaultResponseBody());
                } catch (Exception e) {
                    log.warn("Failed to parse default response body for operation {}", op.getId(), e);
                    responseBody = randomDataGenerator.generateReflectedOutput(op.getOutputParametersJson());
                }
            } else {
                responseBody = randomDataGenerator.generateReflectedOutput(op.getOutputParametersJson());
            }
        }

        // 6. Log
        logRequest(service != null ? service.getName() : "UNKNOWN", op.getName() + " (" + method + " " + path + ")",
                body, status, responseBody);

        return ResponseEntity.status(status).body(responseBody);
    }

    private boolean isRuleMatch(com.example.mockservice.domain.MockRule rule, JsonNode requestJson) {
        try {
            if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
                return true;
            }

            // Use Map conversion to avoid JsonNode iteration API issues
            java.util.Map<String, Object> conditions = objectMapper.convertValue(
                    objectMapper.readTree(rule.getConditions()),
                    new tools.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                    });

            for (java.util.Map.Entry<String, Object> entry : conditions.entrySet()) {
                String key = entry.getKey();
                // Simple equality check for now
                String expectedValue = String.valueOf(entry.getValue());

                if (!requestJson.has(key))
                    return false;

                // Safe string conversion
                JsonNode actualNode = requestJson.get(key);
                String actualValue = actualNode.isValueNode() ? actualNode.asString() : actualNode.toString();

                if (!actualValue.equals(expectedValue))
                    return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error evaluating rule conditions", e);
        }
        return false;
    }

    private void logRequest(String serviceName, String operationName, String requestBody, int status,
            Object responseBody) {
        try {
            RequestLog log = new RequestLog();
            log.setServiceName(serviceName);
            log.setOperationName(operationName);
            log.setRequestBody(requestBody);
            log.setResponseStatus(status);

            if (responseBody instanceof JsonNode) {
                log.setResponseBody(responseBody.toString());
            } else if (responseBody != null) {
                log.setResponseBody(responseBody.toString());
            }

            requestLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to save request log", e);
        }
    }

    @Transactional
    public void addRule(String operationId, com.example.mockservice.domain.MockRule rule) {
        ServiceOperation op = serviceOperationRepository.findById(operationId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid operation Id: " + operationId));

        // Create a new rule entity to avoid detached entity issues
        com.example.mockservice.domain.MockRule newRule = new com.example.mockservice.domain.MockRule();
        newRule.setServiceOperation(op);
        newRule.setConditions(rule.getConditions());
        newRule.setResponseStatus(rule.getResponseStatus());
        newRule.setResponseBody(rule.getResponseBody());
        newRule.setPriority(rule.getPriority() == 0 ? 10 : rule.getPriority());

        // Save the new rule
        mockRuleRepository.save(newRule);
    }

    @Transactional
    public String deleteRule(String ruleId) {
        com.example.mockservice.domain.MockRule rule = mockRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid rule Id: " + ruleId));

        String serviceId = rule.getServiceOperation().getServiceDefinition().getId();

        // Delete rule directly
        mockRuleRepository.delete(rule);

        return serviceId;
    }
}
