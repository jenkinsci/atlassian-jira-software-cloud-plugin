package com.atlassian.jira.cloud.jenkins.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import static java.util.Objects.requireNonNull;

/**
 * For builds/deployments rejected from the API, it includes an appropriate message on why it was rejected (eg.
 * invalid URL)
 */
public class ApiErrorResponse {
    private String message;

    @JsonCreator
    public ApiErrorResponse(@JsonProperty("message") final String message) {
        this.message = requireNonNull(message);
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ApiErrorResponse{" + "message='" + message + '\'' + '}';
    }
}
