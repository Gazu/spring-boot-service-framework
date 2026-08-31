package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import com.smbtech.serviceframework.logging.domain.EventType;
import com.smbtech.serviceframework.logging.domain.LogLevel;
import com.smbtech.serviceframework.logging.domain.StructuredEvent;
import com.smbtech.serviceframework.logging.port.in.StructuredLogger;
import com.smbtech.serviceframework.starter.restclient.autoconfigure.RestClientProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.jwt.Jwt;

/** Provides OAuth2 token diagnostics logger behavior. */
final class OAuth2TokenDiagnosticsLogger {

    private static final EventType DIAGNOSTIC_TYPE = EventType.named("OAUTH2_TOKEN_DIAGNOSTIC");

    private final StructuredLogger logger;
    private final RestClientProperties.Diagnostics properties;
    private final OAuth2TokenDiagnosticSanitizer sanitizer;

    /**
     * Creates a OAuth2 token diagnostics logger instance.
     *
     * @param logger logger value
     * @param properties properties value
     * @param sanitizer sanitizer value
     */
    public OAuth2TokenDiagnosticsLogger(
            StructuredLogger logger,
            RestClientProperties.Diagnostics properties,
            OAuth2TokenDiagnosticSanitizer sanitizer) {
        this.logger = Objects.requireNonNull(logger, "logger must not be null");
        this.properties = properties == null ? new RestClientProperties.Diagnostics() : properties;
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer must not be null");
    }

    /**
     * Performs the disabled operation.
     *
     * @return disabled result
     */
    public static OAuth2TokenDiagnosticsLogger disabled() {
        return new OAuth2TokenDiagnosticsLogger(
                new NoOpStructuredLogger(),
                new RestClientProperties.Diagnostics(),
                new OAuth2TokenDiagnosticSanitizer());
    }

    /**
     * Performs the token request started operation.
     *
     * @param registration registration value
     * @param authorizationAttributes authorization attributes value
     */
    public void tokenRequestStarted(
            ClientRegistration registration, Map<String, Object> authorizationAttributes) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> data = registrationData("TOKEN_REQUEST_STARTED", registration);
        data.put("scopes", registration.getScopes());
        data.put("tokenUri", registration.getProviderDetails().getTokenUri());
        if (properties.isIncludeClaims()
                && authorizationAttributes != null
                && !authorizationAttributes.isEmpty()) {
            data.put("authorizationAttributes", sanitizer.sanitize(authorizationAttributes));
        }
        info("OAuth2 token request started", data);
    }

    /**
     * Performs the token request succeeded operation.
     *
     * @param registration registration value
     * @param token token value
     * @param scopes scopes value
     */
    public void tokenRequestSucceeded(
            ClientRegistration registration, OAuth2AccessToken token, Set<String> scopes) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> data = registrationData("TOKEN_REQUEST_SUCCEEDED", registration);
        data.put(
                "tokenType",
                token.getTokenType() == null ? "Bearer" : token.getTokenType().getValue());
        data.put("scopes", scopes);
        data.put("expiresAt", token.getExpiresAt());
        if (token.getExpiresAt() != null) {
            data.put(
                    "secondsUntilExpiration",
                    Math.max(0, Duration.between(Instant.now(), token.getExpiresAt()).toSeconds()));
        }
        if (properties.isIncludeTokenPreview()) {
            data.put(
                    "accessTokenPreview",
                    sanitizer.preview(token.getTokenValue(), properties.getTokenPreviewLength()));
        }
        info("OAuth2 token request succeeded", data);
    }

    /**
     * Performs the token request failed operation.
     *
     * @param registration registration value
     * @param exception exception value
     */
    public void tokenRequestFailed(ClientRegistration registration, Throwable exception) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> data = registrationData("TOKEN_REQUEST_FAILED", registration);
        data.put("errorType", exception.getClass().getSimpleName());
        data.put("message", sanitizer.sanitizeText(exception.getMessage()));
        warn("OAuth2 token request failed", data);
    }

    /**
     * Performs the token request failed operation.
     *
     * @param registrationId registration id value
     * @param grantName grant name value
     * @param exception exception value
     */
    public void tokenRequestFailed(String registrationId, String grantName, Throwable exception) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", "TOKEN_REQUEST_FAILED");
        data.put("registrationId", registrationId);
        data.put("grantType", grantName);
        data.put("errorType", exception.getClass().getSimpleName());
        data.put("message", sanitizer.sanitizeText(exception.getMessage()));
        warn("OAuth2 token request failed", data);
    }

    /**
     * Performs the JWT bearer assertion created operation.
     *
     * @param registrationId registration id value
     * @param jwt generated JWT
     */
    public void jwtBearerAssertionCreated(String registrationId, Jwt jwt) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> claims = jwt.getClaims();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", "JWT_BEARER_ASSERTION_CREATED");
        data.put("registrationId", registrationId);
        data.put("grantType", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        data.put("algorithm", jwt.getHeaders().get("alg"));
        data.put("keyId", jwt.getHeaders().get("kid"));
        data.put("issuer", claims.get("iss"));
        data.put("subject", claims.get("sub"));
        data.put("audience", claims.get("aud"));
        data.put("issuedAt", claims.get("iat"));
        data.put("expiresAt", claims.get("exp"));
        if (properties.isIncludeClaims()) {
            data.put("claims", sanitizer.sanitize(claims));
        }
        if (properties.isIncludeTokenPreview()) {
            data.put(
                    "assertionPreview",
                    sanitizer.preview(jwt.getTokenValue(), properties.getTokenPreviewLength()));
        }
        info("JWT bearer assertion created", data);
    }

    /**
     * Performs the client assertion created operation.
     *
     * @param registrationId registration id value
     * @param tokenLifetime token lifetime value
     * @param customClaims custom claims value
     */
    public void clientAssertionCreated(
            String registrationId, Duration tokenLifetime, Map<String, Object> customClaims) {
        if (!isEnabled()) {
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", "CLIENT_ASSERTION_CREATED");
        data.put("registrationId", registrationId);
        data.put("clientAuthenticationMethod", "private_key_jwt");
        data.put("expiresInSeconds", tokenLifetime == null ? null : tokenLifetime.toSeconds());
        if (properties.isIncludeClaims()) {
            data.put("customClaims", sanitizer.sanitize(customClaims));
        }
        info("OAuth2 client assertion created", data);
    }

    /**
     * Performs the token cache hit operation.
     *
     * @param registration registration value
     * @param principalName principal name value
     * @param details details value
     */
    public void tokenCacheHit(
            ClientRegistration registration,
            String principalName,
            OAuth2AuthorizedClientDetails details) {
        cacheEvent("TOKEN_CACHE_HIT", registration, principalName, details);
    }

    /**
     * Performs the token cache miss operation.
     *
     * @param registration registration value
     * @param principalName principal name value
     */
    public void tokenCacheMiss(ClientRegistration registration, String principalName) {
        cacheEvent("TOKEN_CACHE_MISS", registration, principalName, null);
    }

    /**
     * Performs the token cache skipped operation.
     *
     * @param registration registration value
     * @param principalName principal name value
     */
    public void tokenCacheSkipped(ClientRegistration registration, String principalName) {
        cacheEvent("TOKEN_CACHE_SKIPPED", registration, principalName, null);
    }

    /**
     * Performs the token cache saved operation.
     *
     * @param registration registration value
     * @param principalName principal name value
     * @param details details value
     */
    public void tokenCacheSaved(
            ClientRegistration registration,
            String principalName,
            OAuth2AuthorizedClientDetails details) {
        cacheEvent("TOKEN_CACHE_SAVED", registration, principalName, details);
    }

    /**
     * Carries immutable OAuth2 authorized client details data.
     *
     * @param expiresAt expires at value
     */
    public record OAuth2AuthorizedClientDetails(Instant expiresAt) {}

    private void cacheEvent(
            String event,
            ClientRegistration registration,
            String principalName,
            OAuth2AuthorizedClientDetails details) {
        if (!isEnabled() || !properties.isIncludeCacheEvents()) {
            return;
        }
        Map<String, Object> data = registrationData(event, registration);
        data.put("principalName", principalName);
        if (details != null && details.expiresAt() != null) {
            data.put("expiresAt", details.expiresAt());
            data.put(
                    "secondsUntilExpiration",
                    Math.max(0, Duration.between(Instant.now(), details.expiresAt()).toSeconds()));
        }
        info("OAuth2 token cache event", data);
    }

    private boolean isEnabled() {
        return properties.isEnabled() && logger.isEnabled(LogLevel.INFO, DIAGNOSTIC_TYPE);
    }

    private Map<String, Object> registrationData(String event, ClientRegistration registration) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("event", event);
        data.put("registrationId", registration.getRegistrationId());
        data.put("grantType", registration.getAuthorizationGrantType().getValue());
        data.put(
                "clientAuthenticationMethod",
                registration.getClientAuthenticationMethod().getValue());
        return data;
    }

    private void info(String message, Map<String, Object> data) {
        logger.info(event -> populate(event, message, data));
    }

    private void warn(String message, Map<String, Object> data) {
        logger.warn(event -> populate(event, message, data));
    }

    private StructuredEvent.Builder populate(
            StructuredEvent.Builder event, String message, Map<String, Object> data) {
        event.type(DIAGNOSTIC_TYPE).message(message).tag("oauth2").tag("diagnostic");
        data.forEach(
                (key, value) -> {
                    if (value != null) {
                        event.with(key, value);
                    }
                });
        return event;
    }

    private static final class NoOpStructuredLogger implements StructuredLogger {

        @Override
        public boolean isEnabled(LogLevel level, EventType eventType) {
            return false;
        }

        @Override
        public void log(LogLevel level, StructuredEvent event) {}
    }
}
