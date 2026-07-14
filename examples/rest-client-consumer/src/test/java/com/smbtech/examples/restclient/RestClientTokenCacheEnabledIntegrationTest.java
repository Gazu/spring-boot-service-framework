package com.smbtech.examples.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "smbtech.rest-clients.authentication.token-cache.client-credentials=true",
                "smbtech.rest-clients.authentication.token-cache.jwt-bearer=true"
        }
)
class RestClientTokenCacheEnabledIntegrationTest extends AbstractRestClientTokenCacheIntegrationTest {

    @Test
    void cachesClientCredentialsAndJwtBearerTokensByDefault() {
        assertThat(getWithClientCredentialsRestClient()).isEqualTo("ok-from-payments");
        assertThat(getWithClientCredentialsRestClient()).isEqualTo("ok-from-payments");
        assertThat(paymentsService.dummy()).isEqualTo("ok-from-payments");
        assertThat(paymentsService.dummy()).isEqualTo("ok-from-payments");

        assertThat(tokenRequests()).isEqualTo(2);
        assertThat(clientCredentialsTokenRequests()).isEqualTo(1);
        assertThat(jwtBearerTokenRequests()).isEqualTo(1);
        assertThat(paymentsRequests()).isEqualTo(4);
        assertThat(paymentsAuthorizationHeaders()).containsExactly(
                "Bearer client-credentials-token-1",
                "Bearer client-credentials-token-1",
                "Bearer jwt-bearer-token-1",
                "Bearer jwt-bearer-token-1"
        );
    }
}
