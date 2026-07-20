package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.smbtech.serviceframework.starter.restclient.adapter.out.spring.DefaultApiClientFactory;
import com.smbtech.serviceframework.starter.restclient.api.HttpApiClient;
import com.smbtech.serviceframework.starter.restclient.api.RestClientRegistry;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

class ApiClientFactoryTest {

    @Test
    void createsDeclarativeApiClientForConfiguredRestClient() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://projects.example");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DefaultApiClientFactory factory = new DefaultApiClientFactory(registry(builder.build()));

        server.expect(requestTo("https://projects.example/projects/123"))
                .andRespond(withSuccess("demo", MediaType.TEXT_PLAIN));

        ProjectsApi api = factory.create("projects", ProjectsApi.class);

        assertThat(api.findName("123")).isEqualTo("demo");
        server.verify();
    }

    @Test
    void createsDeclarativeApiClientUsingHttpApiClientAnnotation() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://payments.example");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        DefaultApiClientFactory factory = new DefaultApiClientFactory(registry(builder.build()));

        server.expect(requestTo("https://payments.example/dummy"))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        PaymentsApi api = factory.create(PaymentsApi.class);

        assertThat(api.dummy()).isEqualTo("ok");
        server.verify();
    }

    @Test
    void reusesApiClientProxyForSameClientAndInterface() {
        RestClient restClient = RestClient.builder().baseUrl("https://projects.example").build();
        DefaultApiClientFactory factory = new DefaultApiClientFactory(registry(restClient));

        ProjectsApi first = factory.create("projects", ProjectsApi.class);
        ProjectsApi second = factory.create("projects", ProjectsApi.class);

        assertThat(second).isSameAs(first);
    }

    @Test
    void rejectsAnnotationBasedCreationWithoutClientNameAnnotation() {
        DefaultApiClientFactory factory =
                new DefaultApiClientFactory(
                        registry(RestClient.builder().baseUrl("https://projects.example").build()));

        assertThatThrownBy(() -> factory.create(UnnamedApi.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@HttpApiClient");
    }

    private RestClientRegistry registry(RestClient restClient) {
        return new RestClientRegistry() {
            @Override
            public RestClient get(String name) {
                return restClient;
            }

            @Override
            public Set<String> names() {
                return Set.of("projects", "payments");
            }

            @Override
            public Map<String, RestClient> all() {
                return Map.of("projects", restClient, "payments", restClient);
            }
        };
    }

    @HttpExchange
    interface ProjectsApi {

        @GetExchange("/projects/{id}")
        String findName(@PathVariable String id);
    }

    @HttpApiClient("payments")
    @HttpExchange
    interface PaymentsApi {

        @GetExchange("/dummy")
        String dummy();
    }

    @HttpExchange
    interface UnnamedApi {

        @GetExchange("/dummy")
        String dummy();
    }
}
