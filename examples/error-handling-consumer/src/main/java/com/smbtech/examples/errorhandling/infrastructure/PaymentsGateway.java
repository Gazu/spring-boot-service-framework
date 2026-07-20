package com.smbtech.examples.errorhandling.infrastructure;

import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentsGateway {

    public void authorize() {
        HttpErrorResponse response =
                new HttpErrorResponse(
                        "payments",
                        "POST",
                        "https://payments.example/authorizations?token=downstream-uri-secret",
                        503,
                        "Service Unavailable",
                        HttpErrorResponse.categoryOf(503),
                        Map.of(
                                "Authorization", "Bearer downstream-header-secret",
                                "Set-Cookie", "session=downstream-cookie-secret"),
                        "{\"password\":\"downstream-body-secret\"}",
                        "application/json",
                        "UTF-8",
                        false);
        throw new HttpClientResponseException(
                response,
                java.util.List.of(),
                new IllegalStateException("TLS downstream-cause-secret"));
    }
}
