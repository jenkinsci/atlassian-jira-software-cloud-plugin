package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EnvironmentValidator {

    private static final Set<String> ALLOWED_ENVS =
            ImmutableSet.of("development", "testing", "acceptance", "staging", "production", "unmapped");

    public static List<String> validate(final Environment environment) {
        final List<String> errorMessages = new ArrayList<>();

        if (StringUtils.isBlank(environment.getId())) {
            errorMessages.add("The parameter environmentId is required.");
        }

        if (StringUtils.isBlank(environment.getDisplayName())) {
            errorMessages.add("The parameter environmentName is required.");
        }

        if (!isAllowedEnvironmentType(environment.getType())) {
            errorMessages.add(
                    "The parameter environmentType is not valid. Allowed values are: "
                            + ALLOWED_ENVS);
        }

        return errorMessages;
    }

    private static boolean isAllowedEnvironmentType(final String environmentTyp) {
        return StringUtils.isNotBlank(environmentTyp) && ALLOWED_ENVS.contains(environmentTyp);
    }
}
