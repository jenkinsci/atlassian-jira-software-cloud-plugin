package com.atlassian.jira.cloud.jenkins.common.response;

public abstract class JiraSendInfoResponse {

    public enum Status {
        SUCCESS_BUILD_ACCEPTED,
        SUCCESS_DEPLOYMENT_ACCEPTED,
        FAILURE_BUILD_REJECTED,
        FAILURE_DEPLOYMENT_REJECTED,
        FAILURE_UNKNOWN_ISSUE_KEYS,
        FAILURE_SITE_CONFIG_NOT_FOUND,
        FAILURE_SECRET_NOT_FOUND,
        FAILURE_SCM_REVISION_NOT_FOUND,
        FAILURE_SITE_NOT_FOUND,
        FAILURE_ACCESS_TOKEN,
        FAILURE_BUILDS_API_RESPONSE,
        FAILURE_DEPLOYMENTS_API_RESPONSE,
        FAILURE_UNEXPECTED_RESPONSE,
        SKIPPED_ISSUE_KEYS_NOT_FOUND,
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
}
