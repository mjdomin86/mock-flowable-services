# Path Variables and Query Parameters Support

This document explains how the mock service handles path variables and query parameters for dynamic URL matching.

## Path Variables

Path variables allow you to define dynamic URL patterns where parts of the URL can vary. This is useful for RESTful APIs that use IDs or other identifiers in the URL path.

### Syntax

Path variables are defined using the `${variableName}` syntax in the operation URL. For example:

```
/api/employee/${employeeId}
```

### Examples

#### Single Path Variable

**Operation URL**: `/api/employee/${employeeId}`

**Matching Requests**:
- `/api/employee/123` → `employeeId = "123"`
- `/api/employee/john.doe` → `employeeId = "john.doe"`
- `/api/employee/abc-xyz-999` → `employeeId = "abc-xyz-999"`

**Non-Matching Requests**:
- `/api/employee` (missing the employee ID)
- `/api/employee/123/details` (too many path segments)

#### Multiple Path Variables

**Operation URL**: `/api/department/${deptId}/employee/${employeeId}`

**Matching Request**: `/api/department/HR/employee/456`
- Extracted variables:
  - `deptId = "HR"`
  - `employeeId = "456"`

#### Path Variable at Different Positions

**Operation URL**: `/${serviceKey}/employee/details`

**Matching Request**: `/hr-service/employee/details`
- Extracted variable: `serviceKey = "hr-service"`

## Query Parameters

Query parameters are automatically extracted from the URL and can be used in mock rules for conditional responses.

### Examples

**Request**: `/api/employees?status=active&department=HR`

**Extracted Parameters**:
- `status = "active"`
- `department = "HR"`

## Combining Path Variables and Query Parameters

You can use both path variables and query parameters together. Both will be available for rule matching.

**Operation URL**: `/api/employee/${employeeId}`

**Request**: `/api/employee/123?includeDetails=true`

**Available for Rule Matching**:
- `employeeId = "123"` (from path variable)
- `includeDetails = "true"` (from query parameter)

### Example Rule Configuration

```json
{
  "conditions": {
    "employeeId": "999",
    "includeDetails": "true"
  },
  "responseStatus": 404,
  "responseBody": "{\"error\": \"Employee not found\"}"
}
```

This rule will match when:
1. The employee ID in the path is "999" AND
2. The `includeDetails` query parameter is "true"

## How Matching Works

The mock service uses a two-step matching process:

### 1. Exact Match (First Priority)

The service first tries to find an exact match for the URL path. This is faster and takes priority over pattern matching.

**Example**:
- Operation URL: `/api/employee/special`
- Request: `/api/employee/special` → Matches exactly

### 2. Pattern Match (Fallback)

If no exact match is found, the service searches for operations with path variables that match the request path.

**Example**:
- Operation URL: `/api/employee/${employeeId}`
- Request: `/api/employee/123` → Matches via pattern

### Example: Exact vs Pattern Match

If you have two operations:
1. `/api/employee/special` (exact path)
2. `/api/employee/${employeeId}` (pattern with variable)

A request to `/api/employee/special` will **always** match operation #1 (exact match takes priority).

A request to `/api/employee/123` will match operation #2 (pattern match).

## Rule Matching with Path Variables

When you create mock rules, the path variables are automatically included in the request data that rules can match against.

### For GET/DELETE Requests (No Body)

The rule matching data includes:
- All path variables
- All query parameters

### For POST/PUT/PATCH Requests (With Body)

The rule matching data includes:
- All fields from the request body
- All path variables (merged into the data)

### Example: Rule Matching Different Employees

**Operation**: `GET /api/employee/${employeeId}`

**Rule 1**: Return employee details for ID "123"
```json
{
  "conditions": {
    "employeeId": "123"
  },
  "responseStatus": 200,
  "responseBody": "{\"id\": \"123\", \"name\": \"John Doe\"}"
}
```

**Rule 2**: Return 404 for ID "999"
```json
{
  "conditions": {
    "employeeId": "999"
  },
  "responseStatus": 404,
  "responseBody": "{\"error\": \"Employee not found\"}"
}
```

**Requests**:
- `GET /api/employee/123` → Returns John Doe (Rule 1)
- `GET /api/employee/999` → Returns 404 (Rule 2)
- `GET /api/employee/456` → Returns default/random response (no rule matches)

## Best Practices

1. **Use exact paths for special cases**: If you have a special endpoint like `/api/employee/special`, define it as an exact path rather than relying on pattern matching.

2. **Name variables descriptively**: Use clear names like `${employeeId}`, `${departmentId}`, etc. rather than generic names like `${id}`.

3. **Keep path variables in the middle or end**: While path variables can appear at any position, it's most common to have them in the middle or at the end of the path for clarity.

4. **Use rules for dynamic behavior**: Instead of creating many similar operations, use one operation with path variables and define rules for different behaviors.

5. **Query parameters are case-sensitive**: Make sure your rule conditions use the exact same case as the query parameter names.

## Technical Details

### Path Variable Matching Algorithm

1. Extract the pattern from the operation URL
2. Convert `${variableName}` placeholders to regex capture groups
3. Match the incoming path against the regex
4. Extract values from capture groups and map them to variable names

### Supported Characters in Path Variables

Path variables can contain any characters except forward slash (`/`). This includes:
- Alphanumeric characters: `a-z`, `A-Z`, `0-9`
- Hyphens and underscores: `-`, `_`
- Dots: `.`
- Special characters: `@`, etc.

### Performance Considerations

- **Exact matches** are very fast (database index lookup)
- **Pattern matches** require iterating through all operations for the HTTP method
- For better performance, minimize the number of operations with the same HTTP method

## Troubleshooting

### Path Variable Not Matching

**Problem**: Request is not matching an operation with path variables

**Solutions**:
1. Check that the number of path segments matches
   - Pattern: `/api/employee/${id}` requires exactly 3 segments
   - Request: `/api/employee/123` ✓
   - Request: `/api/employee/123/details` ✗ (too many segments)

2. Verify there are no typos in the base path
   - Pattern: `/api/employe/${id}` 
   - Request: `/api/employee/123` ✗ (typo in "employe")

### Rule Not Matching with Path Variables

**Problem**: Rule conditions include path variable but don't match

**Solutions**:
1. Check the variable name matches exactly
   - Pattern: `${employeeId}`
   - Rule condition: `"employeeId": "123"` ✓
   - Rule condition: `"employee_id": "123"` ✗ (different name)

2. Ensure the value is a string in the condition
   - Rule condition: `"employeeId": "123"` ✓
   - Rule condition: `"employeeId": 123` ✗ (number, not string)

3. Check the logs to see what values were extracted
   - Look for: `Extracted path variables: {employeeId=123}`
