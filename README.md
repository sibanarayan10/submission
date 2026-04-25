# Submission Service

A high-throughput, event-driven microservice responsible for accepting code submissions, managing submission lifecycle, and orchestrating code execution via a sandboxed Python judge worker.

---

## Overview

The Submission Service is the most performance-critical component of the platform. It is designed to be **stateless and horizontally scalable** — absorbing burst traffic during contests while delegating actual code execution to isolated worker containers asynchronously via Apache Kafka.

---

## Architecture

```
Client
  ↓
POST /api/v1/submissions
  ↓
Submission Service
├── Validates user and problem via local snapshots
├── Persists submission with status PENDING
├── Publishes SubmissionEvent → Kafka (submission.pending)
└── Returns submissionId immediately
        ↓
Judge Worker (Python)
├── Consumes from submission.pending
├── Spawns isolated Docker container per submission
├── Runs user code against test cases fetched from Core Service
├── Compares output against expected output
└── Publishes JudgeResultEvent → Kafka (judge.results)
        ↓
Submission Service (Consumer)
├── Consumes from judge.results
├── Updates submission status and verdict in DB
└── Pushes result to client via SSE
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build Tool | Maven |
| Database | PostgreSQL |
| Migrations | Flyway |
| Messaging | Apache Kafka |
| Real-time | Server-Sent Events (SSE) |
| Execution | Docker (sandboxed containers) |
| ORM | Spring Data JPA + Hibernate |

---

## Submission Lifecycle

```
PENDING → QUEUED → RUNNING → ACCEPTED
                            → WRONG_ANSWER
                            → TIME_LIMIT_EXCEEDED
                            → MEMORY_LIMIT_EXCEEDED
                            → RUNTIME_ERROR
                            → COMPILE_ERROR
```

---

## API Endpoints

### Submit Code
```
POST /api/v1/submissions
Header: X-User-Id: <uuid>
```
```json
{
  "problemId": "uuid",
  "language": "PYTHON",
  "sourceCode": "nums = list(map(int, input().split()))..."
}
```
**Response:**
```json
{
  "submissionId": "uuid",
  "status": "PENDING"
}
```

---

### Get Submission Result (SSE)
```
GET /api/v1/submissions/{submissionId}/result
Header: X-User-Id: <uuid>
```
Long-lived SSE connection. Closes automatically when verdict is delivered.

---

### Get Submission by ID
```
GET /api/v1/submissions/{submissionId}
Header: X-User-Id: <uuid>
```

---

### Get User Submission History
```
GET /api/v1/submissions?problemId={uuid}&page=0&size=20
Header: X-User-Id: <uuid>
```

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `submission.pending` | Submission Service | Judge Worker | Triggers code execution |
| `judge.results` | Judge Worker | Submission Service | Delivers verdict back |

---

## Project Structure

```
submission-service/
├── src/main/java/com/sibanarayan/submission/
│   ├── entities/
│   │   ├── Base.java
│   │   ├── Submission.java
│   │   ├── ProblemSnapshot.java
│   │   └── UserSnapshot.java
│   ├── enums/
│   │   ├── ProgrammingLanguage.java
│   │   ├── SubmissionStatus.java
│   │   └── RecordStatus.java
│   ├── events/
│   │   ├── SubmissionEvent.java
│   │   └── JudgeResultEvent.java
│   ├── consumers/
│   │   └── JudgeResultConsumer.java
│   ├── service/
│   │   ├── SubmissionService.java
│   │   └── impl/SubmissionServiceImpl.java
│   ├── controllers/
│   │   └── SubmissionController.java
│   ├── repositories/
│   │   ├── SubmissionRepository.java
│   │   ├── ProblemSnapshotRepository.java
│   │   └── UserSnapshotRepository.java
│   ├── models/
│   │   ├── request/SubmissionRequest.java
│   │   └── response/SubmissionResponse.java
│   └── exceptions/
│       ├── GlobalExceptionHandler.java
│       └── EntityNotFoundException.java
└── src/main/resources/
    ├── application.yaml
    └── db/migration/
        └── V1__create_submissions.sql
```

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- Docker
- PostgreSQL 15+
- Apache Kafka 3.9+

### Setup

**1. Create the database**
```sql
CREATE DATABASE submission;
```

**2. Configure `application.yaml`**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/submission
    username: your_username
    password: your_password
  kafka:
    bootstrap-servers: localhost:9092
```

**3. Start Kafka**
```bash
docker run -d --name kafka \
  -p 9092:9092 \
  apache/kafka:3.9.0
```

**4. Build and run**
```bash
mvn clean package -DskipTests
java -jar target/submission-0.0.1-SNAPSHOT.jar
```

Service starts on `http://localhost:8082`

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `localhost:5432/submission` | PostgreSQL connection URL |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | — | Database password |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka broker address |
| `SERVER_PORT` | `8082` | Service port |

---

## Judge Worker

The Python judge worker runs inside this service as a separate component. For each submission it:

1. Writes user code to a temp file
2. Spawns an isolated Docker container with memory and CPU limits
3. Pipes test case input via stdin
4. Captures stdout and compares against expected output
5. Publishes verdict back to `judge.results`

**Execution limits per submission:**
- Memory: 256 MB
- CPU: 0.5 cores
- Timeout: 5 seconds
- Network: disabled (`--network=none`)

---

## Related Services

| Service | Port | Responsibility |
|---|---|---|
| Core Service | 8080 | Problems, users, engagement |
| Submission Service | 8082 | Submissions, execution |

---

## Collaboration

Feel free to reach out for contributions, or questions.

**Sibanarayan Choudhury**
- Email: sibanarayan015@gmail.com
- Phone: +91 7848916166
