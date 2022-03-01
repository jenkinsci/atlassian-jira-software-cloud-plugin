package com.atlassian.jira.cloud.jenkins.common.client;

import com.fasterxml.jackson.annotation.JsonValue;

public class JenkinsAppEventRequest extends JenkinsAppRequest {

    private final EventType eventType;
    private final String pipelineName;
    private final String status;
    private final String lastUpdated;
    private final JiraRequest payload;
    private final String pipelineId;

    public JenkinsAppEventRequest(
            EventType eventType,
            String pipelineId,
            String pipelineName,
            String status,
            String lastUpdated,
            JiraRequest payload) {
        super(RequestType.EVENT);
        this.eventType = eventType;
        this.pipelineName = pipelineName;
        this.status = status;
        this.lastUpdated = lastUpdated;
        this.payload = payload;
        this.pipelineId = pipelineId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getStatus() {
        return status;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public JiraRequest getPayload() {
        return payload;
    }

    public enum EventType {
        BUILD("build"),
        DEPLOYMENT("deployment");

        private final String value;

        EventType(final String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }
    }

}
