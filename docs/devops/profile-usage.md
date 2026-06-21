# Spring Profile Usage

## LOCAL

Use for daily backend development.

```bash
export $(grep -v '^#' .env.local | xargs)
./mvnw spring-boot:run
```

API:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## DEV

Use for the shared development backend.

```bash
export $(grep -v '^#' .env.dev | xargs)
./mvnw spring-boot:run
```

Public API:

```text
https://api-dev.easyfleetmatch.com
```

Frontend and mobile developers should use this base URL for shared development.

## PROD

Use only on production infrastructure.

```bash
export $(grep -v '^#' .env.prod | xargs)
java -jar target/fleetmatch-backend-0.0.1-SNAPSHOT.jar
```

Public API:

```text
https://api.easyfleetmatch.com
```
