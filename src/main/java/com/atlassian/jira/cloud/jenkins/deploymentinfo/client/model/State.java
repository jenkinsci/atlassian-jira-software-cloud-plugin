package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum State {
    UNKNOWN("unknown"),
    PENDING("pending"),
    IN_PROGRESS("in_progress"),
    CANCELLED("cancelled"),
    FAILED("failed"),
    ROLLED_BACK("rolled_back"),
    SUCCESSFUL("successful");

    public final String value;

    public static final List<String> ALLOWED_STATES =
            Collections.unmodifiableList(
                    Stream.of(State.values())
                            .map(state -> state.value)
                            .collect(Collectors.toList()));

    State(final String state) {
        this.value = state;
    }
}
