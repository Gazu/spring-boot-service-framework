package com.smbtech.examples.restclient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "smbtech.rest-clients.authentication.token-cache.client-credentials=false",
                "smbtech.rest-clients.authentication.token-cache.jwt-bearer=false"
        }
)
class RestClientTokenCacheDisabledIntegrationTest extends AbstractRestClientTokenCacheIntegrationTest {

    @Test
    void fetchesFreshClientCredentialsAndJwtBearerTokensWhenCacheIsDisabled() {
        assertThat(getWithClientCredentialsRestClient()).isEqualTo("ok-from-payments");
        assertThat(getWithClientCredentialsRestClient()).isEqualTo("ok-from-payments");
        assertThat(paymentsService.dummy()).isEqualTo("ok-from-payments");
        assertThat(paymentsService.dummy()).isEqualTo("ok-from-payments");

        assertThat(tokenRequests()).isEqualTo(4);
        assertThat(clientCredentialsTokenRequests()).isEqualTo(2);
        assertThat(jwtBearerTokenRequests()).isEqualTo(2);
        assertThat(paymentsRequests()).isEqualTo(4);
        assertThat(paymentsAuthorizationHeaders()).containsExactly(
                "Bearer client-credentials-token-1",
                "Bearer client-credentials-token-2",
                "Bearer jwt-bearer-token-1",
                "Bearer jwt-bearer-token-2"
        );
    }
}
