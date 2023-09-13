package com.atlassian.jira.cloud.jenkins.config;

public class MyJsonObject {
    private String message;

    //empty constructor required for JSON parsing.
    public MyJsonObject() { }

    public MyJsonObject(final String message) {
        this.message = message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
