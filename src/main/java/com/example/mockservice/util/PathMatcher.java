package com.example.mockservice.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for matching URL paths with path variables and query
 * parameters.
 * Supports path variables in the format ${variableName} (e.g.,
 * /api/employee/${employeeId})
 */
@Slf4j
public class PathMatcher {

    // Pattern to match path variables like ${employeeId}
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Represents a path pattern with extracted variable names and a regex for
     * matching
     */
    @Data
    public static class PathPattern {
        private final String originalPattern;
        private final List<String> variableNames;
        private final Pattern matchingRegex;

        public PathPattern(String pattern) {
            this.originalPattern = pattern;
            this.variableNames = new ArrayList<>();
            this.matchingRegex = compilePattern(pattern);
        }

        private Pattern compilePattern(String pattern) {
            // Remove query parameters if present for path matching
            String pathOnly = pattern.split("\\?")[0];

            StringBuilder regexBuilder = new StringBuilder("^");
            Matcher matcher = PATH_VARIABLE_PATTERN.matcher(pathOnly);
            int lastEnd = 0;

            while (matcher.find()) {
                // Add the literal part before this variable
                String literal = pathOnly.substring(lastEnd, matcher.start());
                regexBuilder.append(Pattern.quote(literal));

                // Add a capturing group for the variable (matches any non-slash characters)
                String variableName = matcher.group(1);
                variableNames.add(variableName);
                regexBuilder.append("([^/]+)");

                lastEnd = matcher.end();
            }

            // Add the remaining literal part
            if (lastEnd < pathOnly.length()) {
                regexBuilder.append(Pattern.quote(pathOnly.substring(lastEnd)));
            }

            regexBuilder.append("$");

            log.debug("Compiled pattern '{}' to regex: {}", pattern, regexBuilder.toString());
            return Pattern.compile(regexBuilder.toString());
        }

        /**
         * Matches the given path against this pattern and extracts path variables
         * 
         * @param actualPath The actual incoming path (without query parameters)
         * @return PathMatchResult containing whether it matched and extracted variables
         */
        public PathMatchResult match(String actualPath) {
            // Remove query parameters from actual path
            String pathOnly = actualPath.split("\\?")[0];

            Matcher matcher = matchingRegex.matcher(pathOnly);
            if (!matcher.matches()) {
                return PathMatchResult.noMatch();
            }

            Map<String, String> pathVariables = new HashMap<>();
            for (int i = 0; i < variableNames.size(); i++) {
                String variableName = variableNames.get(i);
                String value = matcher.group(i + 1);
                pathVariables.put(variableName, value);
            }

            return PathMatchResult.match(pathVariables);
        }

        /**
         * Checks if this pattern has any path variables
         */
        public boolean hasPathVariables() {
            return !variableNames.isEmpty();
        }
    }

    /**
     * Result of matching a path against a pattern
     */
    @Data
    public static class PathMatchResult {
        private final boolean matched;
        private final Map<String, String> pathVariables;

        public static PathMatchResult match(Map<String, String> pathVariables) {
            return new PathMatchResult(true, pathVariables);
        }

        public static PathMatchResult noMatch() {
            return new PathMatchResult(false, Collections.emptyMap());
        }
    }

    /**
     * Checks if a URL pattern matches an actual path
     * 
     * @param pattern    The pattern URL (may contain ${variableName} placeholders)
     * @param actualPath The actual incoming path
     * @return true if the pattern matches the path
     */
    public static boolean matches(String pattern, String actualPath) {
        if (pattern == null || actualPath == null) {
            return false;
        }

        PathPattern pathPattern = new PathPattern(pattern);
        return pathPattern.match(actualPath).isMatched();
    }

    /**
     * Extracts path variables from an actual path using a pattern
     * 
     * @param pattern    The pattern URL (may contain ${variableName} placeholders)
     * @param actualPath The actual incoming path
     * @return Map of variable names to their values, or empty map if no match
     */
    public static Map<String, String> extractPathVariables(String pattern, String actualPath) {
        if (pattern == null || actualPath == null) {
            return Collections.emptyMap();
        }

        PathPattern pathPattern = new PathPattern(pattern);
        PathMatchResult result = pathPattern.match(actualPath);
        return result.getPathVariables();
    }

    /**
     * Checks if a path pattern contains any path variables
     * 
     * @param pattern The pattern to check
     * @return true if the pattern contains ${...} variables
     */
    public static boolean hasPathVariables(String pattern) {
        if (pattern == null) {
            return false;
        }
        return PATH_VARIABLE_PATTERN.matcher(pattern).find();
    }

    /**
     * Extracts query parameters from a URL path
     * 
     * @param path Full path including query parameters
     * @return Map of query parameter names to values
     */
    public static Map<String, String> extractQueryParameters(String path) {
        Map<String, String> queryParams = new HashMap<>();

        if (path == null || !path.contains("?")) {
            return queryParams;
        }

        String queryString = path.substring(path.indexOf('?') + 1);
        String[] pairs = queryString.split("&");

        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = pair.substring(0, idx);
                String value = pair.substring(idx + 1);
                queryParams.put(key, value);
            } else {
                // Handle parameters without values
                queryParams.put(pair, "");
            }
        }

        return queryParams;
    }
}
