package com.atlassian.jira.cloud.jenkins.common.response;

import java.io.Serializable;
import java.util.Objects;

public abstract class JiraSendInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status {
        SUCCESS_BUILD_ACCEPTED,
        SUCCESS_DEPLOYMENT_ACCEPTED,
        SUCCESS_GATE_CHECK,
        FAILURE_BUILD_REJECTED,
        FAILURE_DEPLOYMENT_REJECTED,
        FAILURE_UNKNOWN_ISSUE_KEYS,
        FAILURE_UNKNOWN_ASSOCIATIONS,
        FAILURE_SITE_CONFIG_NOT_FOUND,
        FAILURE_SECRET_NOT_FOUND,
        FAILURE_SCM_REVISION_NOT_FOUND,
        FAILURE_SITE_NOT_FOUND,
        FAILURE_ACCESS_TOKEN,
        FAILURE_BUILDS_API_RESPONSE,
        FAILURE_DEPLOYMENTS_API_RESPONSE,
        FAILURE_UNEXPECTED_RESPONSE,
        FAILURE_ENVIRONMENT_INVALID,
        FAILURE_STATE_INVALID,
        FAILURE_GATE_CHECK,
        SKIPPED_ISSUE_KEYS_NOT_FOUND,
        SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY,
    }

    private final Status status;
    private final String message;

    public JiraSendInfoResponse(final Status status, final String message) {
        this.status = status;
        this.message = message;
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
        return status == that.status && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, message);
    }
}
