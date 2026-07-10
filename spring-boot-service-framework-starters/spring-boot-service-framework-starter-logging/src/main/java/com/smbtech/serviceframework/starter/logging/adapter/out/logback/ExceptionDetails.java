package com.smbtech.serviceframework.starter.logging.adapter.out.logback;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

final class ExceptionDetails {

    private ExceptionDetails() {
    }

    static Map<String, Object> from(Throwable throwable) {
        if (throwable == null) {
            return Map.of();
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("message", throwable.getMessage());
        details.put("class", throwable.getClass().getName());
        details.put("hash", fingerprint(throwable));
        details.put("cause", rootCauseMessage(throwable));
        details.put("stack", stackTrace(throwable));
        return details;
    }

    private static String fingerprint(Throwable throwable) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            update(digest, throwable);
            return HexFormat.of().formatHex(digest.digest(), 0, 8);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void update(MessageDigest digest, Throwable throwable) {
        digest.update(throwable.getClass().getName().getBytes(StandardCharsets.UTF_8));
        for (StackTraceElement element : throwable.getStackTrace()) {
            digest.update(element.getClassName().getBytes(StandardCharsets.UTF_8));
            digest.update(element.getMethodName().getBytes(StandardCharsets.UTF_8));
            digest.update(Integer.toString(element.getLineNumber()).getBytes(StandardCharsets.UTF_8));
        }
        if (throwable.getCause() != null && throwable.getCause() != throwable) {
            update(digest, throwable.getCause());
        }
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + root.getMessage();
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter output = new StringWriter();
        throwable.printStackTrace(new PrintWriter(output));
        return output.toString();
    }
}
