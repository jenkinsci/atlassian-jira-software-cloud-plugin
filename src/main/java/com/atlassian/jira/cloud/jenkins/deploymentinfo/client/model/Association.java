package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The entities to associate the Deployment information with.
 * */
public class Association {

    private final AssociationType associationType;
    private final Set<String> values;

    @JsonCreator
    public Association(
            @JsonProperty("type") final AssociationType associationType,
            @JsonProperty("values") final Set<String> values) {
        this.associationType = associationType;
        this.values = values;
    }

    public AssociationType getAssociationType() {
        return associationType;
    }

    public Set<String> getValues() {
        return values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private AssociationType associationType;
        private Set<String> values;

        public Builder withAssociationType(final AssociationType associationsType) {
            this.associationType = associationsType;
            return this;
        }

        public Builder withValues(final Set<String> values) {
            this.values = values;
            return this;
        }

        public Association build() {
            return new Association(associationType, values);
        }
    }
}
