package com.example.mockservice.web;

import com.example.mockservice.service.MockExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{serviceKey}")
@RequiredArgsConstructor
@Slf4j
public class DynamicMockController {

    private final MockExecutionService mockExecutionService;

    @RequestMapping(value = "/**", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
            RequestMethod.DELETE, RequestMethod.PATCH })
    public ResponseEntity<Object> handleMockRequest(HttpServletRequest request) throws IOException {

        String method = request.getMethod();
        String fullPath = request.getRequestURI();

        // Use fullPath directly as it should match the operation URL (e.g. /data)
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

        // Extract query parameters for rule matching
        java.util.Map<String, String[]> queryParams = request.getParameterMap();

        log.debug("Received mock request for method: {}, path: {}, params: {}", method, fullPath, queryParams);

        return mockExecutionService.executeMock(method, fullPath, body, queryParams);
    }
}
