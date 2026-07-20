package com.smbtech.serviceframework.starter.restclient.api.oauth2;

import java.util.Map;

/** Contributes dynamic custom claims to a JWT bearer grant assertion. */
@FunctionalInterface
public interface JwtBearerClaimsContributor {

    /**
     * Returns custom claims for the current JWT bearer grant request.
     *
     * @param context JWT bearer claim contribution context
     * @return claims to merge into the assertion
     */
    Map<String, Object> contribute(JwtBearerClaimsContext context);
}
