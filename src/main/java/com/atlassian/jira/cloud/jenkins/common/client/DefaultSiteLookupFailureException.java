package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.Messages;

public class DefaultSiteLookupFailureException extends RuntimeException {
    public DefaultSiteLookupFailureException() {
        super(Messages.JiraCommonResponse_FAILURE_UNABLE_TO_DETERMINE_DEFAULT_SITE());
    }
}
