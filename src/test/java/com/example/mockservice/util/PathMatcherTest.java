package com.example.mockservice.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PathMatcherTest {

    @Test
    void testExactPathMatch() {
        String pattern = "/api/employee";
        String actualPath = "/api/employee";

        assertTrue(PathMatcher.matches(pattern, actualPath));
        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertTrue(vars.isEmpty());
    }

    @Test
    void testSinglePathVariable() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/employee/123";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals(1, vars.size());
        assertEquals("123", vars.get("employeeId"));
    }

    @Test
    void testMultiplePathVariables() {
        String pattern = "/api/department/${deptId}/employee/${employeeId}";
        String actualPath = "/api/department/HR/employee/456";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals(2, vars.size());
        assertEquals("HR", vars.get("deptId"));
        assertEquals("456", vars.get("employeeId"));
    }

    @Test
    void testPathVariableWithQueryParameters() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/employee/789?status=active&role=manager";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals(1, vars.size());
        assertEquals("789", vars.get("employeeId"));
    }

    @Test
    void testNoMatchDifferentPath() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/department/123";

        assertFalse(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertTrue(vars.isEmpty());
    }

    @Test
    void testNoMatchDifferentSegmentCount() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/employee/123/details";

        assertFalse(PathMatcher.matches(pattern, actualPath));
    }

    @Test
    void testPathVariableAtEnd() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/employee/abc-123-xyz";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals("abc-123-xyz", vars.get("employeeId"));
    }

    @Test
    void testPathVariableAtBeginning() {
        String pattern = "/${serviceKey}/employee/details";
        String actualPath = "/hr-service/employee/details";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals("hr-service", vars.get("serviceKey"));
    }

    @Test
    void testHasPathVariables() {
        assertTrue(PathMatcher.hasPathVariables("/api/employee/${employeeId}"));
        assertTrue(PathMatcher.hasPathVariables("/api/${service}/${id}"));
        assertFalse(PathMatcher.hasPathVariables("/api/employee"));
        assertFalse(PathMatcher.hasPathVariables("/api/employee/123"));
        assertFalse(PathMatcher.hasPathVariables(null));
    }

    @Test
    void testExtractQueryParameters() {
        String path = "/api/employee?status=active&role=manager&dept=HR";

        Map<String, String> params = PathMatcher.extractQueryParameters(path);
        assertEquals(3, params.size());
        assertEquals("active", params.get("status"));
        assertEquals("manager", params.get("role"));
        assertEquals("HR", params.get("dept"));
    }

    @Test
    void testExtractQueryParametersNoParams() {
        String path = "/api/employee";

        Map<String, String> params = PathMatcher.extractQueryParameters(path);
        assertTrue(params.isEmpty());
    }

    @Test
    void testExtractQueryParametersEmptyValue() {
        String path = "/api/employee?status=&role=manager";

        Map<String, String> params = PathMatcher.extractQueryParameters(path);
        assertEquals(2, params.size());
        assertEquals("", params.get("status"));
        assertEquals("manager", params.get("role"));
    }

    @Test
    void testNullPaths() {
        assertFalse(PathMatcher.matches(null, "/api/employee"));
        assertFalse(PathMatcher.matches("/api/employee", null));
        assertFalse(PathMatcher.matches(null, null));

        assertTrue(PathMatcher.extractPathVariables(null, "/api/employee").isEmpty());
        assertTrue(PathMatcher.extractPathVariables("/api/employee", null).isEmpty());
    }

    @Test
    void testComplexScenario() {
        // Test a real-world scenario with multiple path variables
        String pattern = "/api/v1/companies/${companyId}/departments/${deptId}/employees/${employeeId}";
        String actualPath = "/api/v1/companies/ACME-Corp/departments/Engineering/employees/john.doe";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals(3, vars.size());
        assertEquals("ACME-Corp", vars.get("companyId"));
        assertEquals("Engineering", vars.get("deptId"));
        assertEquals("john.doe", vars.get("employeeId"));
    }

    @Test
    void testPathPattern() {
        PathMatcher.PathPattern pattern = new PathMatcher.PathPattern("/api/employee/${employeeId}");

        assertTrue(pattern.hasPathVariables());
        assertEquals(1, pattern.getVariableNames().size());
        assertEquals("employeeId", pattern.getVariableNames().get(0));

        PathMatcher.PathMatchResult result = pattern.match("/api/employee/123");
        assertTrue(result.isMatched());
        assertEquals("123", result.getPathVariables().get("employeeId"));
    }

    @Test
    void testPathPatternNoVariables() {
        PathMatcher.PathPattern pattern = new PathMatcher.PathPattern("/api/employee");

        assertFalse(pattern.hasPathVariables());
        assertTrue(pattern.getVariableNames().isEmpty());

        PathMatcher.PathMatchResult result = pattern.match("/api/employee");
        assertTrue(result.isMatched());
        assertTrue(result.getPathVariables().isEmpty());
    }

    @Test
    void testSpecialCharactersInPath() {
        String pattern = "/api/employee/${employeeId}";
        String actualPath = "/api/employee/john.doe@example.com";

        assertTrue(PathMatcher.matches(pattern, actualPath));

        Map<String, String> vars = PathMatcher.extractPathVariables(pattern, actualPath);
        assertEquals("john.doe@example.com", vars.get("employeeId"));
    }
}
