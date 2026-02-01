# Restaurant Back (Backend)

Overview

This repository contains a Java backend for a restaurant management system. It provides APIs for bookings, dishes, employees, feedback, reporting, reservations, and related services. The codebase is organized following a typical Maven/Gradle Java project layout under `main/java/com/restaurantback`.
# Restaurant Back (Backend)

Overview

This repository contains the backend implementation for a restaurant management system. Important: this codebase is organized as a set of AWS Lambda handlers rather than a single Spring Boot application entry point. Each top-level class under `src/main/java/com/restaurantback` is intended to run as an independent Lambda (for example API handlers, report senders, and SQS event senders).

Key features
- Booking and reservation management
- Dish management and menus
- Employee and waiter handling
- Customer feedback collection
- Sales and staff reports (Excel export)
- Integration points for Cognito authentication and AWS SQS messaging

Project structure (selected)
- `main/java/com/restaurantback` — Lambda handler classes and support code
  - `dto/` — Data Transfer Objects (API payloads and responses)
  - `handlers/` — Lambda handler implementations and HTTP routing helpers
  - `models/` — Domain entities (Booking, Dish, Employee, User, Table, etc.)
  - `repository/` — Persistence/repository interfaces
  - `services/` — Business logic services (BookingService, DishesService, EmailService, ExcelService, etc.)
  - `utils/` — Utility helpers
  - `exceptions/` — Application-specific exceptions

Handler files of interest
- `ApiHandler.java` — API gateway / HTTP-style Lambda routing
- `ReportsHandler.java`, `ReportsSenderHandler.java` — report generation and sending
- `SQSApplication.java`, `SqsEventSenderHandler.java` — SQS integration and event sender

Build & package for AWS Lambda

This project is a Java Maven project. Build and package artifacts for Lambda using your build tool and a Lambda-compatible packaging approach (fat JAR / shaded JAR). Example with Maven:

```powershell
mvn clean package
# resulting shaded/fat JAR typically under target/ (adjust based on pom configuration)
```

Deployment notes
- Each Lambda handler class can be deployed as a separate AWS Lambda function. Configure the function's handler to the fully-qualified class name (or to the adapter/mapping class if you use aws-serverless-java-container).
- For HTTP APIs, use API Gateway or HTTP API with a Lambda integration that forwards requests to `ApiHandler` or an adapter wrapper.
- For SQS consumers or scheduled jobs, configure the appropriate event source mapping to point to the SQS handler classes.
- Consider using AWS SAM, Serverless Framework, or CDK to define and deploy multiple Lambda functions from this repository.

Configuration

Configuration values (Cognito, database, SQS, SMTP, etc.) are loaded from environment variables or configuration files. When deploying to Lambda, supply these values via function environment variables or use a secrets manager (AWS Secrets Manager / Parameter Store).

Quick local testing
- Unit-test service logic locally with your IDE or `mvn test`.
- For local Lambda invocation and integration testing, use AWS SAM CLI or localstack to emulate AWS services and exercise Lambda handlers.

Testing

Run unit tests with Maven:

```powershell
# Maven
mvn test
```

Developer notes
- Treat top-level classes in `com.restaurantback` as Lambda handlers rather than a single `main` entrypoint.
- Use an IDE to navigate DTOs and handlers; the primary business logic lives in `services/` and is suitable for unit testing.

Contributing

- Fork and create feature branches.
- Add tests for new functionality.
- Use descriptive commit messages and open pull requests with a summary of changes.

Next steps
- I can expand this README with deployment examples for AWS SAM/Serverless Framework, add example `template.yaml` or `serverless.yml`, or generate per-handler documentation. Which would you like?
