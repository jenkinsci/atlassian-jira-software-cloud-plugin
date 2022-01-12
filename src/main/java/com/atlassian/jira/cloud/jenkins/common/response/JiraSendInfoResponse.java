package com.atlassian.jira.cloud.jenkins.common.response;

import java.io.Serializable;
import java.util.Objects;

public abstract class JiraSendInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS_BUILD_ACCEPTED,
        SUCCESS_DEPLOYMENT_ACCEPTED,
        SUCCESS_GATE_CHECK,
        FAILURE_BUILD_REJECTED(true),
        FAILURE_DEPLOYMENT_REJECTED(true),
        FAILURE_UNKNOWN_ISSUE_KEYS(true),
        FAILURE_UNKNOWN_ASSOCIATIONS(true),
        FAILURE_SITE_CONFIG_NOT_FOUND(true),
        FAILURE_SECRET_NOT_FOUND(true),
        FAILURE_SCM_REVISION_NOT_FOUND(true),
        FAILURE_SITE_NOT_FOUND(true),
        FAILURE_ACCESS_TOKEN(true),
        FAILURE_BUILDS_API_RESPONSE(true),
        FAILURE_DEPLOYMENTS_API_RESPONSE(true),
        FAILURE_DEPLOYMENT_GATING_MANY_JIRAS(true),
        FAILURE_UNEXPECTED_RESPONSE(true),
        FAILURE_ENVIRONMENT_INVALID(true),
        FAILURE_STATE_INVALID(true),
        FAILURE_GATE_CHECK(true),
        SKIPPED_ISSUE_KEYS_NOT_FOUND,
        SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY;

        public final boolean isFailure;

        Status(final boolean isFailure) {
            this.isFailure = isFailure;
        }

        Status() {
            this.isFailure = false;
        }
    }

    private final String jiraSite;
    private final Status status;
    private final String message;

    public JiraSendInfoResponse(final String jiraSite, final Status status, final String message) {
        this.jiraSite = jiraSite;
        this.status = status;
        this.message = message;
    }

    public String getJiraSite() {
        return jiraSite;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JiraSendInfoResponse that = (JiraSendInfoResponse) o;
        return status == that.status
                && Objects.equals(message, that.message)
                && Objects.equals(jiraSite, that.jiraSite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message, jiraSite);
    }
}
