package com.atlassian.jira.cloud.jenkins.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import org.apache.commons.lang.StringUtils;

public final class StateValidator {

    public static List<String> validate(@Nullable final String state) {
        final List<String> errorMessages = new ArrayList<>();

        if (StringUtils.isNotBlank(state) && !State.ALLOWED_STATES.contains(state)) {
            errorMessages.add(
                    String.format(
                            "The parameter state is not valid. Allowed values are: %s. Provided value was: '%s'",
                            State.ALLOWED_STATES, state));
        }

        return errorMessages;
    }
}
