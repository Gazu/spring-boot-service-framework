package com.smbtech.serviceframework.starter.errorhandling.api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Writes the standard {@code WWW-Authenticate} challenge for a resolved security failure without
 * writing a response body.
 */
@FunctionalInterface
public interface OAuth2SecurityChallengeWriter {

    /**
     * Writes a Bearer challenge when requested by the resolution.
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param context safe security failure context
     * @param resolution resolved security failure
     * @throws IOException when a Spring Security delegate cannot write its challenge
     */
    void write(
            HttpServletRequest request,
            HttpServletResponse response,
            SecurityFailureContext context,
            SecurityFailureResolution resolution)
            throws IOException;
}
