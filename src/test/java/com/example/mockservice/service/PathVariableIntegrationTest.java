package com.example.mockservice.service;

import com.example.mockservice.domain.MockRule;
import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.MockConfigurationRepository;
import com.example.mockservice.repository.MockRuleRepository;
import com.example.mockservice.repository.RequestLogRepository;
import com.example.mockservice.repository.ServiceOperationRepository;
import com.example.mockservice.util.RandomDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PathVariableIntegrationTest {

        @Mock
        private ServiceOperationRepository serviceOperationRepository;

        @Mock
        private MockConfigurationRepository mockConfigurationRepository;

        @Mock
        private MockRuleRepository mockRuleRepository;

        @Mock
        private RequestLogRepository requestLogRepository;

        @Mock
        private RandomDataGenerator randomDataGenerator;

        private ObjectMapper objectMapper;
        private MockExecutionService mockExecutionService;

        @BeforeEach
        void setUp() {
                objectMapper = new ObjectMapper();
                mockExecutionService = new MockExecutionService(
                                serviceOperationRepository,
                                mockConfigurationRepository,
                                mockRuleRepository,
                                requestLogRepository,
                                randomDataGenerator,
                                objectMapper);
        }

        @Test
        void testPathVariableMatching() throws Exception {
                // Setup: Create an operation with path variable
                ServiceDefinition service = new ServiceDefinition();
                service.setId("service-1");
                service.setName("Employee Service");

                ServiceOperation operation = new ServiceOperation();
                operation.setId("op-1");
                operation.setKey("getEmployee");
                operation.setName("Get Employee");
                operation.setMethod("GET");
                operation.setUrl("/api/employee/${employeeId}");
                operation.setServiceDefinition(service);
                operation.setOutputParametersJson(
                                "[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"name\",\"type\":\"string\"}]");

                // Mock repository responses
                when(serviceOperationRepository.findByMethodAndUrl("GET", "/api/employee/123"))
                                .thenReturn(Collections.emptyList());
                when(serviceOperationRepository.findByMethod("GET"))
                                .thenReturn(Arrays.asList(operation));
                when(mockConfigurationRepository.findByOperationId("op-1"))
                                .thenReturn(Optional.empty());
                when(mockRuleRepository.findByServiceOperationIdOrderByPriorityAsc("op-1"))
                                .thenReturn(Collections.emptyList());
                when(randomDataGenerator.generateReflectedOutput(anyString()))
                                .thenReturn(objectMapper.createObjectNode()
                                                .put("id", "123")
                                                .put("name", "John Doe"));

                // Execute
                ResponseEntity<Object> response = mockExecutionService.executeMock(
                                "GET",
                                "/api/employee/123",
                                null,
                                null);

                // Verify
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());

                // Verify that the path variable was extracted (check logs)
                verify(serviceOperationRepository).findByMethodAndUrl("GET", "/api/employee/123");
                verify(serviceOperationRepository).findByMethod("GET");
        }

        @Test
        void testMultiplePathVariables() throws Exception {
                // Setup
                ServiceDefinition service = new ServiceDefinition();
                service.setId("service-1");
                service.setName("Department Service");

                ServiceOperation operation = new ServiceOperation();
                operation.setId("op-2");
                operation.setKey("getEmployee");
                operation.setName("Get Employee in Department");
                operation.setMethod("GET");
                operation.setUrl("/api/department/${deptId}/employee/${employeeId}");
                operation.setServiceDefinition(service);
                operation.setOutputParametersJson("[]");

                when(serviceOperationRepository.findByMethodAndUrl("GET", "/api/department/HR/employee/456"))
                                .thenReturn(Collections.emptyList());
                when(serviceOperationRepository.findByMethod("GET"))
                                .thenReturn(Arrays.asList(operation));
                when(mockConfigurationRepository.findByOperationId("op-2"))
                                .thenReturn(Optional.empty());
                when(mockRuleRepository.findByServiceOperationIdOrderByPriorityAsc("op-2"))
                                .thenReturn(Collections.emptyList());
                when(randomDataGenerator.generateReflectedOutput(anyString()))
                                .thenReturn(objectMapper.createObjectNode()
                                                .put("id", "456")
                                                .put("name", "Jane Smith")
                                                .put("department", "HR"));

                // Execute
                ResponseEntity<Object> response = mockExecutionService.executeMock(
                                "GET",
                                "/api/department/HR/employee/456",
                                null,
                                null);

                // Verify
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
        }

        @Test
        void testPathVariableWithRuleMatching() throws Exception {
                // Setup
                ServiceDefinition service = new ServiceDefinition();
                service.setId("service-1");
                service.setName("Employee Service");

                ServiceOperation operation = new ServiceOperation();
                operation.setId("op-3");
                operation.setKey("getEmployee");
                operation.setName("Get Employee");
                operation.setMethod("GET");
                operation.setUrl("/api/employee/${employeeId}");
                operation.setServiceDefinition(service);
                operation.setOutputParametersJson("[]");

                // Create a rule that matches when employeeId is "999"
                MockRule rule = new MockRule();
                rule.setId("rule-1");
                rule.setServiceOperation(operation);
                rule.setConditions("{\"employeeId\":\"999\"}");
                rule.setResponseStatus(404);
                rule.setResponseBody("{\"error\":\"Employee not found\"}");
                rule.setPriority(1);

                when(serviceOperationRepository.findByMethodAndUrl("GET", "/api/employee/999"))
                                .thenReturn(Collections.emptyList());
                when(serviceOperationRepository.findByMethod("GET"))
                                .thenReturn(Arrays.asList(operation));
                when(mockConfigurationRepository.findByOperationId("op-3"))
                                .thenReturn(Optional.empty());
                when(mockRuleRepository.findByServiceOperationIdOrderByPriorityAsc("op-3"))
                                .thenReturn(Arrays.asList(rule));

                // Execute
                ResponseEntity<Object> response = mockExecutionService.executeMock(
                                "GET",
                                "/api/employee/999",
                                null,
                                null);

                // Verify - should match the rule and return 404
                assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().toString().contains("Employee not found"));
        }

        @Test
        void testQueryParametersWithPathVariable() throws Exception {
                // Setup
                ServiceDefinition service = new ServiceDefinition();
                service.setId("service-1");
                service.setName("Employee Service");

                ServiceOperation operation = new ServiceOperation();
                operation.setId("op-4");
                operation.setKey("getEmployee");
                operation.setName("Get Employee");
                operation.setMethod("GET");
                operation.setUrl("/api/employee/${employeeId}");
                operation.setServiceDefinition(service);
                operation.setOutputParametersJson("[]");

                // Create a rule that matches both path variable and query parameter
                MockRule rule = new MockRule();
                rule.setId("rule-2");
                rule.setServiceOperation(operation);
                rule.setConditions("{\"employeeId\":\"123\",\"includeDetails\":\"true\"}");
                rule.setResponseStatus(200);
                rule.setResponseBody("{\"id\":\"123\",\"name\":\"John Doe\",\"details\":{\"age\":30}}");
                rule.setPriority(1);

                when(serviceOperationRepository.findByMethodAndUrl("GET", "/api/employee/123"))
                                .thenReturn(Collections.emptyList());
                when(serviceOperationRepository.findByMethod("GET"))
                                .thenReturn(Arrays.asList(operation));
                when(mockConfigurationRepository.findByOperationId("op-4"))
                                .thenReturn(Optional.empty());
                when(mockRuleRepository.findByServiceOperationIdOrderByPriorityAsc("op-4"))
                                .thenReturn(Arrays.asList(rule));

                // Execute with query parameter
                Map<String, String[]> queryParams = new HashMap<>();
                queryParams.put("includeDetails", new String[] { "true" });

                ResponseEntity<Object> response = mockExecutionService.executeMock(
                                "GET",
                                "/api/employee/123",
                                null,
                                queryParams);

                // Verify - should match the rule with both path variable and query param
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                assertTrue(response.getBody().toString().contains("\"age\":30"));
        }

        @Test
        void testExactMatchPrioritizedOverPatternMatch() throws Exception {
                // Setup - Create two operations: one with exact path, one with variable
                ServiceDefinition service = new ServiceDefinition();
                service.setId("service-1");
                service.setName("Employee Service");

                ServiceOperation exactOp = new ServiceOperation();
                exactOp.setId("op-exact");
                exactOp.setKey("getSpecialEmployee");
                exactOp.setName("Get Special Employee");
                exactOp.setMethod("GET");
                exactOp.setUrl("/api/employee/special");
                exactOp.setServiceDefinition(service);
                exactOp.setOutputParametersJson("[]");

                ServiceOperation variableOp = new ServiceOperation();
                variableOp.setId("op-variable");
                variableOp.setKey("getEmployee");
                variableOp.setName("Get Employee");
                variableOp.setMethod("GET");
                variableOp.setUrl("/api/employee/${employeeId}");
                variableOp.setServiceDefinition(service);
                variableOp.setOutputParametersJson("[]");

                // Configure exact match to return specific operation
                when(serviceOperationRepository.findByMethodAndUrl("GET", "/api/employee/special"))
                                .thenReturn(Arrays.asList(exactOp));
                when(mockConfigurationRepository.findByOperationId("op-exact"))
                                .thenReturn(Optional.empty());
                when(mockRuleRepository.findByServiceOperationIdOrderByPriorityAsc("op-exact"))
                                .thenReturn(Collections.emptyList());
                when(randomDataGenerator.generateReflectedOutput(anyString()))
                                .thenReturn(objectMapper.createObjectNode()
                                                .put("type", "special")
                                                .put("id", "special"));

                // Execute
                ResponseEntity<Object> response = mockExecutionService.executeMock(
                                "GET",
                                "/api/employee/special",
                                null,
                                null);

                // Verify - should use exact match, not pattern match
                assertEquals(HttpStatus.OK, response.getStatusCode());
                verify(serviceOperationRepository).findByMethodAndUrl("GET", "/api/employee/special");
                // Should NOT call findByMethod since exact match was found
                verify(serviceOperationRepository, never()).findByMethod(anyString());
        }
}
