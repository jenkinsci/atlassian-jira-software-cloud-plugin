package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Pipeline {

    private final String id;
    private final String displayName;
    private final String url;

    @JsonCreator
    public Pipeline(
            @JsonProperty("id") final String id,
            @JsonProperty("displayName") final String displayName,
            @JsonProperty("url") final String url) {
        this.id = id;
        this.displayName = displayName;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String displayName;
        private String url;

        public Builder withId(final String id) {
            this.id = id;
            return this;
        }

        public Builder withDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withUrl(final String url) {
            this.url = url;
            return this;
        }

        public Pipeline build() {
            return new Pipeline(id, displayName, url);
        }
    }
}
