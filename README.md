# Loan Application System

A microservices-based loan application system built to learn and demonstrate core backend engineering concepts. Built with Spring Boot, MySQL, Redis, Docker, and Kubernetes.

---

## What this project does

A user registers and logs in to get a JWT token. They can then apply for a loan by submitting their financial profile. The system runs eligibility checks and either approves or rejects the application. Admins can then approve, disburse, or close loans.

---

## Services

- **service-discovery** (port 8761) — Eureka server, all services register here
- **user-management** (port 8081) — handles registration, login, JWT
- **loan-eligibility** (port 8082) — handles loan applications and eligibility engine

Each service has its own database. They communicate via REST using WebClient.

---

## Concepts implemented

- JWT authentication and Spring Security
- `@Transactional` with pessimistic row locking to prevent race conditions
- Idempotency keys to prevent duplicate loan applications
- Redis caching for user data and eligibility results
- Rate limiting with Bucket4j (3 requests per hour per user)
- Circuit breaker with Resilience4j on the user-service call
- Database indexing on frequently queried columns
- Eureka service discovery
- Docker and Docker Compose for local setup
- Kubernetes deployment using minikube

---

## Tech stack

- Java 17, Spring Boot 3
- MySQL, JPA/Hibernate
- Redis (Lettuce)
- Resilience4j, Bucket4j
- Netflix Eureka
- Docker, Docker Compose
- Kubernetes (minikube)

---

## How to run locally

**1. Build all services**
```bash
cd service-discovery && ./mvnw clean package -DskipTests && cd ..
cd user-management && ./mvnw clean package -DskipTests && cd ..
cd loan-eligibility && ./mvnw clean package -DskipTests && cd ..
```

**2. Start everything**
```bash
docker-compose up --build
```

**3. Access**
- Eureka: `http://localhost:8761`
- User service: `http://localhost:8081`
- Loan service: `http://localhost:8082`

---

## How to run on Kubernetes

**1. Start minikube**
```bash
minikube start
```

**2. Point Docker to minikube**
```bash
# Windows
minikube docker-env | Invoke-Expression
# Mac/Linux
eval $(minikube docker-env)
```

**3. Build images**
```bash
docker build -t service-discovery:latest ./service-discovery
docker build -t user-management:latest ./user-management
docker build -t loan-eligibility:latest ./loan-eligibility
```

**4. Deploy**
```bash
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/mysql/
kubectl apply -f k8s/redis/
kubectl apply -f k8s/service-discovery/
kubectl apply -f k8s/user-management/
kubectl apply -f k8s/loan-eligibility/
```

**5. Access services**
```bash
minikube service user-management -n loan-system
minikube service loan-eligibility -n loan-system
```

---

## API endpoints

### Register
```
POST /api/auth/register
{
    "username": "john",
    "password": "pass123",
    "email": "john@gmail.com",
    "fullName": "John Doe"
}
```

### Login
```
POST /api/auth/login
{
    "usernameOrEmail": "john",
    "password": "pass123"
}
```

### Apply for loan
```
POST /api/loans/apply
Authorization: Bearer <token>
{
    "loanType": "PERSONAL",
    "requestedAmount": 500000,
    "tenureMonths": 24,
    "idempotencyKey": "unique-key-001",
    "applicantProfile": {
        "nationality": "Indian",
        "location": "Hyderabad",
        "incomeSource": "SALARIED",
        "monthlyIncome": 50000,
        "cibilScore": 750,
        "existingLoanCount": 0,
        "totalExistingEmi": 0
    }
}
```

### Other loan endpoints
```
GET  /api/loans/my-loans          — get all my loans
GET  /api/loans/{id}              — get loan by id
PUT  /api/loans/{id}/approve      — admin only
PUT  /api/loans/{id}/reject       — admin only
PUT  /api/loans/{id}/disburse     — admin only
PUT  /api/loans/{id}/close        — admin only
```

---

## Eligibility rules

- CIBIL score must be at least 650
- Monthly income must be at least ₹25,000
- Cannot have more than 3 existing loans
- Total existing EMI must not exceed 50% of monthly income
- Cannot have an active loan application already

---

## Loan lifecycle
```
APPLIED → UNDER_REVIEW → APPROVED → DISBURSED → CLOSED
                              ↓
                          REJECTED
```