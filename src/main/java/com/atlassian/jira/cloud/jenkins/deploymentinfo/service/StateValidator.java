package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

public final class StateValidator {

    private static final Set<String> ALLOWED_STATES =
            ImmutableSet.of("unknown", "pending", "in_progress", "cancelled", "failed", "rolled_back", "successful");

    public static List<String> validate(@Nullable final String state) {
        final List<String> errorMessages = new ArrayList<>();

        if (state != null && !isAllowedState(state)) {
            errorMessages.add(
                    "The parameter state is not valid. Allowed values are: " + ALLOWED_STATES);
        }

        return errorMessages;
    }

    private static boolean isAllowedState(final String environmentTyp) {
        return ALLOWED_STATES.contains(environmentTyp);
    }
}
