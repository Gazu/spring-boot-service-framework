# Actuator Consumer Example

Standalone Spring Boot application that imports
`com.smbtech:spring-boot-service-framework-platform:0.5.0` and consumes
`com.smbtech:spring-boot-service-framework-starter-actuator` without an
individual version from the framework module-local Maven repositories.

The example resolves published Maven artifacts instead of Gradle
`project(...)` dependencies.

## Covered Scenarios

| Endpoint | Access | Purpose |
|---|---|---|
| `GET /api/dummy` | Public | Confirms the application remains available. |
| `GET /actuator/health` | Public; details require `ACTUATOR` | Shows the `serviceFramework` health contributor. |
| `GET /actuator/info` | Public | Shows bounded framework module information. |
| `GET /actuator/serviceframework` | `ACTUATOR` | Shows the read-only framework diagnostic endpoint. |
| `GET /actuator/metrics/smbtech.service.framework.status` | `ACTUATOR` | Shows the bounded aggregate status metric. |

The application publishes one `DiagnosticProbe` and one
`FrameworkModuleInfoProvider`. The probe intentionally supplies a value under
the sensitive `clientSecret` key so the smoke test can prove that only
`[REDACTED]` reaches the endpoint.

## Security

`SecurityConfiguration` is application code. The framework starter does not
create a `SecurityFilterChain`.

The dummy, health, and info endpoints are public in this example. The custom
diagnostic and metrics endpoints require HTTP Basic authentication with the
`ACTUATOR` role. Health details use Spring Boot's `when-authorized` policy.

Local credentials are environment-backed:

| Variable | Default |
|---|---|
| `EXAMPLE_ACTUATOR_USERNAME` | `actuator-user` |
| `EXAMPLE_ACTUATOR_PASSWORD` | `change-me` |

## Configuration

```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
      roles: ACTUATOR
    serviceframework:
      access: read-only
  endpoints:
    web:
      exposure:
        include: health,info,metrics,serviceframework

smbtech:
  actuator:
    diagnostics:
      cache-ttl: 5s
      operation-timeout: 2s
      max-components: 64
      max-modules: 64
    metrics:
      enabled: true
      cache-ttl: 10s
```

## Run

From the framework root:

```bash
./gradlew publishLocalArtifacts
cd examples/actuator-consumer
../../gradlew bootRun
```

Call the protected diagnostic endpoint:

```bash
ACTUATOR_USERNAME=actuator-user
ACTUATOR_PASSWORD=change-me
curl --user "${ACTUATOR_USERNAME}:${ACTUATOR_PASSWORD}" \
  http://localhost:8080/actuator/serviceframework
```

## Tests

The HTTP smoke tests verify public and protected routes, health detail
authorization, diagnostic probe and module discovery, secret redaction, and
Micrometer metric availability.

Run the standalone published-artifact test from the framework root:

```bash
./gradlew actuatorConsumerSmoke
```

The supported API and runtime contract is documented in
[Actuator Compatibility](../../docs/actuator/compatibility.md).
