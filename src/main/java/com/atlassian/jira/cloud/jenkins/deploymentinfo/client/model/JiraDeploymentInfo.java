package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Includes the necessary info representing a deployment that has completed. Uniquely represented by
 * pipeline.id, enviroment.id and deploymentSequenceNumber
 */
public final class JiraDeploymentInfo {

    private static final String SCHEMA_VERSION = "1.0";

    private final Integer deploymentSequenceNumber;
    private final Long updateSequenceNumber;
    private final Set<Association> associations;
    private final String displayName;
    private final String url;
    private final String description;
    private final String lastUpdated;
    private final String label;
    private final String state;
    private final Pipeline pipeline;
    private final Environment environment;
    private final List<Command> commands;

    @JsonCreator
    public JiraDeploymentInfo(
            @JsonProperty("deploymentSequenceNumber") final Integer deploymentSequenceNumber,
            @JsonProperty("updateSequenceNumber") final Long updateSequenceNumber,
            @JsonProperty("associations") final Set<Association> associations,
            @JsonProperty("displayName") final String displayName,
            @JsonProperty("url") final String url,
            @JsonProperty("description") final String description,
            @JsonProperty("lastUpdated") final String lastUpdated,
            @JsonProperty("label") final String label,
            @JsonProperty("state") final String state,
            @JsonProperty("pipeline") final Pipeline pipeline,
            @JsonProperty("environment") final Environment environment,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) @JsonProperty("commands")
                    final List<Command> commands) {
        this.deploymentSequenceNumber = deploymentSequenceNumber;
        this.updateSequenceNumber = updateSequenceNumber;
        this.associations = associations;
        this.displayName = displayName;
        this.url = url;
        this.description = description;
        this.lastUpdated = lastUpdated;
        this.label = label;
        this.state = state;
        this.pipeline = pipeline;
        this.environment = environment;
        this.commands = commands;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getDeploymentSequenceNumber() {
        return deploymentSequenceNumber;
    }

    public Long getUpdateSequenceNumber() {
        return updateSequenceNumber;
    }

    public Set<Association> getAssociations() {
        return associations;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    public String getLabel() {
        return label;
    }

    public String getState() {
        return state;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public static class Builder {
        private Integer deploymentSequenceNumber;
        private Long updateSequenceNumber;
        private Set<Association> associations;
        private String displayName;
        private String url;
        private String description;
        private String lastUpdated;
        private String label;
        private String state;
        private Pipeline pipeline;
        private Environment environment;
        private List<Command> commands = Collections.emptyList();

        public Builder withDeploymentSequenceNumber(final Integer deploymentSequenceNumber) {
            this.deploymentSequenceNumber = deploymentSequenceNumber;
            return this;
        }

        public Builder withUpdateSequenceNumber(final Long updateSequenceNumber) {
            this.updateSequenceNumber = updateSequenceNumber;
            return this;
        }

        public Builder withAssociations(final Set<Association> associations) {
            this.associations = ImmutableSet.copyOf(associations);
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

        public Builder withDescription(final String description) {
            this.description = description;
            return this;
        }

        public Builder withLastUpdated(final String lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public Builder withLabel(final String label) {
            this.label = label;
            return this;
        }

        public Builder withState(final String state) {
            this.state = state;
            return this;
        }

        public Builder withPipeline(final Pipeline pipeline) {
            this.pipeline = pipeline;
            return this;
        }

        public Builder withEnvironment(final Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder withCommands(final List<Command> commands) {
            this.commands = commands;
            return this;
        }

        public JiraDeploymentInfo build() {
            return new JiraDeploymentInfo(
                    deploymentSequenceNumber,
                    updateSequenceNumber,
                    associations,
                    displayName,
                    url,
                    description,
                    lastUpdated,
                    label,
                    state,
                    pipeline,
                    environment,
                    commands);
        }
    }
}
