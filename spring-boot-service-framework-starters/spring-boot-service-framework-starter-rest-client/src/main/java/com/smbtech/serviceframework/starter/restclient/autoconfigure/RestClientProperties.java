package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;

@ConfigurationProperties(prefix = "smbtech.rest-clients")
public class RestClientProperties {

    private Map<String, Client> clients = new LinkedHashMap<>();
    private Authentication authentication = new Authentication();

    public Map<String, Client> getClients() {
        return clients;
    }

    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public static class Client {
        private boolean enabled = true;
        private String beanName;
        private String baseUrl;
        private ClientType clientType = ClientType.DEFAULT;
        private AuthenticationType authenticationType = AuthenticationType.NO_AUTH;
        private BasicAuthentication basicAuthentication = new BasicAuthentication();
        private String credentialTokenRequestorId;
        private String scopes;
        private Timeout timeout = new Timeout();
        private Pooling pooling = new Pooling();
        private Apache apache = new Apache();
        private ErrorHandling errorHandling = new ErrorHandling();
        private Observability observability = new Observability();
        private Resilience resilience = new Resilience();
        private Audit audit = new Audit();
        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBeanName() {
            return beanName;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public ClientType getClientType() {
            return clientType;
        }

        public void setClientType(ClientType clientType) {
            this.clientType = clientType;
        }

        public AuthenticationType getAuthenticationType() {
            return authenticationType;
        }

        public void setAuthenticationType(AuthenticationType authenticationType) {
            this.authenticationType = authenticationType;
        }

        public BasicAuthentication getBasicAuthentication() {
            return basicAuthentication;
        }

        public void setBasicAuthentication(BasicAuthentication basicAuthentication) {
            this.basicAuthentication = basicAuthentication;
        }

        public String getCredentialTokenRequestorId() {
            return credentialTokenRequestorId;
        }

        public void setCredentialTokenRequestorId(String credentialTokenRequestorId) {
            this.credentialTokenRequestorId = credentialTokenRequestorId;
        }

        public String getScopes() {
            return scopes;
        }

        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public void setTimeout(Timeout timeout) {
            this.timeout = timeout;
        }

        public Pooling getPooling() {
            return pooling;
        }

        public void setPooling(Pooling pooling) {
            this.pooling = pooling;
        }

        public Apache getApache() {
            return apache;
        }

        public void setApache(Apache apache) {
            this.apache = apache;
        }

        public ErrorHandling getErrorHandling() {
            return errorHandling;
        }

        public void setErrorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
        }

        public Observability getObservability() {
            return observability;
        }

        public void setObservability(Observability observability) {
            this.observability = observability;
        }

        public Resilience getResilience() {
            return resilience;
        }

        public void setResilience(Resilience resilience) {
            this.resilience = resilience;
        }

        public Audit getAudit() {
            return audit;
        }

        public void setAudit(Audit audit) {
            this.audit = audit;
        }

        public Map<String, String> getDefaultHeaders() {
            return defaultHeaders;
        }

        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
        }
    }

    public static class BasicAuthentication {
        private String username;
        private String usernameRef;
        private String password;
        private String passwordRef;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUsernameRef() {
            return usernameRef;
        }

        public void setUsernameRef(String usernameRef) {
            this.usernameRef = usernameRef;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordRef() {
            return passwordRef;
        }

        public void setPasswordRef(String passwordRef) {
            this.passwordRef = passwordRef;
        }
    }

    public static class Timeout {
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration connectionRequestTimeout = Duration.ofSeconds(2);
        private Duration responseTimeout = Duration.ofSeconds(15);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
        }

        public Duration getResponseTimeout() {
            return responseTimeout;
        }

        public void setResponseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
        }
    }

    public static class Pooling {
        private ConnectionReusePolicy connectionReusePolicy = ConnectionReusePolicy.DEFAULT;
        private Duration keepAlive = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 20;
        private boolean tcpKeepAlive;

        public ConnectionReusePolicy getConnectionReusePolicy() {
            return connectionReusePolicy;
        }

        public void setConnectionReusePolicy(ConnectionReusePolicy connectionReusePolicy) {
            this.connectionReusePolicy = connectionReusePolicy;
        }

        public Duration getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(Duration keepAlive) {
            this.keepAlive = keepAlive;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        public boolean isTcpKeepAlive() {
            return tcpKeepAlive;
        }

        public void setTcpKeepAlive(boolean tcpKeepAlive) {
            this.tcpKeepAlive = tcpKeepAlive;
        }
    }

    public static class Apache {
        private boolean hostnameVerificationEnabled = true;
        private Duration validateAfterInactivity = Duration.ofSeconds(5);
        private Duration connectionTimeToLive = Duration.ofMinutes(5);
        private Ssl ssl = new Ssl();

        public boolean isHostnameVerificationEnabled() {
            return hostnameVerificationEnabled;
        }

        public void setHostnameVerificationEnabled(boolean hostnameVerificationEnabled) {
            this.hostnameVerificationEnabled = hostnameVerificationEnabled;
        }

        public Duration getValidateAfterInactivity() {
            return validateAfterInactivity;
        }

        public void setValidateAfterInactivity(Duration validateAfterInactivity) {
            this.validateAfterInactivity = validateAfterInactivity;
        }

        public Duration getConnectionTimeToLive() {
            return connectionTimeToLive;
        }

        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
        }

        public Ssl getSsl() {
            return ssl;
        }

        public void setSsl(Ssl ssl) {
            this.ssl = ssl;
        }
    }

    public static class Ssl {
        private boolean enabled;
        private String trustStoreId;
        private String keyStoreId;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getTrustStoreId() {
            return trustStoreId;
        }

        public void setTrustStoreId(String trustStoreId) {
            this.trustStoreId = trustStoreId;
        }

        public String getKeyStoreId() {
            return keyStoreId;
        }

        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }
    }

    public static class Audit {
        private boolean enabled;
        private boolean includeRequest;
        private boolean includeResponse;
        private boolean includeHeaders;
        private boolean includeBody;
        private int maxBodySize = 4096;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeRequest() {
            return includeRequest;
        }

        public void setIncludeRequest(boolean includeRequest) {
            this.includeRequest = includeRequest;
        }

        public boolean isIncludeResponse() {
            return includeResponse;
        }

        public void setIncludeResponse(boolean includeResponse) {
            this.includeResponse = includeResponse;
        }

        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        public boolean isIncludeBody() {
            return includeBody;
        }

        public void setIncludeBody(boolean includeBody) {
            this.includeBody = includeBody;
        }

        public int getMaxBodySize() {
            return maxBodySize;
        }

        public void setMaxBodySize(int maxBodySize) {
            this.maxBodySize = maxBodySize;
        }
    }

    public static class ErrorHandling {
        private boolean enabled = true;
        private boolean includeBody = true;
        private boolean includeHeaders = true;
        private boolean includeNotificationMetadata = true;
        private String notificationCodePrefix = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";
        private int maxBodySize = 4096;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isIncludeBody() {
            return includeBody;
        }

        public void setIncludeBody(boolean includeBody) {
            this.includeBody = includeBody;
        }

        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        public boolean isIncludeNotificationMetadata() {
            return includeNotificationMetadata;
        }

        public void setIncludeNotificationMetadata(boolean includeNotificationMetadata) {
            this.includeNotificationMetadata = includeNotificationMetadata;
        }

        public String getNotificationCodePrefix() {
            return notificationCodePrefix;
        }

        public void setNotificationCodePrefix(String notificationCodePrefix) {
            this.notificationCodePrefix = notificationCodePrefix;
        }

        public int getMaxBodySize() {
            return maxBodySize;
        }

        public void setMaxBodySize(int maxBodySize) {
            this.maxBodySize = maxBodySize;
        }
    }

    public static class Observability {
        private boolean enabled = true;
        private String metricName = "smbtech.http.client.requests";
        private boolean includeUri;
        private boolean includeStatus = true;
        private boolean includeException = true;
        private Map<String, String> tags = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMetricName() {
            return metricName;
        }

        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        public boolean isIncludeUri() {
            return includeUri;
        }

        public void setIncludeUri(boolean includeUri) {
            this.includeUri = includeUri;
        }

        public boolean isIncludeStatus() {
            return includeStatus;
        }

        public void setIncludeStatus(boolean includeStatus) {
            this.includeStatus = includeStatus;
        }

        public boolean isIncludeException() {
            return includeException;
        }

        public void setIncludeException(boolean includeException) {
            this.includeException = includeException;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }

    public static class Resilience {
        private boolean enabled;
        private Retry retry = new Retry();
        private CircuitBreaker circuitBreaker = new CircuitBreaker();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Retry getRetry() {
            return retry;
        }

        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }

        public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }
    }

    public static class Retry {
        private boolean enabled;
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(100);
        private boolean retryOnServerErrors = true;
        private boolean retryOnExceptions = true;
        private Set<Integer> retryOnStatuses = new LinkedHashSet<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Duration getBackoff() {
            return backoff;
        }

        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }

        public boolean isRetryOnServerErrors() {
            return retryOnServerErrors;
        }

        public void setRetryOnServerErrors(boolean retryOnServerErrors) {
            this.retryOnServerErrors = retryOnServerErrors;
        }

        public boolean isRetryOnExceptions() {
            return retryOnExceptions;
        }

        public void setRetryOnExceptions(boolean retryOnExceptions) {
            this.retryOnExceptions = retryOnExceptions;
        }

        public Set<Integer> getRetryOnStatuses() {
            return retryOnStatuses;
        }

        public void setRetryOnStatuses(Set<Integer> retryOnStatuses) {
            this.retryOnStatuses = retryOnStatuses;
        }
    }

    public static class CircuitBreaker {
        private boolean enabled;
        private int failureThreshold = 3;
        private Duration openDuration = Duration.ofSeconds(30);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public Duration getOpenDuration() {
            return openDuration;
        }

        public void setOpenDuration(Duration openDuration) {
            this.openDuration = openDuration;
        }
    }

    public static class Authentication {
        private Map<String, Credential> credentials = new LinkedHashMap<>();
        private Map<String, ClientAssertion> clientAssertions = new LinkedHashMap<>();
        private Map<String, JwtBearer> jwtBearer = new LinkedHashMap<>();
        private Map<String, KeyStore> keyStores = new LinkedHashMap<>();

        public Map<String, Credential> getCredentials() {
            return credentials;
        }

        public void setCredentials(Map<String, Credential> credentials) {
            this.credentials = credentials;
        }

        public Map<String, ClientAssertion> getClientAssertions() {
            return clientAssertions;
        }

        public void setClientAssertions(Map<String, ClientAssertion> clientAssertions) {
            this.clientAssertions = clientAssertions;
        }

        public Map<String, JwtBearer> getJwtBearer() {
            return jwtBearer;
        }

        public void setJwtBearer(Map<String, JwtBearer> jwtBearer) {
            this.jwtBearer = jwtBearer;
        }

        public Map<String, KeyStore> getKeyStores() {
            return keyStores;
        }

        public void setKeyStores(Map<String, KeyStore> keyStores) {
            this.keyStores = keyStores;
        }
    }

    public static class ClientAssertion {
        private String keyStoreId;
        private Duration tokenLifetime = Duration.ofSeconds(60);
        private Map<String, Object> customClaims = new LinkedHashMap<>();

        public String getKeyStoreId() {
            return keyStoreId;
        }

        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }

        public Duration getTokenLifetime() {
            return tokenLifetime;
        }

        public void setTokenLifetime(Duration tokenLifetime) {
            this.tokenLifetime = tokenLifetime;
        }

        public Map<String, Object> getCustomClaims() {
            return customClaims;
        }

        public void setCustomClaims(Map<String, Object> customClaims) {
            this.customClaims = customClaims;
        }
    }

    public static class JwtBearer {
        private String keyStoreId;
        private String issuer;
        private String subject;
        private String audience;
        private Duration tokenLifetime = Duration.ofMinutes(5);
        private Map<String, Object> customClaims = new LinkedHashMap<>();

        public String getKeyStoreId() {
            return keyStoreId;
        }

        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }

        public Duration getTokenLifetime() {
            return tokenLifetime;
        }

        public void setTokenLifetime(Duration tokenLifetime) {
            this.tokenLifetime = tokenLifetime;
        }

        public Map<String, Object> getCustomClaims() {
            return customClaims;
        }

        public void setCustomClaims(Map<String, Object> customClaims) {
            this.customClaims = customClaims;
        }
    }

    public static class KeyStore {
        private String location;
        private String base64;
        private String type = "PKCS12";
        private String password;
        private String passwordRef;
        private String keyAlias;
        private String keyPassword;
        private String keyPasswordRef;

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPasswordRef() {
            return passwordRef;
        }

        public void setPasswordRef(String passwordRef) {
            this.passwordRef = passwordRef;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        public String getKeyPasswordRef() {
            return keyPasswordRef;
        }

        public void setKeyPasswordRef(String keyPasswordRef) {
            this.keyPasswordRef = keyPasswordRef;
        }
    }

    public static class Credential {
        private String value;
        private String base64;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getBase64() {
            return base64;
        }

        public void setBase64(String base64) {
            this.base64 = base64;
        }
    }
}
