package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * Includes the necessary info representing a build that has completed. Uniquely represented by
 * pipelineId and buildNumber.
 */
public final class JiraBuildInfo {

    private static final String SCHEMA_VERSION = "1.0";

    private final String pipelineId;
    private final Integer buildNumber;
    private final Long updateSequenceNumber;
    private final String displayName;
    private final String description;
    private final String label;
    private final String url;
    private final String state;
    private final String lastUpdated;
    private final Set<String> issueKeys;
    private final List<Reference> references;
    private final TestInfo testInfo;

    @JsonCreator
    public JiraBuildInfo(
            @JsonProperty("pipelineId") final String pipelineId,
            @JsonProperty("buildNumber") final Integer buildNumber,
            @JsonProperty("updateSequenceNumber") final Long updateSequenceNumber,
            @JsonProperty("displayName") final String displayName,
            @JsonProperty("description") final String description,
            @JsonProperty("label") final String label,
            @JsonProperty("url") final String url,
            @JsonProperty("state") final String state,
            @JsonProperty("lastUpdated") final String lastUpdated,
            @JsonProperty("issueKeys") final Set<String> issueKeys,
            @JsonProperty("references") final List<Reference> references,
            @JsonProperty("testInfo") final TestInfo testInfo) {
        this.pipelineId = pipelineId;
        this.buildNumber = buildNumber;
        this.updateSequenceNumber = updateSequenceNumber;
        this.displayName = displayName;
        this.description = description;
        this.label = label;
        this.url = url;
        this.state = state;
        this.lastUpdated = lastUpdated;
        this.issueKeys = issueKeys;
        this.references = references;
        this.testInfo = testInfo;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public Long getUpdateSequenceNumber() {
        return updateSequenceNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public String getState() {
        return state;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public Set<String> getIssueKeys() {
        return issueKeys;
    }

    public List<Reference> getReferences() {
        return references;
    }

    public TestInfo getTestInfo() {
        return testInfo;
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String pipelineId;
        private Integer buildNumber;
        private Long updateSequenceNumber;
        private String displayName;
        private String description;
        private String label;
        private String url;
        private String state;
        private String lastUpdated;
        private Set<String> issueKeys;
        private List<Reference> references;
        private TestInfo testInfo;

        public Builder withPipelineId(final String pipelineId) {
            this.pipelineId = pipelineId;
            return this;
        }

        public Builder withBuildNumber(final Integer buildNumber) {
            this.buildNumber = buildNumber;
            return this;
        }

        public Builder withUpdateSequenceNumber(final Long updateSequenceNumber) {
            this.updateSequenceNumber = updateSequenceNumber;
            return this;
        }

        public Builder withDisplayName(final String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withUrl(final String url) {
            this.url = url;
            return this;
        }

        public Builder withState(final String state) {
            this.state = state;
            return this;
        }

        public Builder withLastUpdated(final String lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withIssueKeys(final Set<String> issueKeys) {
            this.issueKeys = ImmutableSet.copyOf(issueKeys);
            return this;
        }

        public Builder withReferences(final List<Reference> references) {
            this.references = ImmutableList.copyOf(references);
            return this;
        }

        public Builder withTestInfo(final TestInfo testInfo) {
            this.testInfo = testInfo;
            return this;
        }

        public JiraBuildInfo build() {
            return new JiraBuildInfo(
                    pipelineId,
                    buildNumber,
                    updateSequenceNumber,
                    displayName,
                    description,
                    label,
                    url,
                    state,
                    lastUpdated,
                    issueKeys,
                    references,
                    testInfo);
        }
    }
}
