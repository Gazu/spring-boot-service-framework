package com.smbtech.serviceframework.starter.restclient.autoconfigure;

import com.smbtech.serviceframework.httpclient.domain.AuthenticationType;
import com.smbtech.serviceframework.httpclient.domain.ClientType;
import com.smbtech.serviceframework.httpclient.domain.ConnectionReusePolicy;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Provides rest client properties behavior. */
@ConfigurationProperties(prefix = "smbtech.rest-clients")
public class RestClientProperties {
    /** Creates a rest client properties instance. */
    public RestClientProperties() {}

    private Map<String, Client> clients = new LinkedHashMap<>();
    private Authentication authentication = new Authentication();
    private Validation validation = new Validation();
    private RequestContext requestContext = new RequestContext();

    /**
     * Returns the configured clients.
     *
     * @return get clients result
     */
    public Map<String, Client> getClients() {
        return clients;
    }

    /**
     * Sets the configured clients.
     *
     * @param clients clients value
     */
    public void setClients(Map<String, Client> clients) {
        this.clients = clients;
    }

    /**
     * Returns the configured authentication.
     *
     * @return get authentication result
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * Sets the configured authentication.
     *
     * @param authentication authentication value
     */
    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Returns the configured validation.
     *
     * @return get validation result
     */
    public Validation getValidation() {
        return validation;
    }

    /**
     * Sets the configured validation.
     *
     * @param validation validation value
     */
    public void setValidation(Validation validation) {
        this.validation = validation;
    }

    /**
     * Returns the configured request context.
     *
     * @return get request context result
     */
    public RequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * Sets the configured request context.
     *
     * @param requestContext request context value
     */
    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /** Provides request context behavior. */
    public static class RequestContext {
        /** Creates a request context instance. */
        public RequestContext() {}

        private boolean enabled = true;
        private boolean headers = true;
        private boolean jwtBearerClaims = true;
        private Set<String> blockedHeaders = new LinkedHashSet<>();
        private Set<String> blockedJwtBearerClaims = new LinkedHashSet<>();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether headers.
         *
         * @return is headers result
         */
        public boolean isHeaders() {
            return headers;
        }

        /**
         * Sets the configured headers.
         *
         * @param headers headers value
         */
        public void setHeaders(boolean headers) {
            this.headers = headers;
        }

        /**
         * Reports whether JWT bearer claims.
         *
         * @return is JWT bearer claims result
         */
        public boolean isJwtBearerClaims() {
            return jwtBearerClaims;
        }

        /**
         * Sets the configured JWT bearer claims.
         *
         * @param jwtBearerClaims JWT bearer claims value
         */
        public void setJwtBearerClaims(boolean jwtBearerClaims) {
            this.jwtBearerClaims = jwtBearerClaims;
        }

        /**
         * Returns the configured blocked headers.
         *
         * @return get blocked headers result
         */
        public Set<String> getBlockedHeaders() {
            return blockedHeaders;
        }

        /**
         * Sets the configured blocked headers.
         *
         * @param blockedHeaders blocked headers value
         */
        public void setBlockedHeaders(Set<String> blockedHeaders) {
            this.blockedHeaders = blockedHeaders;
        }

        /**
         * Returns the configured blocked JWT bearer claims.
         *
         * @return get blocked JWT bearer claims result
         */
        public Set<String> getBlockedJwtBearerClaims() {
            return blockedJwtBearerClaims;
        }

        /**
         * Sets the configured blocked JWT bearer claims.
         *
         * @param blockedJwtBearerClaims blocked JWT bearer claims value
         */
        public void setBlockedJwtBearerClaims(Set<String> blockedJwtBearerClaims) {
            this.blockedJwtBearerClaims = blockedJwtBearerClaims;
        }
    }

    /** Provides client behavior. */
    public static class Client {
        /** Creates a client instance. */
        public Client() {}

        private boolean enabled = true;
        private String beanName;
        private String baseUrl;
        private ClientType clientType = ClientType.DEFAULT;
        private AuthenticationType authenticationType = AuthenticationType.NO_AUTH;
        private BasicAuthentication basicAuthentication = new BasicAuthentication();
        private String tokenRequestId;
        private String scopes;
        private Timeout timeout = new Timeout();
        private Pooling pooling = new Pooling();
        private Apache apache = new Apache();
        private ErrorHandling errorHandling = new ErrorHandling();
        private Observability observability = new Observability();
        private Resilience resilience = new Resilience();
        private Audit audit = new Audit();
        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured bean name.
         *
         * @return get bean name result
         */
        public String getBeanName() {
            return beanName;
        }

        /**
         * Sets the configured bean name.
         *
         * @param beanName bean name value
         */
        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }

        /**
         * Returns the configured base url.
         *
         * @return get base url result
         */
        public String getBaseUrl() {
            return baseUrl;
        }

        /**
         * Sets the configured base url.
         *
         * @param baseUrl base url value
         */
        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * Returns the configured client type.
         *
         * @return get client type result
         */
        public ClientType getClientType() {
            return clientType;
        }

        /**
         * Sets the configured client type.
         *
         * @param clientType client type value
         */
        public void setClientType(ClientType clientType) {
            this.clientType = clientType;
        }

        /**
         * Returns the configured authentication type.
         *
         * @return get authentication type result
         */
        public AuthenticationType getAuthenticationType() {
            return authenticationType;
        }

        /**
         * Sets the configured authentication type.
         *
         * @param authenticationType authentication type value
         */
        public void setAuthenticationType(AuthenticationType authenticationType) {
            this.authenticationType = authenticationType;
        }

        /**
         * Returns the configured basic authentication.
         *
         * @return get basic authentication result
         */
        public BasicAuthentication getBasicAuthentication() {
            return basicAuthentication;
        }

        /**
         * Sets the configured basic authentication.
         *
         * @param basicAuthentication basic authentication value
         */
        public void setBasicAuthentication(BasicAuthentication basicAuthentication) {
            this.basicAuthentication = basicAuthentication;
        }

        /**
         * Returns the configured token request id.
         *
         * @return get token request id result
         */
        public String getTokenRequestId() {
            return tokenRequestId;
        }

        /**
         * Sets the configured token request id.
         *
         * @param tokenRequestId token request id value
         */
        public void setTokenRequestId(String tokenRequestId) {
            this.tokenRequestId = tokenRequestId;
        }

        /**
         * Returns the configured scopes.
         *
         * @return get scopes result
         */
        public String getScopes() {
            return scopes;
        }

        /**
         * Sets the configured scopes.
         *
         * @param scopes scopes value
         */
        public void setScopes(String scopes) {
            this.scopes = scopes;
        }

        /**
         * Returns the configured timeout.
         *
         * @return get timeout result
         */
        public Timeout getTimeout() {
            return timeout;
        }

        /**
         * Sets the configured timeout.
         *
         * @param timeout timeout value
         */
        public void setTimeout(Timeout timeout) {
            this.timeout = timeout;
        }

        /**
         * Returns the configured pooling.
         *
         * @return get pooling result
         */
        public Pooling getPooling() {
            return pooling;
        }

        /**
         * Sets the configured pooling.
         *
         * @param pooling pooling value
         */
        public void setPooling(Pooling pooling) {
            this.pooling = pooling;
        }

        /**
         * Returns the configured apache.
         *
         * @return get apache result
         */
        public Apache getApache() {
            return apache;
        }

        /**
         * Sets the configured apache.
         *
         * @param apache apache value
         */
        public void setApache(Apache apache) {
            this.apache = apache;
        }

        /**
         * Returns the configured error handling.
         *
         * @return get error handling result
         */
        public ErrorHandling getErrorHandling() {
            return errorHandling;
        }

        /**
         * Sets the configured error handling.
         *
         * @param errorHandling error handling value
         */
        public void setErrorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
        }

        /**
         * Returns the configured observability.
         *
         * @return get observability result
         */
        public Observability getObservability() {
            return observability;
        }

        /**
         * Sets the configured observability.
         *
         * @param observability observability value
         */
        public void setObservability(Observability observability) {
            this.observability = observability;
        }

        /**
         * Returns the configured resilience.
         *
         * @return get resilience result
         */
        public Resilience getResilience() {
            return resilience;
        }

        /**
         * Sets the configured resilience.
         *
         * @param resilience resilience value
         */
        public void setResilience(Resilience resilience) {
            this.resilience = resilience;
        }

        /**
         * Returns the configured audit.
         *
         * @return get audit result
         */
        public Audit getAudit() {
            return audit;
        }

        /**
         * Sets the configured audit.
         *
         * @param audit audit value
         */
        public void setAudit(Audit audit) {
            this.audit = audit;
        }

        /**
         * Returns the configured default headers.
         *
         * @return get default headers result
         */
        public Map<String, String> getDefaultHeaders() {
            return defaultHeaders;
        }

        /**
         * Sets the configured default headers.
         *
         * @param defaultHeaders default headers value
         */
        public void setDefaultHeaders(Map<String, String> defaultHeaders) {
            this.defaultHeaders = defaultHeaders;
        }
    }

    /** Provides basic authentication behavior. */
    public static class BasicAuthentication {
        /** Creates a basic authentication instance. */
        public BasicAuthentication() {}

        private String username;
        private String usernameRef;
        private String password;
        private String passwordRef;

        /**
         * Returns the configured username.
         *
         * @return get username result
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets the configured username.
         *
         * @param username username value
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Returns the configured username ref.
         *
         * @return get username ref result
         */
        public String getUsernameRef() {
            return usernameRef;
        }

        /**
         * Sets the configured username ref.
         *
         * @param usernameRef username ref value
         */
        public void setUsernameRef(String usernameRef) {
            this.usernameRef = usernameRef;
        }

        /**
         * Returns the configured password.
         *
         * @return get password result
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the configured password.
         *
         * @param password password value
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Returns the configured password ref.
         *
         * @return get password ref result
         */
        public String getPasswordRef() {
            return passwordRef;
        }

        /**
         * Sets the configured password ref.
         *
         * @param passwordRef password ref value
         */
        public void setPasswordRef(String passwordRef) {
            this.passwordRef = passwordRef;
        }
    }

    /** Provides timeout behavior. */
    public static class Timeout {
        /** Creates a timeout instance. */
        public Timeout() {}

        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration connectionRequestTimeout = Duration.ofSeconds(2);
        private Duration responseTimeout = Duration.ofSeconds(15);

        /**
         * Returns the configured connect timeout.
         *
         * @return get connect timeout result
         */
        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        /**
         * Sets the configured connect timeout.
         *
         * @param connectTimeout connect timeout value
         */
        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        /**
         * Returns the configured connection request timeout.
         *
         * @return get connection request timeout result
         */
        public Duration getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }

        /**
         * Sets the configured connection request timeout.
         *
         * @param connectionRequestTimeout connection request timeout value
         */
        public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
        }

        /**
         * Returns the configured response timeout.
         *
         * @return get response timeout result
         */
        public Duration getResponseTimeout() {
            return responseTimeout;
        }

        /**
         * Sets the configured response timeout.
         *
         * @param responseTimeout response timeout value
         */
        public void setResponseTimeout(Duration responseTimeout) {
            this.responseTimeout = responseTimeout;
        }
    }

    /** Provides pooling behavior. */
    public static class Pooling {
        /** Creates a pooling instance. */
        public Pooling() {}

        private ConnectionReusePolicy connectionReusePolicy = ConnectionReusePolicy.DEFAULT;
        private Duration keepAlive = Duration.ofSeconds(30);
        private int maxConnections = 100;
        private int maxConnectionsPerRoute = 20;
        private boolean tcpKeepAlive;

        /**
         * Returns the configured connection reuse policy.
         *
         * @return get connection reuse policy result
         */
        public ConnectionReusePolicy getConnectionReusePolicy() {
            return connectionReusePolicy;
        }

        /**
         * Sets the configured connection reuse policy.
         *
         * @param connectionReusePolicy connection reuse policy value
         */
        public void setConnectionReusePolicy(ConnectionReusePolicy connectionReusePolicy) {
            this.connectionReusePolicy = connectionReusePolicy;
        }

        /**
         * Returns the configured keep alive.
         *
         * @return get keep alive result
         */
        public Duration getKeepAlive() {
            return keepAlive;
        }

        /**
         * Sets the configured keep alive.
         *
         * @param keepAlive keep alive value
         */
        public void setKeepAlive(Duration keepAlive) {
            this.keepAlive = keepAlive;
        }

        /**
         * Returns the configured max connections.
         *
         * @return get max connections result
         */
        public int getMaxConnections() {
            return maxConnections;
        }

        /**
         * Sets the configured max connections.
         *
         * @param maxConnections max connections value
         */
        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        /**
         * Returns the configured max connections per route.
         *
         * @return get max connections per route result
         */
        public int getMaxConnectionsPerRoute() {
            return maxConnectionsPerRoute;
        }

        /**
         * Sets the configured max connections per route.
         *
         * @param maxConnectionsPerRoute max connections per route value
         */
        public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
        }

        /**
         * Reports whether tcp keep alive.
         *
         * @return is tcp keep alive result
         */
        public boolean isTcpKeepAlive() {
            return tcpKeepAlive;
        }

        /**
         * Sets the configured tcp keep alive.
         *
         * @param tcpKeepAlive tcp keep alive value
         */
        public void setTcpKeepAlive(boolean tcpKeepAlive) {
            this.tcpKeepAlive = tcpKeepAlive;
        }
    }

    /** Provides apache behavior. */
    public static class Apache {
        /** Creates an Apache instance. */
        public Apache() {}

        private boolean hostnameVerificationEnabled = true;
        private Duration validateAfterInactivity = Duration.ofSeconds(5);
        private Duration connectionTimeToLive = Duration.ofMinutes(5);
        private Ssl ssl = new Ssl();

        /**
         * Reports whether hostname verification enabled.
         *
         * @return is hostname verification enabled result
         */
        public boolean isHostnameVerificationEnabled() {
            return hostnameVerificationEnabled;
        }

        /**
         * Sets the configured hostname verification enabled.
         *
         * @param hostnameVerificationEnabled hostname verification enabled value
         */
        public void setHostnameVerificationEnabled(boolean hostnameVerificationEnabled) {
            this.hostnameVerificationEnabled = hostnameVerificationEnabled;
        }

        /**
         * Returns the configured validate after inactivity.
         *
         * @return get validate after inactivity result
         */
        public Duration getValidateAfterInactivity() {
            return validateAfterInactivity;
        }

        /**
         * Sets the configured validate after inactivity.
         *
         * @param validateAfterInactivity validate after inactivity value
         */
        public void setValidateAfterInactivity(Duration validateAfterInactivity) {
            this.validateAfterInactivity = validateAfterInactivity;
        }

        /**
         * Returns the configured connection time to live.
         *
         * @return get connection time to live result
         */
        public Duration getConnectionTimeToLive() {
            return connectionTimeToLive;
        }

        /**
         * Sets the configured connection time to live.
         *
         * @param connectionTimeToLive connection time to live value
         */
        public void setConnectionTimeToLive(Duration connectionTimeToLive) {
            this.connectionTimeToLive = connectionTimeToLive;
        }

        /**
         * Returns the configured ssl.
         *
         * @return get ssl result
         */
        public Ssl getSsl() {
            return ssl;
        }

        /**
         * Sets the configured ssl.
         *
         * @param ssl ssl value
         */
        public void setSsl(Ssl ssl) {
            this.ssl = ssl;
        }
    }

    /** Provides ssl behavior. */
    public static class Ssl {
        /** Creates an SSL instance. */
        public Ssl() {}

        private boolean enabled;
        private String trustStoreId;
        private String keyStoreId;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured trust store id.
         *
         * @return get trust store id result
         */
        public String getTrustStoreId() {
            return trustStoreId;
        }

        /**
         * Sets the configured trust store id.
         *
         * @param trustStoreId trust store id value
         */
        public void setTrustStoreId(String trustStoreId) {
            this.trustStoreId = trustStoreId;
        }

        /**
         * Returns the configured key store id.
         *
         * @return get key store id result
         */
        public String getKeyStoreId() {
            return keyStoreId;
        }

        /**
         * Sets the configured key store id.
         *
         * @param keyStoreId key store id value
         */
        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }
    }

    /** Provides audit behavior. */
    public static class Audit {
        /** Creates a audit instance. */
        public Audit() {}

        private boolean enabled;
        private boolean includeRequest;
        private boolean includeResponse;
        private boolean includeHeaders;
        private boolean includeBody;
        private int maxBodySize = 4096;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether include request.
         *
         * @return is include request result
         */
        public boolean isIncludeRequest() {
            return includeRequest;
        }

        /**
         * Sets the configured include request.
         *
         * @param includeRequest include request value
         */
        public void setIncludeRequest(boolean includeRequest) {
            this.includeRequest = includeRequest;
        }

        /**
         * Reports whether include response.
         *
         * @return is include response result
         */
        public boolean isIncludeResponse() {
            return includeResponse;
        }

        /**
         * Sets the configured include response.
         *
         * @param includeResponse include response value
         */
        public void setIncludeResponse(boolean includeResponse) {
            this.includeResponse = includeResponse;
        }

        /**
         * Reports whether include headers.
         *
         * @return is include headers result
         */
        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        /**
         * Sets the configured include headers.
         *
         * @param includeHeaders include headers value
         */
        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        /**
         * Reports whether include body.
         *
         * @return is include body result
         */
        public boolean isIncludeBody() {
            return includeBody;
        }

        /**
         * Sets the configured include body.
         *
         * @param includeBody include body value
         */
        public void setIncludeBody(boolean includeBody) {
            this.includeBody = includeBody;
        }

        /**
         * Returns the configured max body size.
         *
         * @return get max body size result
         */
        public int getMaxBodySize() {
            return maxBodySize;
        }

        /**
         * Sets the configured max body size.
         *
         * @param maxBodySize max body size value
         */
        public void setMaxBodySize(int maxBodySize) {
            this.maxBodySize = maxBodySize;
        }
    }

    /** Provides error handling behavior. */
    public static class ErrorHandling {
        /** Creates a error handling instance. */
        public ErrorHandling() {}

        private boolean enabled = true;
        private boolean includeBody = true;
        private boolean includeHeaders = true;
        private boolean includeNotificationMetadata = true;
        private String notificationCodePrefix = "E_SERVICE_FRAMEWORK_HTTP_CLIENT_";
        private int maxBodySize = 4096;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether include body.
         *
         * @return is include body result
         */
        public boolean isIncludeBody() {
            return includeBody;
        }

        /**
         * Sets the configured include body.
         *
         * @param includeBody include body value
         */
        public void setIncludeBody(boolean includeBody) {
            this.includeBody = includeBody;
        }

        /**
         * Reports whether include headers.
         *
         * @return is include headers result
         */
        public boolean isIncludeHeaders() {
            return includeHeaders;
        }

        /**
         * Sets the configured include headers.
         *
         * @param includeHeaders include headers value
         */
        public void setIncludeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
        }

        /**
         * Reports whether include notification metadata.
         *
         * @return is include notification metadata result
         */
        public boolean isIncludeNotificationMetadata() {
            return includeNotificationMetadata;
        }

        /**
         * Sets the configured include notification metadata.
         *
         * @param includeNotificationMetadata include notification metadata value
         */
        public void setIncludeNotificationMetadata(boolean includeNotificationMetadata) {
            this.includeNotificationMetadata = includeNotificationMetadata;
        }

        /**
         * Returns the configured notification code prefix.
         *
         * @return get notification code prefix result
         */
        public String getNotificationCodePrefix() {
            return notificationCodePrefix;
        }

        /**
         * Sets the configured notification code prefix.
         *
         * @param notificationCodePrefix notification code prefix value
         */
        public void setNotificationCodePrefix(String notificationCodePrefix) {
            this.notificationCodePrefix = notificationCodePrefix;
        }

        /**
         * Returns the configured max body size.
         *
         * @return get max body size result
         */
        public int getMaxBodySize() {
            return maxBodySize;
        }

        /**
         * Sets the configured max body size.
         *
         * @param maxBodySize max body size value
         */
        public void setMaxBodySize(int maxBodySize) {
            this.maxBodySize = maxBodySize;
        }
    }

    /** Provides observability behavior. */
    public static class Observability {
        /** Creates a observability instance. */
        public Observability() {}

        private boolean enabled = true;
        private String metricName = "smbtech.http.client.requests";
        private boolean includeUri;
        private boolean includeStatus = true;
        private boolean includeException = true;
        private Map<String, String> tags = new LinkedHashMap<>();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured metric name.
         *
         * @return get metric name result
         */
        public String getMetricName() {
            return metricName;
        }

        /**
         * Sets the configured metric name.
         *
         * @param metricName metric name value
         */
        public void setMetricName(String metricName) {
            this.metricName = metricName;
        }

        /**
         * Reports whether include uri.
         *
         * @return is include uri result
         */
        public boolean isIncludeUri() {
            return includeUri;
        }

        /**
         * Sets the configured include uri.
         *
         * @param includeUri include uri value
         */
        public void setIncludeUri(boolean includeUri) {
            this.includeUri = includeUri;
        }

        /**
         * Reports whether include status.
         *
         * @return is include status result
         */
        public boolean isIncludeStatus() {
            return includeStatus;
        }

        /**
         * Sets the configured include status.
         *
         * @param includeStatus include status value
         */
        public void setIncludeStatus(boolean includeStatus) {
            this.includeStatus = includeStatus;
        }

        /**
         * Reports whether include exception.
         *
         * @return is include exception result
         */
        public boolean isIncludeException() {
            return includeException;
        }

        /**
         * Sets the configured include exception.
         *
         * @param includeException include exception value
         */
        public void setIncludeException(boolean includeException) {
            this.includeException = includeException;
        }

        /**
         * Returns the configured tags.
         *
         * @return get tags result
         */
        public Map<String, String> getTags() {
            return tags;
        }

        /**
         * Sets the configured tags.
         *
         * @param tags tags value
         */
        public void setTags(Map<String, String> tags) {
            this.tags = tags;
        }
    }

    /** Provides resilience behavior. */
    public static class Resilience {
        /** Creates a resilience instance. */
        public Resilience() {}

        private boolean enabled;
        private Retry retry = new Retry();
        private CircuitBreaker circuitBreaker = new CircuitBreaker();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured retry.
         *
         * @return get retry result
         */
        public Retry getRetry() {
            return retry;
        }

        /**
         * Sets the configured retry.
         *
         * @param retry retry value
         */
        public void setRetry(Retry retry) {
            this.retry = retry;
        }

        /**
         * Returns the configured circuit breaker.
         *
         * @return get circuit breaker result
         */
        public CircuitBreaker getCircuitBreaker() {
            return circuitBreaker;
        }

        /**
         * Sets the configured circuit breaker.
         *
         * @param circuitBreaker circuit breaker value
         */
        public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
        }
    }

    /** Provides retry behavior. */
    public static class Retry {
        /** Creates a retry instance. */
        public Retry() {}

        private boolean enabled;
        private int maxAttempts = 3;
        private Duration backoff = Duration.ofMillis(100);
        private boolean retryOnServerErrors = true;
        private boolean retryOnExceptions = true;
        private Set<Integer> retryOnStatuses = new LinkedHashSet<>();

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured max attempts.
         *
         * @return get max attempts result
         */
        public int getMaxAttempts() {
            return maxAttempts;
        }

        /**
         * Sets the configured max attempts.
         *
         * @param maxAttempts max attempts value
         */
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        /**
         * Returns the configured backoff.
         *
         * @return get backoff result
         */
        public Duration getBackoff() {
            return backoff;
        }

        /**
         * Sets the configured backoff.
         *
         * @param backoff backoff value
         */
        public void setBackoff(Duration backoff) {
            this.backoff = backoff;
        }

        /**
         * Reports whether retry on server errors.
         *
         * @return is retry on server errors result
         */
        public boolean isRetryOnServerErrors() {
            return retryOnServerErrors;
        }

        /**
         * Sets the configured retry on server errors.
         *
         * @param retryOnServerErrors retry on server errors value
         */
        public void setRetryOnServerErrors(boolean retryOnServerErrors) {
            this.retryOnServerErrors = retryOnServerErrors;
        }

        /**
         * Reports whether retry on exceptions.
         *
         * @return is retry on exceptions result
         */
        public boolean isRetryOnExceptions() {
            return retryOnExceptions;
        }

        /**
         * Sets the configured retry on exceptions.
         *
         * @param retryOnExceptions retry on exceptions value
         */
        public void setRetryOnExceptions(boolean retryOnExceptions) {
            this.retryOnExceptions = retryOnExceptions;
        }

        /**
         * Returns the configured retry on statuses.
         *
         * @return get retry on statuses result
         */
        public Set<Integer> getRetryOnStatuses() {
            return retryOnStatuses;
        }

        /**
         * Sets the configured retry on statuses.
         *
         * @param retryOnStatuses retry on statuses value
         */
        public void setRetryOnStatuses(Set<Integer> retryOnStatuses) {
            this.retryOnStatuses = retryOnStatuses;
        }
    }

    /** Provides circuit breaker behavior. */
    public static class CircuitBreaker {
        /** Creates a circuit breaker instance. */
        public CircuitBreaker() {}

        private boolean enabled;
        private int failureThreshold = 3;
        private Duration openDuration = Duration.ofSeconds(30);

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the configured failure threshold.
         *
         * @return get failure threshold result
         */
        public int getFailureThreshold() {
            return failureThreshold;
        }

        /**
         * Sets the configured failure threshold.
         *
         * @param failureThreshold failure threshold value
         */
        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        /**
         * Returns the configured open duration.
         *
         * @return get open duration result
         */
        public Duration getOpenDuration() {
            return openDuration;
        }

        /**
         * Sets the configured open duration.
         *
         * @param openDuration open duration value
         */
        public void setOpenDuration(Duration openDuration) {
            this.openDuration = openDuration;
        }
    }

    /** Provides authentication behavior. */
    public static class Authentication {
        /** Creates a authentication instance. */
        public Authentication() {}

        private Map<String, Credential> credentials = new LinkedHashMap<>();
        private Map<String, ClientAssertion> clientAssertions = new LinkedHashMap<>();
        private Map<String, JwtBearer> jwtBearer = new LinkedHashMap<>();
        private Map<String, KeyStore> keyStores = new LinkedHashMap<>();
        private TokenCache tokenCache = new TokenCache();
        private Diagnostics diagnostics = new Diagnostics();

        /**
         * Returns the configured credentials.
         *
         * @return get credentials result
         */
        public Map<String, Credential> getCredentials() {
            return credentials;
        }

        /**
         * Sets the configured credentials.
         *
         * @param credentials credentials value
         */
        public void setCredentials(Map<String, Credential> credentials) {
            this.credentials = credentials;
        }

        /**
         * Returns the configured client assertions.
         *
         * @return get client assertions result
         */
        public Map<String, ClientAssertion> getClientAssertions() {
            return clientAssertions;
        }

        /**
         * Sets the configured client assertions.
         *
         * @param clientAssertions client assertions value
         */
        public void setClientAssertions(Map<String, ClientAssertion> clientAssertions) {
            this.clientAssertions = clientAssertions;
        }

        /**
         * Returns the configured JWT bearer.
         *
         * @return get JWT bearer result
         */
        public Map<String, JwtBearer> getJwtBearer() {
            return jwtBearer;
        }

        /**
         * Sets the configured JWT bearer.
         *
         * @param jwtBearer JWT bearer value
         */
        public void setJwtBearer(Map<String, JwtBearer> jwtBearer) {
            this.jwtBearer = jwtBearer;
        }

        /**
         * Returns the configured key stores.
         *
         * @return get key stores result
         */
        public Map<String, KeyStore> getKeyStores() {
            return keyStores;
        }

        /**
         * Sets the configured key stores.
         *
         * @param keyStores key stores value
         */
        public void setKeyStores(Map<String, KeyStore> keyStores) {
            this.keyStores = keyStores;
        }

        /**
         * Returns the configured token cache.
         *
         * @return get token cache result
         */
        public TokenCache getTokenCache() {
            return tokenCache;
        }

        /**
         * Sets the configured token cache.
         *
         * @param tokenCache token cache value
         */
        public void setTokenCache(TokenCache tokenCache) {
            this.tokenCache = tokenCache;
        }

        /**
         * Returns the configured diagnostics.
         *
         * @return get diagnostics result
         */
        public Diagnostics getDiagnostics() {
            return diagnostics;
        }

        /**
         * Sets the configured diagnostics.
         *
         * @param diagnostics diagnostics value
         */
        public void setDiagnostics(Diagnostics diagnostics) {
            this.diagnostics = diagnostics;
        }
    }

    /** Provides validation behavior. */
    public static class Validation {
        /** Creates a validation instance. */
        public Validation() {}

        private boolean enabled = true;
        private boolean failOnWarnings;
        private boolean validateKeyStoreContent;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether fail on warnings.
         *
         * @return is fail on warnings result
         */
        public boolean isFailOnWarnings() {
            return failOnWarnings;
        }

        /**
         * Sets the configured fail on warnings.
         *
         * @param failOnWarnings fail on warnings value
         */
        public void setFailOnWarnings(boolean failOnWarnings) {
            this.failOnWarnings = failOnWarnings;
        }

        /**
         * Reports whether validate key store content.
         *
         * @return is validate key store content result
         */
        public boolean isValidateKeyStoreContent() {
            return validateKeyStoreContent;
        }

        /**
         * Sets the configured validate key store content.
         *
         * @param validateKeyStoreContent validate key store content value
         */
        public void setValidateKeyStoreContent(boolean validateKeyStoreContent) {
            this.validateKeyStoreContent = validateKeyStoreContent;
        }
    }

    /** Provides token cache behavior. */
    public static class TokenCache {
        /** Creates a token cache instance. */
        public TokenCache() {}

        private boolean clientCredentials = true;
        private boolean jwtBearer = true;

        /**
         * Reports whether client credentials.
         *
         * @return is client credentials result
         */
        public boolean isClientCredentials() {
            return clientCredentials;
        }

        /**
         * Sets the configured client credentials.
         *
         * @param clientCredentials client credentials value
         */
        public void setClientCredentials(boolean clientCredentials) {
            this.clientCredentials = clientCredentials;
        }

        /**
         * Reports whether JWT bearer.
         *
         * @return is JWT bearer result
         */
        public boolean isJwtBearer() {
            return jwtBearer;
        }

        /**
         * Sets the configured JWT bearer.
         *
         * @param jwtBearer JWT bearer value
         */
        public void setJwtBearer(boolean jwtBearer) {
            this.jwtBearer = jwtBearer;
        }
    }

    /** Provides diagnostics behavior. */
    public static class Diagnostics {
        /** Creates a diagnostics instance. */
        public Diagnostics() {}

        private boolean enabled;
        private boolean includeClaims;
        private boolean includeCacheEvents = true;
        private boolean includeTokenPreview;
        private int tokenPreviewLength = 24;

        /**
         * Reports whether enabled.
         *
         * @return is enabled result
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets the configured enabled.
         *
         * @param enabled enabled value
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Reports whether include claims.
         *
         * @return is include claims result
         */
        public boolean isIncludeClaims() {
            return includeClaims;
        }

        /**
         * Sets the configured include claims.
         *
         * @param includeClaims include claims value
         */
        public void setIncludeClaims(boolean includeClaims) {
            this.includeClaims = includeClaims;
        }

        /**
         * Reports whether include cache events.
         *
         * @return is include cache events result
         */
        public boolean isIncludeCacheEvents() {
            return includeCacheEvents;
        }

        /**
         * Sets the configured include cache events.
         *
         * @param includeCacheEvents include cache events value
         */
        public void setIncludeCacheEvents(boolean includeCacheEvents) {
            this.includeCacheEvents = includeCacheEvents;
        }

        /**
         * Reports whether include token preview.
         *
         * @return is include token preview result
         */
        public boolean isIncludeTokenPreview() {
            return includeTokenPreview;
        }

        /**
         * Sets the configured include token preview.
         *
         * @param includeTokenPreview include token preview value
         */
        public void setIncludeTokenPreview(boolean includeTokenPreview) {
            this.includeTokenPreview = includeTokenPreview;
        }

        /**
         * Returns the configured token preview length.
         *
         * @return get token preview length result
         */
        public int getTokenPreviewLength() {
            return tokenPreviewLength;
        }

        /**
         * Sets the configured token preview length.
         *
         * @param tokenPreviewLength token preview length value
         */
        public void setTokenPreviewLength(int tokenPreviewLength) {
            this.tokenPreviewLength = tokenPreviewLength;
        }
    }

    /** Provides client assertion behavior. */
    public static class ClientAssertion {
        /** Creates a client assertion instance. */
        public ClientAssertion() {}

        private String keyStoreId;
        private Duration tokenLifetime = Duration.ofSeconds(60);
        private Map<String, Object> customClaims = new LinkedHashMap<>();

        /**
         * Returns the configured key store id.
         *
         * @return get key store id result
         */
        public String getKeyStoreId() {
            return keyStoreId;
        }

        /**
         * Sets the configured key store id.
         *
         * @param keyStoreId key store id value
         */
        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }

        /**
         * Returns the configured token lifetime.
         *
         * @return get token lifetime result
         */
        public Duration getTokenLifetime() {
            return tokenLifetime;
        }

        /**
         * Sets the configured token lifetime.
         *
         * @param tokenLifetime token lifetime value
         */
        public void setTokenLifetime(Duration tokenLifetime) {
            this.tokenLifetime = tokenLifetime;
        }

        /**
         * Returns the configured custom claims.
         *
         * @return get custom claims result
         */
        public Map<String, Object> getCustomClaims() {
            return customClaims;
        }

        /**
         * Sets the configured custom claims.
         *
         * @param customClaims custom claims value
         */
        public void setCustomClaims(Map<String, Object> customClaims) {
            this.customClaims = customClaims;
        }
    }

    /** Provides JWT bearer behavior. */
    public static class JwtBearer {
        /** Creates a JWT bearer instance. */
        public JwtBearer() {}

        private String keyStoreId;
        private String issuer;
        private String subject;
        private String audience;
        private Duration tokenLifetime = Duration.ofMinutes(5);
        private Map<String, Object> customClaims = new LinkedHashMap<>();

        /**
         * Returns the configured key store id.
         *
         * @return get key store id result
         */
        public String getKeyStoreId() {
            return keyStoreId;
        }

        /**
         * Sets the configured key store id.
         *
         * @param keyStoreId key store id value
         */
        public void setKeyStoreId(String keyStoreId) {
            this.keyStoreId = keyStoreId;
        }

        /**
         * Returns the configured issuer.
         *
         * @return get issuer result
         */
        public String getIssuer() {
            return issuer;
        }

        /**
         * Sets the configured issuer.
         *
         * @param issuer issuer value
         */
        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        /**
         * Returns the configured subject.
         *
         * @return get subject result
         */
        public String getSubject() {
            return subject;
        }

        /**
         * Sets the configured subject.
         *
         * @param subject subject value
         */
        public void setSubject(String subject) {
            this.subject = subject;
        }

        /**
         * Returns the configured audience.
         *
         * @return get audience result
         */
        public String getAudience() {
            return audience;
        }

        /**
         * Sets the configured audience.
         *
         * @param audience audience value
         */
        public void setAudience(String audience) {
            this.audience = audience;
        }

        /**
         * Returns the configured token lifetime.
         *
         * @return get token lifetime result
         */
        public Duration getTokenLifetime() {
            return tokenLifetime;
        }

        /**
         * Sets the configured token lifetime.
         *
         * @param tokenLifetime token lifetime value
         */
        public void setTokenLifetime(Duration tokenLifetime) {
            this.tokenLifetime = tokenLifetime;
        }

        /**
         * Returns the configured custom claims.
         *
         * @return get custom claims result
         */
        public Map<String, Object> getCustomClaims() {
            return customClaims;
        }

        /**
         * Sets the configured custom claims.
         *
         * @param customClaims custom claims value
         */
        public void setCustomClaims(Map<String, Object> customClaims) {
            this.customClaims = customClaims;
        }
    }

    /** Provides key store behavior. */
    public static class KeyStore {
        /** Creates a key store instance. */
        public KeyStore() {}

        private String location;
        private String base64;
        private String type = "PKCS12";
        private String password;
        private String passwordRef;
        private String keyAlias;
        private String keyPassword;
        private String keyPasswordRef;

        /**
         * Returns the configured location.
         *
         * @return get location result
         */
        public String getLocation() {
            return location;
        }

        /**
         * Sets the configured location.
         *
         * @param location location value
         */
        public void setLocation(String location) {
            this.location = location;
        }

        /**
         * Returns the configured base64.
         *
         * @return get base64 result
         */
        public String getBase64() {
            return base64;
        }

        /**
         * Sets the configured base64.
         *
         * @param base64 base64 value
         */
        public void setBase64(String base64) {
            this.base64 = base64;
        }

        /**
         * Returns the configured type.
         *
         * @return get type result
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the configured type.
         *
         * @param type type value
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Returns the configured password.
         *
         * @return get password result
         */
        public String getPassword() {
            return password;
        }

        /**
         * Sets the configured password.
         *
         * @param password password value
         */
        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Returns the configured password ref.
         *
         * @return get password ref result
         */
        public String getPasswordRef() {
            return passwordRef;
        }

        /**
         * Sets the configured password ref.
         *
         * @param passwordRef password ref value
         */
        public void setPasswordRef(String passwordRef) {
            this.passwordRef = passwordRef;
        }

        /**
         * Returns the configured key alias.
         *
         * @return get key alias result
         */
        public String getKeyAlias() {
            return keyAlias;
        }

        /**
         * Sets the configured key alias.
         *
         * @param keyAlias key alias value
         */
        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        /**
         * Returns the configured key password.
         *
         * @return get key password result
         */
        public String getKeyPassword() {
            return keyPassword;
        }

        /**
         * Sets the configured key password.
         *
         * @param keyPassword key password value
         */
        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }

        /**
         * Returns the configured key password ref.
         *
         * @return get key password ref result
         */
        public String getKeyPasswordRef() {
            return keyPasswordRef;
        }

        /**
         * Sets the configured key password ref.
         *
         * @param keyPasswordRef key password ref value
         */
        public void setKeyPasswordRef(String keyPasswordRef) {
            this.keyPasswordRef = keyPasswordRef;
        }
    }

    /** Provides credential behavior. */
    public static class Credential {
        /** Creates a credential instance. */
        public Credential() {}

        private String value;
        private String base64;

        /**
         * Returns the configured value.
         *
         * @return get value result
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the configured value.
         *
         * @param value plain-text credential value
         */
        public void setValue(String value) {
            this.value = value;
        }

        /**
         * Returns the configured base64.
         *
         * @return get base64 result
         */
        public String getBase64() {
            return base64;
        }

        /**
         * Sets the configured base64.
         *
         * @param base64 base64 value
         */
        public void setBase64(String base64) {
            this.base64 = base64;
        }
    }
}
