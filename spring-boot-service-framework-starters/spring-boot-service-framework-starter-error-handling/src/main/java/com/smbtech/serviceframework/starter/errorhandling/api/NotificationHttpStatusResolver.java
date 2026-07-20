package com.smbtech.serviceframework.starter.errorhandling.api;

import com.smbtech.serviceframework.error.ResolvedError;
import org.springframework.http.HttpStatusCode;

/** Resolves the HTTP status associated with a framework-neutral error. */
@FunctionalInterface
public interface NotificationHttpStatusResolver {

    /**
     * Resolves the response status.
     *
     * @param resolvedError resolved error
     * @return HTTP status
     */
    HttpStatusCode resolve(ResolvedError resolvedError);
}
