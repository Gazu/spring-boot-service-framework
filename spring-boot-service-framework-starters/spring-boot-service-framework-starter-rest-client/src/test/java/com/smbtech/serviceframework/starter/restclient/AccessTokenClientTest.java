package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;

import com.smbtech.serviceframework.httpclient.domain.AccessToken;
import com.smbtech.serviceframework.starter.restclient.api.AccessTokenClient;
import com.smbtech.serviceframework.starter.restclient.api.JwtBearerTokenRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AccessTokenClientTest {

    @Test
    void dynamicJwtBearerMethodsDelegateCompleteRequest() {
        RecordingAccessTokenClient client = new RecordingAccessTokenClient();

        AccessToken token =
                client.jwtBearer(
                        "payments-jwt-bearer-token",
                        "payment.read",
                        Map.of("customer_id", "17952397-3"));

        assertThat(token.value()).isEqualTo("jwt-token");
        assertThat(client.tokenRequestId).isEqualTo("payments-jwt-bearer-token");
        assertThat(client.expectedScopes).isEqualTo("payment.read");
        assertThat(client.customClaims).containsEntry("customer_id", "17952397-3");
    }

    @Test
    void explicitJwtBearerRequestPreservesCustomClaims() {
        RecordingAccessTokenClient client = new RecordingAccessTokenClient();

        client.jwtBearer(
                new JwtBearerTokenRequest(
                        "payments-jwt-bearer-token",
                        "payment.write",
                        Map.of("customer_id", "17952397-3")));

        assertThat(client.tokenRequestId).isEqualTo("payments-jwt-bearer-token");
        assertThat(client.expectedScopes).isEqualTo("payment.write");
        assertThat(client.customClaims).containsEntry("customer_id", "17952397-3");
    }

    private static final class RecordingAccessTokenClient implements AccessTokenClient {

        private String tokenRequestId;
        private String expectedScopes;
        private Map<String, Object> customClaims = Map.of();

        @Override
        public AccessToken clientCredentials(String tokenRequestId) {
            return clientCredentials(tokenRequestId, "");
        }

        @Override
        public AccessToken clientCredentials(String tokenRequestId, String expectedScopes) {
            return token("client-credentials-token");
        }

        @Override
        public AccessToken jwtBearer(String tokenRequestId) {
            return jwtBearer(tokenRequestId, "");
        }

        @Override
        public AccessToken jwtBearer(String tokenRequestId, String expectedScopes) {
            return AccessTokenClient.super.jwtBearer(tokenRequestId, expectedScopes);
        }

        @Override
        public AccessToken jwtBearer(JwtBearerTokenRequest request) {
            this.tokenRequestId = request.tokenRequestId();
            this.expectedScopes = request.expectedScopes();
            this.customClaims = request.customClaims();
            return token("jwt-token");
        }

        private AccessToken token(String value) {
            return new AccessToken(
                    value, "Bearer", Instant.now().plusSeconds(60), Set.of("payment.read"));
        }
    }
}
