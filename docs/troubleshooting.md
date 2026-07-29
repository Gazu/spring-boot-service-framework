# Troubleshooting

This is the canonical troubleshooting guide for Spring Boot Service Framework.
Use it when a consumer application fails to resolve artifacts, start, create
clients, request OAuth2 tokens, load keystores, emit logs, or serve mocks.

## First Checks

Run the documentation and build checks from the framework root:

```bash
./gradlew documentationCheck
./gradlew check
```

If a standalone example fails after local changes, republish local artifacts:

```bash
./gradlew publishLocalArtifacts
```

For a full compatibility pass:

```bash
./gradlew compatibilityCheck
```

## Gradle And Publication

| Symptom | Likely cause | Fix |
|---|---|---|
| Gradle cannot resolve a framework artifact | Artifacts were not published to Maven local or the module-local repositories. | Run `./gradlew publishToMavenLocal` for Maven local consumption, or `./gradlew publishLocalArtifacts` for standalone examples. |
| Gradle cannot resolve a generated OpenAPI artifact | The root OpenAPI local build repository was not published or was not declared in the consumer build. | Run `./gradlew publishOpenApiArtifactsToLocalBuildRepository`, then add `build/repository/openapi` as a Maven repository. |
| Standalone examples still use old framework code | Example repositories point to module-local `build/repository` artifacts that were not regenerated. | Run `./gradlew publishLocalArtifacts`, then rerun the example test. |
| `documentationCheck` fails on generated property references | A `@ConfigurationProperties` class changed but generated docs were not refreshed. | Run `./gradlew generatePropertyReferences`, review the generated docs, then rerun `./gradlew documentationCheck`. |
| `documentationCheck` fails on a Markdown link or anchor | A linked file is missing or a `#heading-anchor` does not match a real heading. | Fix the target path or heading, then rerun `./gradlew documentationCheck`. |
| `documentationCheck` fails on documentation catalog or release docs | A required canonical document, index link, changelog section, or release checklist reference is missing. | Update `README.md`, `docs/index.md`, `docs/documentation-architecture.md`, `CHANGELOG.md`, or `docs/releasing.md` as reported. |
| `documentationCheck` fails on framework versions | A dependency snippet, example build, release snippet, or current-status line references an older framework version. | Align the version with the root `build.gradle` project version. |
| A module compatibility contract is stale | A supported public type, extension point, property, auto-configuration import, or Gradle plugin id changed. | Run `./gradlew generateModuleCompatibilityContracts`, review the contract diff, document intentional breaking changes, and rerun the focused `<module>CompatibilityCheck`. |
| `documentationCheck` fails on OpenAPI name normalization | The `info.title` normalization contract changed or a title normalizes to an invalid artifact base name. | Run `./gradlew validateOpenApiNameNormalization`, then fix the title or normalization rule. |
| `documentationCheck` fails on OpenAPI specs | An OpenAPI document has missing/invalid `info.title` or `info.version`, or generated coordinates collide. | Fix the OpenAPI `info` block or rename the contract title before rerunning `./gradlew validateOpenApiSpecs`. |
| `documentationCheck` fails on OpenAPI spec version catalog | A spec changed without a matching catalog entry, or the same `info.version` now has different content. | For intentional contract changes, bump `info.version`, run `./gradlew generateOpenApiSpecVersionCatalog`, and commit the catalog update. |
| `openApiBreakingChangeCheck` reports a missing current baseline | The current `info.version` has no exact snapshot under `docs/openapi-baselines/<contract>`. | Copy the completed current spec to `<version>.yaml` in its baseline directory and rerun the task. |
| `openApiBreakingChangeCheck` reports a baseline mismatch | The same-version baseline was edited independently or was not refreshed with the completed current contract. | Before publication, make the new version snapshot byte-identical to the current spec. Never rewrite a previously published baseline. |
| `openApiBreakingChangeCheck` rejects the version increase | A breaking change used minor/patch, or a compatible addition used patch. | Increase `MAJOR` for breaking changes or `MINOR` for compatible additions, then refresh the spec catalog and current baseline snapshot. |
| `documentationCheck` fails on OpenAPI metadata | Generated `contract.properties` is missing or does not match the source spec. | Run `./gradlew generateOpenApiMetadata validateOpenApiMetadata` and inspect `build/generated/smbtech-openapi/metadata`. |
| `documentationCheck` fails on advanced OpenAPI model generation | The generated model source no longer contains expected refs, enums, arrays, maps, or validation annotations for the advanced fixture. | Run `./gradlew validateOpenApiAdvancedModelGeneration`, then inspect `docs/openapi/warehouse-inventory-catalog.yaml` and `build/generated/smbtech-openapi/models/warehouse-inventory-catalog`. |
| `documentationCheck` fails on OpenAPI models JAR | The generated model sources do not compile or the JAR misses model classes/metadata. | Run `./gradlew openApiModelsJar validateOpenApiModelsJar` and inspect `build/generated/smbtech-openapi/models` plus `build/libs/openapi/models`. |
| `documentationCheck` fails on OpenAPI server API JAR | The generated server API sources do not compile or the JAR misses delegate/controller classes/metadata. | Run `./gradlew openApiServerApiJar validateOpenApiServerApiJar` and inspect `build/generated/smbtech-openapi/server-api` plus `build/libs/openapi/api`. |
| `documentationCheck` fails on OpenAPI client JAR | The generated client interface sources do not compile or the JAR misses client classes/metadata. | Run `./gradlew openApiClientJar validateOpenApiClientJar` and inspect `build/generated/smbtech-openapi/client` plus `build/libs/openapi/client`. |
| `documentationCheck` fails on OpenAPI artifact separation | A generated JAR contains classes from the wrong artifact boundary or duplicates classes across `models`, `api`, and `client`. | Run `./gradlew validateOpenApiArtifactSeparation`, then inspect `build/libs/openapi`. |
| `documentationCheck` fails on OpenAPI reproducibility | Generated output contains volatile values, JAR entry order changed, timestamps are not fixed, or a rebuilt artifact hash differs. | Run `./gradlew validateOpenApiReproducibleGeneration`, then inspect `build/generated/smbtech-openapi`, `build/classes/smbtech-openapi`, and `build/libs/openapi`. |
| `documentationCheck` fails on OpenAPI compilation tests | A consumer-style source cannot compile against the generated `models`, `api`, and `client` JARs together. | Run `./gradlew validateOpenApiCompilationTests`, then inspect `build/generated/smbtech-openapi/compile-tests` and the reported missing type or classpath error. |
| `documentationCheck` fails on OpenAPI task compatibility | A public OpenAPI Gradle command was renamed, removed, or left without task metadata. | Restore the public task name or provide a backward-compatible alias, then run `./gradlew validateOpenApiTaskCompatibility`. |
| `openApiCompatibilityCheck` fails on generator module compatibility | The reusable OpenAPI generator module no longer exposes a required public type or its module tests fail. | Run `./gradlew validateOpenApiGeneratorModuleCompatibility --stacktrace`, then inspect `spring-boot-service-framework-openapi-generator`. |
| `openApiCompatibilityCheck` fails | One part of the generated OpenAPI compatibility contract failed: spec naming, metadata, version catalog, JAR contents, artifact separation, reproducibility, compilation, generator module checks, build-logic checks, or local publication. | Run `./gradlew openApiCompatibilityCheck --stacktrace`, then rerun the specific failing `validateOpenApi*` task named in the Gradle output. |
| `documentationCheck` fails on example secrets | Example docs or config contain literal secret-like values. | Replace real values with environment placeholders such as `${PAYMENTS_CLIENT_SECRET}`. |

## Logging

| Symptom | Likely cause | Fix |
|---|---|---|
| A custom Logback destination is ignored | `SERVICE_FRAMEWORK_LOGGING_DELEGATE` was set after `async-appender.xml` was included, or its value does not match an appender name. | Define the destination, set the delegate property, then include `async-appender.xml` and `root.xml` in that order. |
| Logback reports that only one appender may be attached to `ASYNC` | Logback `AsyncAppender` supports one delegate. | Use `SERVICE_FRAMEWORK_LOGGING_DELEGATE` for one destination, or define application-owned async appenders and a custom root for fan-out. |
| Async metrics are missing with a custom `logback-spring.xml` | The custom configuration omitted the framework `async-appender.xml`, changed the `ASYNC` name, or disabled observability. | Include the framework properties and async fragments, retain the `ASYNC` name, and enable `smbtech.logging.async.observability.enabled`. |
| The application starts with different saturation behavior after migration | A legacy `never-block` or positive `discarding-threshold` override is still configured. | Remove the legacy override and configure `saturation-policy` explicitly, or retain it intentionally according to the compatibility precedence table. |

## REST Client Creation

| Symptom | Likely cause | Fix |
|---|---|---|
| `RestClient` bean is missing | The client definition is missing, disabled, or has no `base-url`. | Check `smbtech.rest-clients.clients.<name>.enabled=true` and `base-url`. |
| `@Qualifier` injection fails | The generated bean name is different from the qualifier. | Use `<clientName>RestClient`, or set `clients.<name>.bean-name`. |
| `RestClientRegistry.get("<name>")` fails | The client name is not configured or is disabled. | Check `smbtech.rest-clients.clients.<name>` and `enabled`. |
| Declarative API proxy creation fails | The interface has no `@HttpApiClient`, or the referenced client name is missing. | Add `@HttpApiClient("<name>")` or call `apiClientFactory.create("<name>", Api.class)`. |

## OAuth2 And Token Acquisition

| Symptom or message | Meaning | Fix |
|---|---|---|
| OAuth2 validation fails at startup | The REST client OAuth2 configuration is inconsistent. | Read the reported YAML path and `Fix:` hint. Most issues are missing registrations, incompatible grant types, missing signing config, or unresolved credential/keystore refs. |
| OAuth2 validation reports that `token-request-id` is required after an upgrade | The removed `credential-token-requestor-id` key is still configured. | Rename the client property to `token-request-id`; the former key has no compatibility alias. |
| `OAuth2 client registration not configured for token request: <id>` | No supported Spring registration exists for the `token-request-id`. | Add `spring.security.oauth2.client.registration.<id>` or fix `token-request-id`. |
| `OAuth2 client registration not configured for client_credentials: <id>` | `AccessTokenClient.clientCredentials(...)` was called for a missing registration or a registration with another grant. | Use a registration whose `authorization-grant-type` is `client_credentials`. |
| `OAuth2 client registration not configured for JWT bearer grant: <id>` | `AccessTokenClient.jwtBearer(...)` was called for a missing registration or a registration with another grant. | Use a registration whose `authorization-grant-type` is `urn:ietf:params:oauth:grant-type:jwt-bearer`. |
| `client assertion configuration not found for OAuth2 registration: <id>` | A `private_key_jwt` registration has no SMBTech signing extension. | Add `smbtech.rest-clients.authentication.client-assertions.<id>`. |
| `key-store-id is required for private_key_jwt client assertion: <id>` | The client assertion extension exists but does not point to a signing keystore. | Set `authentication.client-assertions.<id>.key-store-id`. |
| `jwt-bearer configuration not found for OAuth2 registration: <id>` | A JWT bearer registration has no SMBTech JWT assertion extension. | Add `smbtech.rest-clients.authentication.jwt-bearer.<id>`. |
| Token endpoint rejects `private_key_jwt` | Signing key, alias, password, audience, or provider metadata does not match provider expectations. | Verify keystore material, `key-alias`, `password-ref`, `key-password-ref`, token URI, and provider-specific custom claims. Enable token diagnostics for safe metadata. |
| JWT bearer assertion is rejected | Issuer, subject, audience, signing key, token lifetime, or provider-specific claims are invalid. | Verify `issuer`, `subject`, `audience`, `token-lifetime`, custom claims, and signing keystore. |
| `Access token does not contain expected scopes` | The returned token does not include every expected scope. | Align requested scopes in the Spring registration with `clients.<name>.scopes` or `AccessTokenClient` expected scopes. |

## Keystores And SSL

| Symptom | Likely cause | Fix |
|---|---|---|
| Keystore fails to load | Wrong `type`, invalid `location` or `base64`, or wrong store password. | Check `authentication.key-stores.<id>.type`, `location` or `base64`, and `password-ref`. |
| Private key cannot be recovered | Wrong alias or key password. | Check `key-alias` and `key-password-ref`. If omitted, key password falls back to the store password. |
| mTLS handshake fails | Truststore, client certificate, or hostname verification is wrong. | Verify `apache.ssl.trust-store-id`, `apache.ssl.key-store-id`, certificate chain, and hostname verification setting. |
| Base64 keystore works in scripts but not in the app | The app is using a different value, whitespace handling, type, alias, or password reference. | Compare environment variables, `type`, `key-alias`, `password-ref`, and `key-password-ref`. Keep all secret values as placeholders in YAML. |

## Token Cache

| Symptom | Likely cause | Fix |
|---|---|---|
| Access token is reused when a fresh one is expected | Token cache is enabled for the grant type. | Set `smbtech.rest-clients.authentication.token-cache.client-credentials=false` or `jwt-bearer=false`. |
| JWT bearer token is reused across equivalent dynamic claims | This is expected when JWT bearer token cache is enabled. Dynamic claims are part of the cache identity. | Disable JWT bearer cache if every authorization must fetch a new token. |
| Different dynamic JWT bearer claims still appear to reuse a token | The dynamic claims were not propagated or were sanitized out. | Check `RequestContextManager` scope, `request-context.jwt-bearer-claims`, and blocked claim names. |

## Request Context

| Symptom | Likely cause | Fix |
|---|---|---|
| Dynamic headers are not sent | Request context propagation is disabled or the header is blocked. | Check `request-context.enabled`, `request-context.headers`, blocked headers, and whether an existing request header already exists. |
| Dynamic JWT bearer claims are missing | JWT bearer claim propagation is disabled, scope is not active, or claim name is blocked. | Check `request-context.jwt-bearer-claims`, open the scope around the outbound call, and avoid reserved/sensitive claim names. |
| Context disappears in async code | `RequestContextScope` is thread-bound. | Open a new scope in the executor, scheduler, or async execution path. |

## Diagnostics

| Symptom | Likely cause | Fix |
|---|---|---|
| OAuth2 diagnostic logs are missing | Diagnostics are disabled or logging level/output hides the event. | Set `smbtech.rest-clients.authentication.diagnostics.enabled=true` and verify application logging output. |
| Claims are not visible in diagnostics | Claim logging is disabled or sensitive claims were redacted. | Set `include-claims=true`. Sensitive fields remain redacted by design. |
| `accessTokenPreview` is not visible | Token previews are disabled by default. | Set `include-token-preview=true`. Only a short prefix is emitted followed by `...<redacted>`. |
| Cache events are missing | Cache event diagnostics are disabled. | Set `include-cache-events=true`. |

## Error Handling

| Symptom | Likely cause | Fix |
|---|---|---|
| `getJsonErrorResponseAsObject(...)` fails | No decoder is attached or the body is not valid JSON for the target type. | Use clients created by the starter, verify `error-handling.include-body=true`, and check the target DTO. |
| Error response body is empty | Body capture is disabled or downstream returned an empty body. | Check `clients.<name>.error-handling.include-body=true`. |
| Audit body is truncated but exception body is complete | Audit truncation is independent from exception body capture. | Tune `audit.max-body-size`; use `HttpClientResponseException` for the complete captured error body. |

## Mock

| Symptom | Likely cause | Fix |
|---|---|---|
| Controller returns `404` | Mock key is missing, disabled, or not configured. | Check `smbtech.mocks.endpoints.<key>.enabled=true`. |
| `Mock file does not exist` | Wrong classpath or file location. | Use `classpath:mocks/name.json`, `file:/...`, or place the file under `src/main/resources/mocks`. |
| Outbound `RestClient` still calls the real service | `MockRestClientInterceptor` was not added, `X-Mock-Key` is missing, or no path fallback mock exists. | Add the interceptor manually or through `RestClientBuilderCustomizer`, and configure `default-headers.X-Mock-Key`. |
| Response body conversion fails | Mock JSON body does not match the target DTO. | Validate the mock file body against the controller response type. |
| Core boundary check fails | Spring/Jackson/RestClient import was added to `mock-core`. | Move adapter code to the starter. |

## Logging

| Symptom | Likely cause | Fix |
|---|---|---|
| Transaction id is missing from logs | Transaction filter is disabled or the request is not servlet-based. | Check `smbtech.logging.transaction.enabled=true` and that servlet APIs are present. |
| Incoming `X-Transaction-Id` is ignored | Incoming values are disabled or invalid. | Check `transaction.accept-incoming`, `transaction.max-length`, and allowed characters. |
| `traceId` or `spanId` is missing | No tracing bridge populated MDC. | Add/verify Micrometer tracing configuration. The logging starter copies MDC; it does not create tracing spans. |
| Logs are delayed or missing during shutdown | Async logging queue or flush settings need tuning. | Check `smbtech.logging.async.*` or disable async logging in deterministic tests. |

## See Also

- [REST Client Starter Guide](rest-client.md)
- [OAuth2 Troubleshooting](rest-client/oauth2-troubleshooting.md)
- [SSL and HTTPS](rest-client/ssl-keystore.md)
- [Token Cache and Scope Validation](rest-client/token-cache.md)
- [OAuth2 Token Diagnostics](rest-client/token-diagnostics.md)
- [Request Context Propagation](rest-client/request-context.md)
- [Mock Core and Starter](mock.md)
- [Logging Guide](logging.md)
