package com.atlassian.jira.cloud.jenkins.ping;

public class PingResponse {

    private boolean success;

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }
}
