package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Environment {

    private final String id;
    private final String displayName;
    private final String type;

    @JsonCreator
    public Environment(
            @JsonProperty("id") final String id,
            @JsonProperty("displayName") final String displayName,
            @JsonProperty("type") final String type) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String displayName;
        private String type;

        public Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Builder withDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withType(final String type) {
            this.type = type;
            return this;
        }

        public Environment build() {
            return new Environment(id, displayName, type);
        }
    }
}
