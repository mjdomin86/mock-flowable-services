# How to Create Mock Rules

Mock rules allow you to define conditional responses based on request data (path variables, query parameters, or request body). Rules are evaluated in priority order, and the first matching rule wins.

## Table of Contents
- [Rule Structure](#rule-structure)
- [Creating Rules via Web UI](#creating-rules-via-web-ui)
- [Creating Rules via API](#creating-rules-via-api)
- [Rule Conditions](#rule-conditions)
- [Examples](#examples)
- [Best Practices](#best-practices)

---

## Rule Structure

A mock rule consists of the following fields:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `conditions` | String (JSON) | Yes | JSON object defining matching conditions |
| `responseStatus` | Integer | Yes | HTTP status code to return (e.g., 200, 404, 500) |
| `responseBody` | String | No | Response body to return (can be JSON, plain text, etc.) |
| `priority` | Integer | No | Priority order (lower = higher priority). Default: 10 |

### Example Rule Object
```json
{
  "conditions": "{\"employeeId\":\"999\"}",
  "responseStatus": 404,
  "responseBody": "{\"error\":\"Employee not found\"}",
  "priority": 1
}
```

---

## Creating Rules via Web UI

### Step 1: Navigate to Services
1. Go to the admin interface (typically at `http://localhost:8080/`)
2. You'll see a list of all synced services

### Step 2: Select a Service
1. Click on the service that contains the operation you want to configure
2. You'll see all operations for that service

### Step 3: Add a Rule
1. Find the operation you want to add a rule to
2. Look for the "Add Rule" form (usually below the operation details)
3. Fill in the rule details:
   - **Conditions**: JSON object with field-value pairs
   - **Response Status**: HTTP status code (e.g., 200, 404, 500)
   - **Response Body**: The response to return
   - **Priority**: Lower numbers = higher priority (optional, defaults to 10)
4. Click "Add Rule"

### Example: Creating a Rule in the Web UI

For an operation `GET /api/employee/${employeeId}`:

**Form Values:**
- Conditions: `{"employeeId":"999"}`
- Response Status: `404`
- Response Body: `{"error":"Employee not found"}`
- Priority: `1`

---

## Creating Rules via API

Currently, rules can be created through the web interface. If you need a REST API, you can use the following approach:

### Direct Database Insertion (Not Recommended)

You can import rules via the import/export functionality:

#### Export Current Rules
```bash
curl -o rules-backup.json http://localhost:8080/export
```

#### Import Rules
```bash
curl -X POST http://localhost:8080/import \
  -F "file=@rules-backup.json"
```

### Programmatic Creation (Recommended)

Use the `MockExecutionService.addRule()` method:

```java
@Autowired
private MockExecutionService mockExecutionService;

public void createRule() {
    MockRule rule = new MockRule();
    rule.setConditions("{\"employeeId\":\"999\"}");
    rule.setResponseStatus(404);
    rule.setResponseBody("{\"error\":\"Employee not found\"}");
    rule.setPriority(1);
    
    mockExecutionService.addRule("operation-id", rule);
}
```

---

## Rule Conditions

Conditions are specified as a JSON object where each key-value pair represents a field that must match.

### Condition Matching

- **Exact Match**: Values must match exactly (case-sensitive)
- **All Conditions Must Match**: AND logic (all fields must match)
- **String Comparison**: All values are compared as strings

### What Can Be Matched?

Depending on the HTTP method and request type, different data is available for matching:

#### GET/DELETE Requests (No Body)
- Path variables
- Query parameters

#### POST/PUT/PATCH Requests (With Body)
- Request body fields
- Path variables (merged into the data)

### Examples of Conditions

#### Match Path Variable
```json
{
  "employeeId": "123"
}
```

#### Match Query Parameter
```json
{
  "status": "active"
}
```

#### Match Multiple Conditions (AND)
```json
{
  "employeeId": "999",
  "department": "HR"
}
```

#### Match Request Body Field
For `POST /api/employee` with body `{"name":"John","age":30}`:
```json
{
  "name": "John",
  "age": "30"
}
```

#### Match Path Variable + Query Parameter
For `GET /api/employee/123?includeDetails=true`:
```json
{
  "employeeId": "123",
  "includeDetails": "true"
}
```

---

## Examples

### Example 1: Return 404 for Specific Employee ID

**Operation**: `GET /api/employee/${employeeId}`

**Rule**:
```json
{
  "conditions": "{\"employeeId\":\"999\"}",
  "responseStatus": 404,
  "responseBody": "{\"error\":\"Employee not found\"}",
  "priority": 1
}
```

**Requests**:
- `GET /api/employee/999` → **404** with error message
- `GET /api/employee/123` → Default response (no rule match)

---

### Example 2: Return Different Data Based on Department

**Operation**: `GET /api/employees`

**Rule 1** (High Priority):
```json
{
  "conditions": "{\"department\":\"HR\"}",
  "responseStatus": 200,
  "responseBody": "[{\"id\":\"1\",\"name\":\"Alice\",\"dept\":\"HR\"},{\"id\":\"2\",\"name\":\"Bob\",\"dept\":\"HR\"}]",
  "priority": 1
}
```

**Rule 2** (Lower Priority):
```json
{
  "conditions": "{\"department\":\"IT\"}",
  "responseStatus": 200,
  "responseBody": "[{\"id\":\"3\",\"name\":\"Charlie\",\"dept\":\"IT\"}]",
  "priority": 2
}
```

**Requests**:
- `GET /api/employees?department=HR` → Returns HR employees
- `GET /api/employees?department=IT` → Returns IT employees
- `GET /api/employees?department=Sales` → Default response

---

### Example 3: Simulate Server Error

**Operation**: `POST /api/employee`

**Rule**:
```json
{
  "conditions": "{\"name\":\"ERROR_TEST\"}",
  "responseStatus": 500,
  "responseBody": "{\"error\":\"Internal server error\",\"code\":\"ERR_500\"}",
  "priority": 1
}
```

**Request**:
```bash
POST /api/employee
Content-Type: application/json

{
  "name": "ERROR_TEST",
  "age": 30
}
```

**Response**: **500** with error message

---

### Example 4: Conditional Response Based on Multiple Fields

**Operation**: `GET /api/employee/${employeeId}`

**Rule**:
```json
{
  "conditions": "{\"employeeId\":\"123\",\"includeDetails\":\"true\"}",
  "responseStatus": 200,
  "responseBody": "{\"id\":\"123\",\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\",\"salary\":75000}",
  "priority": 1
}
```

**Requests**:
- `GET /api/employee/123?includeDetails=true` → **200** with full details
- `GET /api/employee/123` → Default response (no `includeDetails` param)
- `GET /api/employee/456?includeDetails=true` → Default response (different ID)

---

### Example 5: Simulate Authentication Failure

**Operation**: `POST /api/login`

**Rule**:
```json
{
  "conditions": "{\"username\":\"test\",\"password\":\"wrong\"}",
  "responseStatus": 401,
  "responseBody": "{\"error\":\"Invalid credentials\"}",
  "priority": 1
}
```

---

## Best Practices

### 1. Use Priority Wisely
- **Lower numbers = Higher priority**
- Set specific rules to priority 1-5
- Set generic/fallback rules to priority 10+

### 2. Keep Conditions Simple
- Use exact matches for reliability
- Avoid complex nested conditions (not currently supported)
- Remember: All conditions must match (AND logic)

### 3. Test Your Rules
After creating a rule, test it immediately:
```bash
curl -X GET "http://localhost:8080/employee-service/api/employee/999"
```

Check the logs (`http://localhost:8080/logs`) to see if the rule matched.

### 4. Use Meaningful Response Bodies
Include helpful information in your responses:
```json
{
  "error": "Employee not found",
  "employeeId": "999",
  "timestamp": "2024-01-27T10:00:00Z"
}
```

### 5. Document Your Rules
Keep track of which rules you've created and why. Consider adding comments in your test scenarios.

### 6. Rule Order Matters
If multiple rules match, the one with the lowest priority number wins. Make sure your most specific rules have the highest priority (lowest number).

### 7. Use String Values in Conditions
Even for numbers and booleans, use string values in conditions:
- ✅ `"age": "30"`
- ❌ `"age": 30`
- ✅ `"isActive": "true"`
- ❌ `"isActive": true`

---

## Troubleshooting

### Rule Not Matching

**Problem**: You created a rule but it's not being triggered

**Solutions**:

1. **Check the conditions syntax**
   - Must be valid JSON
   - Keys must match exactly (case-sensitive)
   - Values must be strings

2. **Verify the operation ID**
   - Rule must be associated with the correct operation
   - Check in the database or UI that the rule exists

3. **Check the logs**
   - Navigate to `/logs` in the admin UI
   - Look for debug messages about rule matching

4. **Check priority**
   - Another rule with higher priority (lower number) might be matching first

5. **Check the request data**
   - For GET requests: path variables and query params
   - For POST requests: body fields + path variables
   - Add logging to see what data is being matched

Example log output:
```
Extracted path variables: {employeeId=123}
Matched rule: rule-id-123 (priority=1)
```

### Multiple Rules Matching

**Problem**: Multiple rules seem to apply

**Solution**: Only the first matching rule (lowest priority number) is used. Adjust priorities:
- Most specific rule: priority 1
- Less specific rule: priority 5
- Fallback rule: priority 10

---

## Advanced: Deleting Rules

### Via Web UI
Navigate to the service page and click "Delete" next to the rule you want to remove.

### Via Code
```java
mockExecutionService.deleteRule("rule-id");
```

---

## Summary

Creating a rule involves:
1. Identifying the operation
2. Defining the conditions (JSON)
3. Setting the response status and body
4. Optionally setting priority
5. Testing the rule

Rules are powerful for:
- Simulating error conditions
- Returning different data based on input
- Testing authentication/authorization flows
- Creating realistic test scenarios
