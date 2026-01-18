# Flowable Mock Service Generator

A Spring Boot application that dynamically mocks Flowable services by fetching service definitions and generating REST endpoints with configurable or random responses.

## Features

- **Dynamic Service Discovery**: Automatically fetches service definitions from a Flowable host
- **Dynamic Endpoint Generation**: Creates mock endpoints that mirror Flowable service operation URLs
- **Static Random Data**: Generates consistent random response data at sync time (not per request)
- **Custom Response Configuration**: Override HTTP status codes and response bodies via web UI
- **Request Logging**: Tracks all incoming requests and responses
- **Import/Export**: Backup and restore mock configurations as JSON
- **Conflict Resolution**: Prioritizes configured operations when multiple services share the same URL
- **Web UI**: User-friendly interface for managing services, operations, and configurations

## Prerequisites

- **Java 21** (required)
- Maven (wrapper included)

## Quick Start

### 1. Configure Flowable Host

Edit `src/main/resources/application.properties`:

```properties
flowable.host=http://localhost:8080
flowable.username=admin
flowable.password=test
```

### 2. Run the Application

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw spring-boot:run
```

The application will start on **http://localhost:8083**

### 3. Sync Services

1. Open http://localhost:8083 in your browser
2. Click **"Sync Services"** to fetch service definitions from your Flowable host
3. View operations and configure mock responses as needed

## Usage

### Viewing Service Operations

1. Navigate to the dashboard at http://localhost:8083
2. Click **"View Operations"** on any service card
3. See the list of operations with their mock endpoint URLs (e.g., `/data`, `/create`)

### Configuring Mock Responses

1. Click **"Configure Response"** on an operation
2. Set the **HTTP Status Code** (200, 404, 500, etc.)
3. Optionally provide a **Custom Response Body** (JSON)
   - If left empty, the system uses pre-generated static random data
4. Save the configuration

### Testing Mock Endpoints

Send requests directly to the operation URLs:

```bash
# Example: GET request to /data endpoint
curl http://localhost:8083/data

# Example: POST request to /create endpoint
curl -X POST http://localhost:8083/create \
  -H "Content-Type: application/json" \
  -d '{"name":"test"}'
```

### Viewing Request Logs

Click **"Logs"** in the navigation to see all captured requests and responses.

### Import/Export Configurations

- **Export**: Download all mock configurations as JSON
- **Import**: Upload a previously exported configuration file to restore settings

## How It Works

### Static Random Data Generation

Random response data is generated **once during service synchronization** and stored in the database. This ensures:
- Consistent responses across multiple requests
- Predictable testing scenarios
- No performance overhead from generating data on every request

To regenerate random data, simply click **"Sync Services"** again.

### URL Conflict Resolution

When multiple services share the same operation URL (e.g., two services both have `/data`):
1. The system first looks for an operation with a custom configuration
2. If found, it uses that configured operation
3. Otherwise, it defaults to the first matching operation

## API Documentation

- **Swagger UI**: http://localhost:8083/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8083/v3/api-docs
- **Actuator**: http://localhost:8083/actuator

## Project Structure

```
mock-services/
├── src/main/java/com/example/mockservice/
│   ├── domain/              # JPA entities
│   ├── repository/          # Spring Data repositories
│   ├── service/             # Business logic
│   ├── util/                # Utilities (random data generator)
│   └── web/                 # Controllers
├── src/main/resources/
│   ├── templates/           # Thymeleaf HTML templates
│   └── application.properties
└── src/test/java/           # Unit tests
```

## Configuration

### Application Properties

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8083 | Application port |
| `flowable.host` | http://localhost:8080 | Flowable server URL |
| `flowable.username` | admin | Flowable authentication username |
| `flowable.password` | test | Flowable authentication password |
| `logging.level.com.example.mockservice` | DEBUG | Logging level |

### Database

Uses H2 in-memory database by default. Data is reset on application restart.

## Testing

Run unit tests:

```bash
./mvnw test
```

Run with coverage:

```bash
./mvnw clean verify
```

## Development

### Building

```bash
./mvnw clean package
```

The executable JAR will be in `target/mock-service-0.0.1-SNAPSHOT.jar`

### Running the JAR

```bash
java -jar target/mock-service-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

### Issue: "Operation not found for path"

**Cause**: The operation URL doesn't match any synced service operation.

**Solution**: 
1. Verify the Flowable host is accessible
2. Click "Sync Services" to refresh definitions
3. Check that the operation exists in your Flowable service definition

### Issue: Mock returns 200 instead of configured status

**Cause**: Multiple operations share the same URL, and an unconfigured one was selected.

**Solution**: The system now prioritizes configured operations. Ensure you've saved your configuration and try again.

### Issue: Random data changes on every request

**Cause**: You may be running an older version.

**Solution**: Pull the latest code. Random data is now static and generated at sync time.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Repository

https://github.com/mjdomin86/mock-flowable-services
