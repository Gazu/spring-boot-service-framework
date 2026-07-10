package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.jwt;

import com.smbtech.serviceframework.httpclient.exception.AuthenticationException;
import com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.keystore.RsaKeyFactory;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

public final class JwtAssertionFactory {

    private static final Set<String> REGISTERED_CLAIMS = Set.of("iss", "sub", "aud", "jti", "iat", "exp");

    private final RsaKeyFactory rsaKeyFactory;

    public JwtAssertionFactory(RsaKeyFactory rsaKeyFactory) {
        this.rsaKeyFactory = rsaKeyFactory;
    }

    public String create(JwtBearerGrantRequest request) {
        try {
            String header = base64Url("""
                    {"alg":"RS256","typ":"JWT"}
                    """);
            String payload = base64Url(payload(request));
            String signingInput = header + "." + payload;

            RSAPrivateKey privateKey = rsaKeyFactory.rsaPrivateKey(request.tokenRequest().jwtBearer().keyStoreId());
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

            return signingInput + "." + Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature.sign());
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AuthenticationException(
                    "Unable to create JWT bearer assertion for token request: " + request.tokenRequest().id(),
                    exception
            );
        }
    }

    private String payload(JwtBearerGrantRequest request) {
        return "{"
                + claim("iss", request.issuer()) + ","
                + claim("sub", request.subject()) + ","
                + claim("aud", request.audience()) + ","
                + claim("jti", request.jwtId()) + ","
                + numericClaim("iat", request.issuedAt().getEpochSecond()) + ","
                + numericClaim("exp", request.expiresAt().getEpochSecond())
                + customClaims(request.tokenRequest().jwtBearer().customClaims())
                + "}";
    }

    private String customClaims(Map<String, Object> customClaims) {
        if (customClaims.isEmpty()) {
            return "";
        }
        StringBuilder json = new StringBuilder();
        customClaims.forEach((name, value) -> {
            if (name != null && !name.isBlank() && !REGISTERED_CLAIMS.contains(name) && value != null) {
                json.append(",").append(claimValue(name, value));
            }
        });
        return json.toString();
    }

    private String claimValue(String name, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return quote(name) + ":" + value;
        }
        if (value instanceof String text) {
            if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                return quote(name) + ":" + Boolean.parseBoolean(text);
            }
            if (text.matches("-?\\d+")) {
                return quote(name) + ":" + Long.parseLong(text);
            }
            if (text.matches("-?\\d+\\.\\d+")) {
                return quote(name) + ":" + Double.parseDouble(text);
            }
            return claim(name, text);
        }
        return claim(name, value.toString());
    }

    private String claim(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    private String numericClaim(String name, long value) {
        return quote(name) + ":" + value;
    }

    private String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.strip().getBytes(StandardCharsets.UTF_8));
    }
}
