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

## Pull Request CI

| Symptom | Likely cause | Fix |
|---|---|---|
| `Pull Request / Policy` fails | The PR title or a non-merge commit does not follow English Conventional Commits, or the workflow no longer matches the versioned contract. | Fix the title and commit subjects, then run `./gradlew validatePullRequestCiContract`. |
| `Pull Request / Quality` fails | Compilation, tests, coverage, formatting, documentation, compatibility, consumers, AOT, or supply-chain validation failed. | Download the Quality artifact, inspect JUnit and JaCoCo XML, then reproduce with `./gradlew clean pullRequestGate --stacktrace`. |
| `Pull Request / Security` fails | Gitleaks found a potential secret, or Trivy found a blocking vulnerability or misconfiguration. | Download the redacted Security artifact, review its SARIF and summary, remediate the finding, and regenerate the SBOM when dependencies changed. |
| A required check remains missing | The branch does not contain `.github/workflows/pull-request.yml`, the PR targets another branch, or the workflow was skipped or cancelled. | Rebase onto current `main`, confirm the workflow file and target branch, then rerun failed jobs from GitHub Actions. |
| GitHub reports that the branch is out of date | Strict status checks require testing the current merge result with `main`. | Rebase the branch on `main` and push the updated commits. Previous approvals may be dismissed. |
| Approval is present but merge remains blocked | The approval became stale, a conversation is unresolved, or the last pusher supplied the final approval. | Resolve conversations and request a fresh approval from someone other than the last pusher. |
| GitHub rejects a merge commit | Protected `main` requires linear history. | Use squash merge or rebase merge. |
| A workflow artifact is missing | The producing step failed before creating evidence, or the seven-day retention period expired. | Reproduce locally or rerun the workflow. Artifacts intentionally exclude compiled binaries and local Maven repositories. |
| Live branch settings differ from the contract | A maintainer changed branch protection or repository merge settings outside the reviewed rollout. | Run `./gradlew verifyPullRequestBranchProtection`, review the reported difference, and restore GitHub settings or update the contract through a pull request. |

## Gradle And Publication

| Symptom | Likely cause | Fix |
|---|---|---|
| Gradle cannot resolve a framework artifact | Artifacts were not published to Maven local or the module-local repositories. | Run `./gradlew publishToMavenLocal` for Maven local consumption, or `./gradlew publishLocalArtifacts` for standalone examples. |
| An OpenAPI configuration, generation, publication, compatibility, mock, or scaffolding task fails | The first failing OpenAPI task identifies the affected layer. | Follow [OpenAPI Troubleshooting](openapi/troubleshooting.md) for task isolation, report locations, exact messages, and recovery. |
| Standalone examples still use old framework code | Example repositories point to module-local `build/repository` artifacts that were not regenerated. | Run `./gradlew publishLocalArtifacts`, then rerun the example test. |
| `documentationCheck` fails on generated property references | A `@ConfigurationProperties` class changed but generated docs were not refreshed. | Run `./gradlew generatePropertyReferences`, review the generated docs, then rerun `./gradlew documentationCheck`. |
| `documentationCheck` fails on a Markdown link or anchor | A linked file is missing or a `#heading-anchor` does not match a real heading. | Fix the target path or heading, then rerun `./gradlew documentationCheck`. |
| `documentationCheck` fails on documentation catalog or release docs | A required canonical document, index link, changelog section, or release checklist reference is missing. | Update `README.md`, `docs/index.md`, `docs/documentation-architecture.md`, `CHANGELOG.md`, or `docs/releasing.md` as reported. |
| `documentationCheck` fails on framework versions | A dependency snippet, example build, release snippet, or current-status line references an older framework version. | Align the version with the root `build.gradle` project version. |
| A module compatibility contract is stale | A supported public type, extension point, property, auto-configuration import, or Gradle plugin id changed. | Run `./gradlew generateModuleCompatibilityContracts`, review the contract diff, document intentional breaking changes, and rerun the focused `<module>CompatibilityCheck`. |
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
| Outbound `RestClient` still calls the real service | The `mockRestClientInterceptor` bean was not added, `X-Mock-Key` is missing, or no path fallback mock exists. | Inject the qualified `ClientHttpRequestInterceptor`, add it manually or through `RestClientBuilderCustomizer`, and configure `default-headers.X-Mock-Key`. |
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
