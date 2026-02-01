# Restaurant Order Management System

> **Cloud-native, serverless restaurant management platform built on AWS**

A comprehensive, enterprise-grade backend system for managing multi-location restaurant chains. Built with Java 17 and AWS serverless architecture, this system handles reservations, menu management, staff operations, customer feedback, and business analytics.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS Lambda](https://img.shields.io/badge/AWS-Lambda-orange.svg)](https://aws.amazon.com/lambda/)
[![DynamoDB](https://img.shields.io/badge/AWS-DynamoDB-blue.svg)](https://aws.amazon.com/dynamodb/)

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Architecture](#-architecture)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Domain Model](#-domain-model)
- [Project Structure](#-project-structure)
- [API Endpoints](#-api-endpoints)
- [Getting Started](#-getting-started)
- [Deployment](#-deployment)
- [Configuration](#-configuration)
- [Testing](#-testing)
- [Use Cases](#-use-cases)

---

## 🎯 Overview

This repository contains a **serverless microservices backend** for a restaurant management system designed to handle the complete operational lifecycle of multi-location restaurant chains. Unlike traditional monolithic Spring Boot applications, this system is architected as **independent AWS Lambda functions** that scale automatically based on demand.

### Why This Architecture?

- **Serverless-First**: Pay only for actual compute time, zero idle costs
- **Auto-Scaling**: Handles 10 or 10,000 concurrent requests automatically
- **High Availability**: Multi-AZ deployment with managed AWS services
- **Cost-Effective**: Eliminates need for constantly running servers
- **Event-Driven**: Asynchronous processing for long-running operations

### What Problems Does It Solve?

1. **Multi-Location Management**: Centralized system for restaurant chains
2. **Table Reservation Conflicts**: Intelligent time-slot management
3. **Staff Performance Tracking**: Automated reports and analytics
4. **Customer Experience**: Seamless booking with feedback integration
5. **Business Intelligence**: Sales trends and operational insights

---

## 🏗️ Architecture

### Serverless Microservices Design

```
┌─────────────────┐
│   API Gateway   │ ← HTTPS Requests
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   ApiHandler    │ ← Main HTTP Lambda
│   (Lambda)      │
└────────┬────────┘
         │
         ├──→ Authentication Handlers
         ├──→ Booking Handlers
         ├──→ Reservation Handlers
         ├──→ Dish Handlers
         ├──→ Location Handlers
         └──→ Feedback Handlers
              │
              ▼
    ┌─────────────────┐
    │  Service Layer  │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐         ┌──────────────┐
    │   DynamoDB      │◄────────│  Cognito     │
    │   (10+ Tables)  │         │  (Auth)      │
    └─────────────────┘         └──────────────┘
             
┌─────────────────┐
│   SQS Queue     │ ← Async Events
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ ReportsHandler  │ ← Report Generation Lambda
│   (Lambda)      │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ EmailService    │ ← Send Reports via Email
└─────────────────┘
```

### Core Components

**Lambda Functions** (Independent Entry Points):
- **`ApiHandler`**: Routes all HTTP API requests
- **`ReportsHandler`**: SQS-triggered report generation
- **`ReportsSenderHandler`**: Email delivery service
- **`SqsEventSenderHandler`**: Event publishing

**Managed AWS Services**:
- **DynamoDB**: NoSQL database (10+ tables)
- **Cognito**: User authentication & authorization
- **SQS**: Asynchronous message queue
- **S3**: Profile image storage
- **API Gateway**: HTTP request routing

---

## ✨ Key Features

### 🍽️ Restaurant Operations
- **Multi-Location Support**: Manage restaurant chains from a single system
- **Smart Table Management**: Capacity-based allocation with real-time availability
- **Time-Slot Reservations**: Conflict-free booking with automated validation
- **Pre-Order Capability**: Customers can select dishes when booking
- **Waiter Assignment**: Intelligent workload distribution across staff

### 📊 Menu Management
- **Comprehensive Dish Database**: Nutrition info, pricing, descriptions
- **Popularity Algorithm**: Dynamic ranking based on orders and feedback
- **Availability Control**: Real-time menu item state management
- **Special Promotions**: Featured dishes and daily specials
- **Multi-Category Support**: Appetizers, mains, desserts, beverages

### 👥 Staff Management
- **Role-Based Access**: Admin, Waiter, Customer, Visitor roles
- **Performance Tracking**: Individual waiter metrics and KPIs
- **Location Assignment**: Staff tied to specific restaurant locations
- **Workload Monitoring**: Real-time booking distribution

### 💬 Customer Feedback
- **Post-Dining Reviews**: Feedback linked to specific bookings
- **Rating System**: 1-5 scale for service quality
- **Dish-Specific Feedback**: Reviews for individual menu items
- **Aggregated Analytics**: Average ratings and trend analysis

### 📈 Business Intelligence
- **Automated Excel Reports**: Professional formatted business reports
- **Sales Analytics**: Revenue, occupancy, customer satisfaction by location
- **Staff Performance Reports**: Productivity, service quality, working hours
- **Trend Analysis**: Period-over-period comparisons with deltas
- **Email Delivery**: Scheduled report distribution

### 🔐 Security & Authentication
- **AWS Cognito Integration**: Enterprise-grade authentication
- **JWT Token Management**: Secure session handling
- **Role-Based Authorization**: Granular access control
- **Password Security**: Managed by AWS Cognito

---

## 🛠️ Technology Stack

### Backend
- **Java 17** - Modern Java with records, pattern matching
- **Spring Boot 3.1.5** - Framework (dependency only, not traditional Spring Boot app)
- **Dagger 2** - Compile-time dependency injection (faster cold starts)
- **Maven** - Build and dependency management

### AWS Services
- **Lambda** (Java 21 runtime) - Serverless compute
- **DynamoDB** - NoSQL database
- **Cognito** - User authentication
- **SQS** - Message queue
- **API Gateway** - HTTP routing
- **S3** - Object storage

### Libraries
- **AWS SDK v1** - SQS, Cognito clients
- **AWS SDK v2** - DynamoDB Enhanced Client
- **Apache POI 5.2.3** - Excel report generation
- **Spring Boot Mail** - Email notifications
- **Lombok** - Reduce boilerplate code
- **Jackson** - JSON serialization

### Development Tools
- **H2 Database** - Local development
- **Spring Boot DevTools** - Hot reload
- **JUnit 5** - Testing framework

---

## 📦 Domain Model

### Core Entities

#### User Management
```java
User
├── cognitoSub: String (Cognito ID)
├── email: String
├── firstName: String
├── lastName: String
├── imageUrl: String
└── role: Role (ADMIN, WAITER, CUSTOMER, VISITOR)

Employee
├── email: String (PK)
├── employeeId: String (GSI)
├── firstName: String
├── lastName: String
├── locationId: String
└── role: String
```

#### Restaurant Operations
```java
Location
├── locationId: String (PK)
├── name: String
├── address: String
├── totalCapacity: String
├── averageOccupancy: String
├── rating: String
└── specialityDishes: String

Table
├── tableId: String
├── locationId: String
├── capacity: Integer
└── status: String

Dish
├── dishId: String (PK)
├── name: String
├── price: String
├── weight: String
├── calories: String
├── carbs, fats, proteins, vitamins: String
├── dishType: String
├── isAvailable: Boolean
├── isPopular: Boolean
└── popularityScore: String
```

#### Booking System
```java
Booking
├── reservationId: String (PK)
├── locationId: String
├── tableId: String
├── date: String
├── timeFrom: String
├── timeTo: String
├── guestsNumber: String
├── userEmail: String
├── waiterId: String
├── status: String (pending, confirmed, completed, cancelled)
├── feedbackId: String
└── byCustomer: Boolean

Feedback
├── feedbackId: String (PK)
├── bookingId: String
├── rating: Integer (1-5)
├── comment: String
└── date: String
```

#### Reporting
```java
LocationReport
├── locationId: String
├── date: String
├── totalRevenue: Double
├── ordersProcessed: Integer
├── occupancyRate: Double
└── customerSatisfaction: Double

WaiterReport
├── waiterId: String
├── date: String
├── workingHours: Integer
├── ordersProcessed: Integer
├── averageFeedback: Double
└── minimumFeedback: Double
```

---

## 📁 Project Structure

```
src/main/java/com/restaurantback/
│
├── ApiHandler.java              # Main HTTP Lambda handler
├── ReportsHandler.java          # Report generation Lambda
├── ReportsSenderHandler.java    # Email delivery Lambda
├── SQSApplication.java          # SQS infrastructure
├── SqsEventSenderHandler.java   # Event publisher
│
├── dto/                         # Data Transfer Objects
│   ├── ApiFeedbackDTO.java
│   ├── DishDTO.java
│   ├── ReservationDto.java
│   ├── WaiterBookingDTO.java
│   └── report/
│       ├── SalesPerformanceDTO.java
│       └── StaffPerformanceDTO.java
│
├── exceptions/                  # Custom exceptions
│   ├── authException/
│   ├── dishException/
│   └── reservationException/
│
├── handlers/                    # HTTP request handlers
│   ├── auth/
│   │   ├── PostSignInHandler.java
│   │   ├── PostSignUpHandler.java
│   │   ├── PostRefreshTokenHandler.java
│   │   └── PostLogOutHandler.java
│   ├── booking/
│   │   ├── BookTableHandler.java
│   │   └── UpdateBookingHandler.java
│   ├── reservation/
│   │   ├── GetReservationHandler.java
│   │   ├── DeleteReservationHandler.java
│   │   └── WaiterUpdateHandler.java
│   ├── dishes/
│   ├── employee/
│   ├── feedbacks/
│   ├── location/
│   ├── profile/
│   ├── tables/
│   └── waiter/
│
├── models/                      # Domain entities
│   ├── User.java
│   ├── Employee.java
│   ├── Waiter.java
│   ├── Booking.java
│   ├── Dish.java
│   ├── Location.java
│   ├── Table.java
│   ├── TimeSlot.java
│   ├── Feedback.java
│   ├── LocationReport.java
│   └── WaiterReport.java
│
├── repository/                  # Data access layer
│   ├── BookingRepository.java
│   ├── UserRepository.java
│   ├── EmployeeRepository.java
│   ├── WaiterRepository.java
│   ├── TableRepository.java
│   └── LocationRepository.java
│
├── services/                    # Business logic
│   ├── BookingService.java      # Reservation management
│   ├── DishesService.java       # Menu operations
│   ├── LocationService.java     # Location management
│   ├── TableService.java        # Table allocation
│   ├── WaiterService.java       # Staff operations
│   ├── FeedbackService.java     # Review handling
│   ├── CognitoSupport.java      # Authentication
│   ├── ExcelService.java        # Report generation
│   ├── SalesReportsService.java # Sales analytics
│   ├── StaffReportsService.java # Staff analytics
│   ├── EmailService.java        # Email notifications
│   └── UserService.java         # User management
│
└── utils/                       # Utility classes
```

---

## 🌐 API Endpoints

### Authentication
```
POST   /signin          # User login
POST   /signup          # User registration
POST   /refresh-token   # Refresh JWT token
POST   /logout          # User logout
```

### Bookings & Reservations
```
POST   /book                      # Create new reservation
PUT    /booking                   # Update reservation
GET    /reservations              # Get user reservations
DELETE /reservation/{id}          # Cancel reservation

# Waiter Operations
GET    /waiter/reservations       # Get assigned bookings
PUT    /waiter/booking            # Update booking status
DELETE /waiter/reservation/{id}   # Cancel on behalf of customer
```

### Menu & Dishes
```
GET    /dishes            # List all dishes
GET    /dishes/{id}       # Get dish details
GET    /popular-dishes    # Get popular items
GET    /dishes/special    # Get special dishes
```

### Locations
```
GET    /locations         # List all restaurant locations
GET    /locations/{id}    # Get location details
```

### Feedback
```
POST   /feedback          # Submit review
GET    /feedback/{id}     # Get feedback details
```

### Reports (Async via SQS)
```
POST   /reports/sales     # Request sales report
POST   /reports/staff     # Request staff performance report
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **AWS Account** (for deployment)
- **AWS CLI** configured
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Itsanshi/Order-management-system.git
   cd Order-management-system
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Configure local environment**
   
   Edit `src/main/resources/application-dev.properties`:
   ```properties
   server.port=8080
   spring.datasource.url=jdbc:h2:mem:restaurantdb
   spring.jpa.hibernate.ddl-auto=update
   ```

4. **Run tests**
   ```bash
   mvn test
   ```

5. **Local testing with AWS SAM**
   ```bash
   sam local start-api
   sam local invoke ApiHandler -e events/test-event.json
   ```

### Package for Deployment

```bash
mvn clean package
```

This produces: `target/restaurant-backend-0.1.0.jar`

---

## 🚢 Deployment

### AWS Lambda Deployment

This project uses **Syndicate Framework** annotations for infrastructure-as-code:

```java
@LambdaHandler(
    lambdaName = "api_handler",
    runtime = DeploymentRuntime.JAVA21,
    roleName = "api_handler-role"
)
@DependsOn(resourceType = ResourceType.DYNAMODB_TABLE, name = "${booking_table}")
```

### Deployment Options

#### Option 1: AWS SAM (Recommended)
```bash
sam build
sam deploy --guided
```

#### Option 2: Serverless Framework
```bash
serverless deploy
```

#### Option 3: AWS CDK
```typescript
new lambda.Function(this, 'ApiHandler', {
  runtime: lambda.Runtime.JAVA_17,
  handler: 'com.restaurantback.ApiHandler',
  code: lambda.Code.fromAsset('target/restaurant-backend-0.1.0.jar')
});
```

#### Option 4: Manual Deployment
1. Upload JAR to S3
2. Create Lambda functions via AWS Console
3. Configure API Gateway integration
4. Set up SQS triggers
5. Configure environment variables

### Required AWS Resources

**Lambda Functions**:
- `api_handler` - Main HTTP handler
- `reports_handler` - Report generation
- `reports_sender_handler` - Email delivery
- `sqs_event_sender_handler` - Event publisher

**DynamoDB Tables**:
- `user_table`
- `employee_table`
- `waiter_table`
- `booking_table`
- `dish_table`
- `feedback_table`
- `location_table`
- `tables_table`
- `timeslot_table`
- `reservation_table`
- `cart_table`
- `locationReportTable`
- `waiterReportTable`

**Other Resources**:
- Cognito User Pool
- SQS Queue (`report_sqs_queue`)
- S3 Bucket (`profile_image_bucket`)
- API Gateway REST API

---

## ⚙️ Configuration

### Environment Variables

Each Lambda function requires the following environment variables:

```bash
# AWS Region
REGION=us-east-1

# Cognito Configuration
COGNITO_ID=us-east-1_xxxxxxxxx
CLIENT_ID=xxxxxxxxxxxxxxxxxxxxx

# DynamoDB Tables
userTable=restaurant-users
employeeTable=restaurant-employees
waiterTable=restaurant-waiters
bookingTable=restaurant-bookings
dishTable=restaurant-dishes
feedbackTable=restaurant-feedback
locationTable=restaurant-locations
tablesTable=restaurant-tables
timeslotTable=restaurant-timeslots
reservationTable=restaurant-reservations
cartTable=restaurant-cart
locationReportTable=restaurant-location-reports
waiterReportTable=restaurant-waiter-reports

# S3 Configuration
profileImageBucket=restaurant-profile-images

# SQS Configuration
reportQueue=restaurant-report-queue

# Email Configuration (for EmailService)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
```

### Secrets Management

For production, use **AWS Secrets Manager** or **Parameter Store**:

```bash
aws secretsmanager create-secret \
  --name restaurant/cognito/client-id \
  --secret-string "your-client-id"
```

---

## 🧪 Testing

### Unit Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Run with coverage
mvn clean test jacoco:report
```

### Integration Tests

```bash
# Using AWS SAM Local
sam local start-api
curl http://localhost:3000/dishes

# Using LocalStack
docker-compose up localstack
mvn verify -Pintegration-tests
```

### Load Testing

```bash
# Using Apache Bench
ab -n 1000 -c 10 https://your-api.execute-api.us-east-1.amazonaws.com/prod/dishes

# Using Artillery
artillery run load-test.yml
```

---

## 💼 Use Cases

### Customer Journey
1. **Sign Up**: Create account via Cognito
2. **Browse Locations**: View available restaurants
3. **Check Menu**: Browse dishes with nutrition info
4. **Make Reservation**: Select date, time, and table
5. **Pre-Order**: Add dishes to booking
6. **View Confirmation**: Receive booking details
7. **Dine & Feedback**: Submit post-dining review

### Waiter Operations
1. **View Assigned Bookings**: Check daily schedule
2. **Update Booking Status**: Mark as confirmed/completed
3. **Assist Customers**: Make reservations on behalf of guests
4. **Manage Tables**: Update table availability
5. **Review Performance**: Access individual metrics

### Admin/Management
1. **Monitor Locations**: Track performance across restaurants
2. **Review Reports**: Analyze sales and staff performance
3. **Manage Menu**: Update dishes and availability
4. **Staff Management**: Assign roles and locations
5. **Business Analytics**: Identify trends and opportunities

### Automated Processes
1. **Report Generation**: Daily/weekly scheduled reports
2. **Email Delivery**: Automated report distribution
3. **Feedback Aggregation**: Real-time rating calculations
4. **Popularity Scoring**: Dynamic dish ranking
5. **Capacity Management**: Real-time table availability

---


## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👥 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📧 Contact

For questions or support, please open an issue in the GitHub repository.

---

**Built with ☕ and ❤️ using Java 17 and AWS Serverless**
