package com.smbtech.serviceframework.starter.restclient.adapter.out.authentication.spring;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Provides OAuth2 configuration validation result behavior. */
public final class OAuth2ConfigurationValidationResult {

    private static final OAuth2ConfigurationValidationResult EMPTY =
            new OAuth2ConfigurationValidationResult(List.of());

    private final List<OAuth2ConfigurationValidationIssue> issues;

    private OAuth2ConfigurationValidationResult(List<OAuth2ConfigurationValidationIssue> issues) {
        this.issues = List.copyOf(issues);
    }

    /**
     * Performs the empty operation.
     *
     * @return empty result
     */
    public static OAuth2ConfigurationValidationResult empty() {
        return EMPTY;
    }

    /**
     * Creates the result.
     *
     * @param issues issues value
     * @return of result
     */
    public static OAuth2ConfigurationValidationResult of(
            List<OAuth2ConfigurationValidationIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return empty();
        }
        return new OAuth2ConfigurationValidationResult(issues);
    }

    /**
     * Creates er.
     *
     * @return builder result
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reports whether sues.
     *
     * @return issues result
     */
    public List<OAuth2ConfigurationValidationIssue> issues() {
        return issues;
    }

    /**
     * Performs the errors operation.
     *
     * @return errors result
     */
    public List<OAuth2ConfigurationValidationIssue> errors() {
        return issues.stream().filter(OAuth2ConfigurationValidationIssue::isError).toList();
    }

    /**
     * Performs the warnings operation.
     *
     * @return warnings result
     */
    public List<OAuth2ConfigurationValidationIssue> warnings() {
        return issues.stream().filter(OAuth2ConfigurationValidationIssue::isWarning).toList();
    }

    /**
     * Reports whether valid.
     *
     * @return is valid result
     */
    public boolean isValid() {
        return !hasErrors();
    }

    /**
     * Reports whether errors.
     *
     * @return has errors result
     */
    public boolean hasErrors() {
        return issues.stream().anyMatch(OAuth2ConfigurationValidationIssue::isError);
    }

    /**
     * Reports whether warnings.
     *
     * @return has warnings result
     */
    public boolean hasWarnings() {
        return issues.stream().anyMatch(OAuth2ConfigurationValidationIssue::isWarning);
    }

    /**
     * Performs the should fail operation.
     *
     * @param failOnWarnings fail on warnings value
     * @return should fail result
     */
    public boolean shouldFail(boolean failOnWarnings) {
        return hasErrors() || (failOnWarnings && hasWarnings());
    }

    /**
     * Performs the merge operation.
     *
     * @param other other value
     * @return merge result
     */
    public OAuth2ConfigurationValidationResult merge(OAuth2ConfigurationValidationResult other) {
        if (other == null || other.issues().isEmpty()) {
            return this;
        }
        if (issues.isEmpty()) {
            return other;
        }
        List<OAuth2ConfigurationValidationIssue> merged = new ArrayList<>(issues);
        merged.addAll(other.issues());
        return new OAuth2ConfigurationValidationResult(merged);
    }

    /** Provides builder behavior. */
    public static final class Builder {
        /** Creates a builder instance. */
        public Builder() {}

        private final List<OAuth2ConfigurationValidationIssue> issues = new ArrayList<>();

        /**
         * Performs the error operation.
         *
         * @param path path value
         * @param message message value
         * @return error result
         */
        public Builder error(String path, String message) {
            return add(OAuth2ConfigurationValidationIssue.error(path, message));
        }

        /**
         * Performs the warning operation.
         *
         * @param path path value
         * @param message message value
         * @return warning result
         */
        public Builder warning(String path, String message) {
            return add(OAuth2ConfigurationValidationIssue.warning(path, message));
        }

        /**
         * Performs the add operation.
         *
         * @param issue issue value
         * @return add result
         */
        public Builder add(OAuth2ConfigurationValidationIssue issue) {
            issues.add(Objects.requireNonNull(issue, "issue must not be null"));
            return this;
        }

        /**
         * Adds all.
         *
         * @param result result value
         * @return add all result
         */
        public Builder addAll(OAuth2ConfigurationValidationResult result) {
            if (result != null) {
                issues.addAll(result.issues());
            }
            return this;
        }

        /**
         * Creates the result.
         *
         * @return build result
         */
        public OAuth2ConfigurationValidationResult build() {
            return OAuth2ConfigurationValidationResult.of(issues);
        }
    }
}
