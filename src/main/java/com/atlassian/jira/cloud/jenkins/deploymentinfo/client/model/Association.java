package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The entities to associate the Deployment information with.
 */
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Association that = (Association) o;
        return associationType == that.associationType && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(associationType, values);
    }

    @Override
    public String toString() {
        return "Association{associationType=" + associationType.getValue() + ", values=" + values + '}';
    }
}
