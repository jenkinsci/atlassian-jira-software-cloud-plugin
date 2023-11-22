package com.atlassian.jira.cloud.jenkins.common.client;

import com.fasterxml.jackson.annotation.JsonValue;

import java.time.LocalDateTime;

public abstract class JenkinsAppRequest {

    private final RequestType requestType;

    public JenkinsAppRequest(final RequestType requestType) {
        this.requestType = requestType;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public enum RequestType {
        EVENT("event"),
        PING("ping"),
        GATING_STATUS("gatingStatus"),
        PLUGIN_CONFIG("pluginConfig");

        private final String value;

        RequestType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }
}
