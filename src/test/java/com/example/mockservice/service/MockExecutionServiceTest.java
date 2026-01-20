package com.example.mockservice.service;

import com.example.mockservice.domain.MockConfiguration;
import com.example.mockservice.domain.RequestLog;
import com.example.mockservice.domain.ServiceDefinition;
import com.example.mockservice.domain.ServiceOperation;
import com.example.mockservice.repository.MockConfigurationRepository;
import com.example.mockservice.repository.RequestLogRepository;
import com.example.mockservice.repository.ServiceDefinitionRepository;
import com.example.mockservice.repository.ServiceOperationRepository;
import com.example.mockservice.util.RandomDataGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MockExecutionServiceTest {

    @Mock
    private ServiceDefinitionRepository serviceDefinitionRepository;
    @Mock
    private ServiceOperationRepository serviceOperationRepository;
    @Mock
    private MockConfigurationRepository mockConfigurationRepository;
    @Mock
    private RequestLogRepository requestLogRepository;
    @Mock
    private RandomDataGenerator randomDataGenerator;
    @org.mockito.Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MockExecutionService mockExecutionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Using the real service with mocks
        // MockExecutionService(ServiceOperationRepository, MockConfigurationRepository,
        // MockRuleRepository, RequestLogRepository, RandomDataGenerator, ObjectMapper)
        mockExecutionService = new MockExecutionService(
                serviceOperationRepository,
                mockConfigurationRepository,
                org.mockito.Mockito.mock(com.example.mockservice.repository.MockRuleRepository.class),
                requestLogRepository,
                randomDataGenerator,
                objectMapper);
    }

    @Test
    void executeMock_OperationFound_ReturnsRandomData() {
        ServiceDefinition def = new ServiceDefinition();
        def.setKey("service1");
        def.setName("Service 1");

        ServiceOperation op = new ServiceOperation();
        op.setId("op1");
        op.setKey("op1");
        op.setMethod("GET");
        op.setUrl("/test");
        op.setOutputParametersJson("[]");
        op.setServiceDefinition(def);

        // Assuming ServiceDefinition's operations list is initialized or can be added
        // to
        // For a test, it's often simpler to set it directly if the getter/setter
        // allows.
        // If def.getOperations() returns null, it needs to be initialized first.
        // For robustness, ensure def.getOperations() returns a mutable list.
        if (def.getOperations() == null) {
            def.setOperations(new java.util.ArrayList<>());
        }
        def.getOperations().add(op);

        when(serviceOperationRepository.findByMethodAndUrl("GET", "/test"))
                .thenReturn(List.of(op));

        when(mockConfigurationRepository.findByOperationId("op1")).thenReturn(Optional.empty());
        when(randomDataGenerator.generateReflectedOutput(any()))
                .thenReturn(tools.jackson.databind.node.JsonNodeFactory.instance.objectNode());

        ResponseEntity<Object> response = mockExecutionService.executeMock("GET", "/test", null, null);

        assertEquals(200, response.getStatusCode().value());
        verify(requestLogRepository).save(any(RequestLog.class));
    }

    @Test
    void executeMock_OperationNotFound_Returns404() {
        when(serviceOperationRepository.findByMethodAndUrl(any(), any())).thenReturn(List.of());

        ResponseEntity<Object> response = mockExecutionService.executeMock("GET", "/unknown", null, null);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void executeMock_ConfigurationOverride_ReturnsCustomResponse() throws Exception {
        ServiceDefinition def = new ServiceDefinition();
        def.setName("Service 1");

        ServiceOperation op = new ServiceOperation();
        op.setId("op1");
        op.setMethod("POST");
        op.setUrl("/api/data");
        op.setServiceDefinition(def);

        when(serviceOperationRepository.findByMethodAndUrl("POST", "/api/data"))
                .thenReturn(List.of(op));

        MockConfiguration config = new MockConfiguration();
        config.setHttpStatus(201);
        config.setCustomResponseBody("{\"status\":\"created\"}");

        when(mockConfigurationRepository.findByOperationId("op1")).thenReturn(Optional.of(config));

        // Spy uses real ObjectMapper, so readTree works automatically on valid JSON
        JsonNode mockJson = objectMapper.readTree("{\"status\":\"created\"}");

        ResponseEntity<Object> response = mockExecutionService.executeMock("POST", "/api/data", "{}", null);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(mockJson, response.getBody());
    }
}
