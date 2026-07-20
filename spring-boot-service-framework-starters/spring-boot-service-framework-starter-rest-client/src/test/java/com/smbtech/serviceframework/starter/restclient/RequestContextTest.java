package com.smbtech.serviceframework.starter.restclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smbtech.serviceframework.starter.restclient.api.RequestContext;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestContextTest {

    @Test
    void emptyContextHasNoHeadersOrJwtBearerClaims() {
        RequestContext context = RequestContext.empty();

        assertThat(context.headers()).isEmpty();
        assertThat(context.jwtBearerClaims()).isEmpty();
        assertThat(context.isEmpty()).isTrue();
    }

    @Test
    void builderCreatesExplicitHeadersAndJwtBearerClaims() {
        RequestContext context =
                RequestContext.builder()
                        .header("X-Customer-Id", "17952397-3")
                        .header(" X-Channel ", "mobile")
                        .jwtBearerClaim("customer_id", "17952397-3")
                        .jwtBearerClaim(" channel ", "mobile")
                        .jwtBearerClaim("priority", 7)
                        .build();

        assertThat(context.isEmpty()).isFalse();
        assertThat(context.headers())
                .containsExactly(
                        Map.entry("X-Customer-Id", "17952397-3"), Map.entry("X-Channel", "mobile"));
        assertThat(context.jwtBearerClaims())
                .containsExactly(
                        Map.entry("customer_id", "17952397-3"),
                        Map.entry("channel", "mobile"),
                        Map.entry("priority", 7));
    }

    @Test
    void factoryMethodsCreateFocusedContexts() {
        RequestContext headersOnly =
                RequestContext.ofHeaders(Map.of("X-Customer-Id", "17952397-3"));
        RequestContext claimsOnly =
                RequestContext.ofJwtBearerClaims(Map.of("customer_id", "17952397-3"));
        RequestContext complete =
                RequestContext.of(Map.of("X-Channel", "mobile"), Map.of("channel", "mobile"));

        assertThat(headersOnly.headers()).containsExactly(Map.entry("X-Customer-Id", "17952397-3"));
        assertThat(headersOnly.jwtBearerClaims()).isEmpty();
        assertThat(claimsOnly.headers()).isEmpty();
        assertThat(claimsOnly.jwtBearerClaims())
                .containsExactly(Map.entry("customer_id", "17952397-3"));
        assertThat(complete.headers()).containsExactly(Map.entry("X-Channel", "mobile"));
        assertThat(complete.jwtBearerClaims()).containsExactly(Map.entry("channel", "mobile"));
    }

    @Test
    void treatsNullMapsAsEmpty() {
        RequestContext context = new RequestContext(null, null);

        assertThat(context.headers()).isEmpty();
        assertThat(context.jwtBearerClaims()).isEmpty();
        assertThat(context.isEmpty()).isTrue();
    }

    @Test
    void copiesMapsDefensivelyAndKeepsContextImmutable() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Customer-Id", "17952397-3");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("customer_id", "17952397-3");

        RequestContext context = new RequestContext(headers, claims);
        headers.put("X-Channel", "mobile");
        claims.put("channel", "mobile");

        assertThat(context.headers()).containsExactly(Map.entry("X-Customer-Id", "17952397-3"));
        assertThat(context.jwtBearerClaims())
                .containsExactly(Map.entry("customer_id", "17952397-3"));
        assertThatThrownBy(() -> context.headers().put("X-Channel", "mobile"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> context.jwtBearerClaims().put("channel", "mobile"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void withMethodsReturnNewContextWithoutMutatingOriginal() {
        RequestContext original =
                RequestContext.builder()
                        .header("X-Customer-Id", "17952397-3")
                        .jwtBearerClaim("customer_id", "17952397-3")
                        .build();

        RequestContext updated =
                original.withHeader("X-Channel", "mobile").withJwtBearerClaim("channel", "mobile");

        assertThat(original.headers()).containsOnly(Map.entry("X-Customer-Id", "17952397-3"));
        assertThat(original.jwtBearerClaims()).containsOnly(Map.entry("customer_id", "17952397-3"));
        assertThat(updated.headers())
                .containsExactly(
                        Map.entry("X-Customer-Id", "17952397-3"), Map.entry("X-Channel", "mobile"));
        assertThat(updated.jwtBearerClaims())
                .containsExactly(
                        Map.entry("customer_id", "17952397-3"), Map.entry("channel", "mobile"));
    }

    @Test
    void toBuilderAndMapWithMethodsCreateUpdatedCopies() {
        RequestContext original =
                RequestContext.builder()
                        .header("X-Customer-Id", "17952397-3")
                        .jwtBearerClaim("customer_id", "17952397-3")
                        .build();

        RequestContext updated =
                original.toBuilder()
                        .headers(Map.of("X-Channel", "mobile"))
                        .jwtBearerClaims(Map.of("channel", "mobile"))
                        .build();
        RequestContext withMaps =
                original.withHeaders(Map.of("X-Operation-Id", "op-123"))
                        .withJwtBearerClaims(Map.of("operation_id", "op-123"));

        assertThat(original.headers()).containsExactly(Map.entry("X-Customer-Id", "17952397-3"));
        assertThat(original.jwtBearerClaims())
                .containsExactly(Map.entry("customer_id", "17952397-3"));
        assertThat(updated.headers())
                .containsExactly(
                        Map.entry("X-Customer-Id", "17952397-3"), Map.entry("X-Channel", "mobile"));
        assertThat(updated.jwtBearerClaims())
                .containsExactly(
                        Map.entry("customer_id", "17952397-3"), Map.entry("channel", "mobile"));
        assertThat(withMaps.headers())
                .containsExactly(
                        Map.entry("X-Customer-Id", "17952397-3"),
                        Map.entry("X-Operation-Id", "op-123"));
        assertThat(withMaps.jwtBearerClaims())
                .containsExactly(
                        Map.entry("customer_id", "17952397-3"),
                        Map.entry("operation_id", "op-123"));
    }

    @Test
    void laterValuesOverrideEarlierValuesWithTheSameName() {
        RequestContext context =
                RequestContext.builder()
                        .header("X-Channel", "web")
                        .header("X-Channel", "mobile")
                        .jwtBearerClaim("channel", "web")
                        .jwtBearerClaim("channel", "mobile")
                        .build();

        assertThat(context.headers()).containsExactly(Map.entry("X-Channel", "mobile"));
        assertThat(context.jwtBearerClaims()).containsExactly(Map.entry("channel", "mobile"));
    }

    @Test
    void rejectsBlankNamesAndNullValues() {
        assertThatThrownBy(() -> RequestContext.builder().header(" ", "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("header name must not be blank");
        assertThatThrownBy(() -> RequestContext.builder().jwtBearerClaim("", "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("claim name must not be blank");
        assertThatThrownBy(() -> RequestContext.builder().header("X-Customer-Id", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null for X-Customer-Id");
        assertThatThrownBy(() -> RequestContext.builder().jwtBearerClaim("customer_id", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value must not be null for customer_id");
    }
}
