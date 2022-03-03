package com.atlassian.jira.cloud.jenkins.ping;

public class PingResponse {

    private final boolean success;

    public PingResponse(final boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}
