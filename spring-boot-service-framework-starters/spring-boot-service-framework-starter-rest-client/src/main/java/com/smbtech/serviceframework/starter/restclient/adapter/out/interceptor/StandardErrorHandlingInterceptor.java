package com.smbtech.serviceframework.starter.restclient.adapter.out.interceptor;

import com.smbtech.serviceframework.httpclient.domain.HttpClientDefinition;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorNotificationMapper;
import com.smbtech.serviceframework.httpclient.domain.HttpErrorResponse;
import com.smbtech.serviceframework.httpclient.exception.HttpClientResponseException;
import com.smbtech.serviceframework.httpclient.port.out.HttpErrorResponseBodyReader;
import com.smbtech.serviceframework.starter.restclient.adapter.out.error.HttpErrorResponseMapper;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public final class StandardErrorHandlingInterceptor implements ClientHttpRequestInterceptor {

    private final HttpClientDefinition definition;
    private final HttpErrorResponseMapper mapper;
    private final HttpErrorNotificationMapper notificationMapper;
    private final HttpErrorResponseBodyReader errorResponseBodyReader;

    public StandardErrorHandlingInterceptor(
            HttpClientDefinition definition,
            HttpErrorResponseMapper mapper
    ) {
        this(definition, mapper, new HttpErrorNotificationMapper());
    }

    public StandardErrorHandlingInterceptor(
            HttpClientDefinition definition,
            HttpErrorResponseMapper mapper,
            HttpErrorNotificationMapper notificationMapper
    ) {
        this(definition, mapper, notificationMapper, null);
    }

    public StandardErrorHandlingInterceptor(
            HttpClientDefinition definition,
            HttpErrorResponseMapper mapper,
            HttpErrorNotificationMapper notificationMapper,
            HttpErrorResponseBodyReader errorResponseBodyReader
    ) {
        this.definition = definition;
        this.mapper = mapper;
        this.notificationMapper = notificationMapper;
        this.errorResponseBodyReader = errorResponseBodyReader;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (definition.errorHandling().enabled() && response.getStatusCode().isError()) {
            HttpErrorResponse error = mapper.map(definition, request, response);
            throw new HttpClientResponseException(
                    error,
                    notificationMapper.map(error, definition.errorHandling()),
                    null,
                    errorResponseBodyReader
            );
        }
        return response;
    }
}
