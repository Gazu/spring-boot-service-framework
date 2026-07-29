# Quality Pilot

Executable Spring Boot application used by the private quality platform pilot.

Run the tests and coverage report:

```bash
./gradlew :examples:quality-pilot:test \
  :examples:quality-pilot:jacocoTestReport
```

Run the application:

```bash
./gradlew :examples:quality-pilot:bootRun
curl --fail http://localhost:8080/api/pilot
```

Expected response:

```json
{"status":"ok"}
```
