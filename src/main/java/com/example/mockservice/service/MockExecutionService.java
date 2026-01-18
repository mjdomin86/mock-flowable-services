package com.example.mockservice.service;

import com.example.mockservice.domain.MockConfiguration;
import com.example.mockservice.domain.RequestLog;
import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.MockConfigurationRepository;
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
    private final RequestLogRepository requestLogRepository;
    private final RandomDataGenerator randomDataGenerator;
    private final ObjectMapper objectMapper;

    @Transactional
    public ResponseEntity<Object> executeMock(String method, String path, String body) {
        // 1. Find Operation Globally
        // Check for exact match first
        List<ServiceOperation> ops = serviceOperationRepository.findByMethodAndUrl(method, path);

        if (ops.isEmpty()) {
            // Unmatched request
            logRequest("UNKNOWN", method + " " + path, body, 404, "Operation not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Operation not found for path: " + path);
        }

        log.debug("Found {} matching operations for method: {}, path: {}", ops.size(), method, path);

        ServiceOperation selectedOp = null;
        MockConfiguration selectedConfig = null;

        // 1. Try to find a configured operation among matches
        for (ServiceOperation op : ops) {
            Optional<MockConfiguration> configOpt = mockConfigurationRepository.findByOperationId(op.getId());
            if (configOpt.isPresent()) {
                selectedOp = op;
                selectedConfig = configOpt.get();
                log.debug("Selected configured operation: {} ({})", op.getName(), op.getId());
                break;
            }
        }

        // 2. Fallback to first available if no configuration found
        if (selectedOp == null) {
            selectedOp = ops.get(0);
            log.debug("No configuration found for any matching operation. defaulting to: {} ({})", selectedOp.getName(),
                    selectedOp.getId());
        }

        ServiceOperation op = selectedOp;
        ServiceDefinition service = op.getServiceDefinition();

        log.debug("Executing mock for method: {}, path: {}", method, path);

        // 3. Apply Configuration (if any)
        int status = 200;
        Object responseBody = null;

        if (selectedConfig != null) {
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

        // 3. Generate Random Data if no custom body
        if (responseBody == null) {
            // Use stored default response body if available (Static Random Data)
            if (op.getDefaultResponseBody() != null && !op.getDefaultResponseBody().isEmpty()) {
                try {
                    responseBody = objectMapper.readTree(op.getDefaultResponseBody());
                } catch (Exception e) {
                    log.warn("Failed to parse default response body for operation {}", op.getId(), e);
                    // Fallback to generation if parsing fails
                    responseBody = randomDataGenerator.generateReflectedOutput(op.getOutputParametersJson());
                }
            } else {
                // Fallback for legacy operations without stored default body
                responseBody = randomDataGenerator.generateReflectedOutput(op.getOutputParametersJson());
            }
        }

        // 4. Log
        logRequest(service != null ? service.getName() : "UNKNOWN", op.getName() + " (" + method + " " + path + ")",
                body, status, responseBody);

        return ResponseEntity.status(status).body(responseBody);
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
            // Don't fail the request if logging fails
            log.error("Failed to save request log", e);
        }
    }
}
