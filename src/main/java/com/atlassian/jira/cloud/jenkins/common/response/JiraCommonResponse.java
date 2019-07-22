package com.atlassian.jira.cloud.jenkins.common.response;

import com.atlassian.jira.cloud.jenkins.Messages;

public class JiraCommonResponse extends JiraSendInfoResponse {

    public JiraCommonResponse(final Status status, final String message) {
        super(status, message);
    }

    public static JiraCommonResponse failureAccessToken(final String jiraSite) {
        final String message = Messages.JiraCommonResponse_FAILURE_ACCESS_TOKEN(jiraSite);
        return new JiraCommonResponse(Status.FAILURE_ACCESS_TOKEN, message);
    }

    public static JiraCommonResponse failureSiteConfigNotFound(final String jiraSite) {
        final String message = Messages.JiraCommonResponse_FAILURE_SITE_CONFIG_NOT_FOUND(jiraSite);
        return new JiraCommonResponse(Status.FAILURE_SITE_CONFIG_NOT_FOUND, message);
    }

    public static JiraCommonResponse failureSecretNotFound(final String jiraSite) {
        final String message = Messages.JiraCommonResponse_FAILURE_SECRET_NOT_FOUND(jiraSite);
        return new JiraCommonResponse(Status.FAILURE_SECRET_NOT_FOUND, message);
    }

    public static JiraCommonResponse failureSiteNotFound(final String jiraSite) {
        final String message = Messages.JiraCommonResponse_FAILURE_SITE_NOT_FOUND(jiraSite);
        return new JiraCommonResponse(Status.FAILURE_SITE_NOT_FOUND, message);
    }
}
