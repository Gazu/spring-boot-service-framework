# REST Client consumer example

Standalone Spring Boot application that consumes
`com.smbtech:spring-boot-service-framework-starter-rest-client:0.1.0-SNAPSHOT` from the local
Maven repositories generated under each module `build/repository` directory.

From the framework root:

```bash
./gradlew restClientConsumerSmoke
```

The example does not use Gradle `project(...)` dependencies. It validates the
same POMs and JARs that another repository would consume.

It demonstrates:

- `smbtech.rest-clients` configuration;
- automatic creation of `paymentsRestClient`;
- `ApiClientFactory`;
- declarative `@HttpApiClient` + `@HttpExchange` interfaces;
- `AccessTokenClient` availability for the `client_credentials` flow;
- retry and circuit breaker configuration.

The example expects OAuth2 values to be supplied through environment variables
when it is run manually:

```bash
export TOKEN_URI=https://auth.example/oauth2/token
export CLIENT_ID=payments-client
export CLIENT_SECRET=change-me
```

To run it manually after publishing local artifacts:

```bash
./gradlew publishLocalArtifacts
cd examples/rest-client-consumer
../../gradlew bootRun
```

The default `application.yml` uses `http://localhost:9999` as an example URL.
Tests override it dynamically with a local HTTP server.
