package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

public class JiraBuildInfoResponse extends JiraSendInfoResponse {

    public JiraBuildInfoResponse(final String jiraSite, final Status status, final String message) {
        super(jiraSite, status, message);
    }

    public static JiraBuildInfoResponse successBuildAccepted(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_SUCCESS_BUILD_ACCEPTED(
                        jiraSite, response.getAcceptedBuilds());
        return new JiraBuildInfoResponse(jiraSite, Status.SUCCESS_BUILD_ACCEPTED, message);
    }

    public static JiraBuildInfoResponse failureBuildRejected(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_BUILD_REJECTED(
                        jiraSite, response.getRejectedBuilds());
        return new JiraBuildInfoResponse(jiraSite, Status.FAILURE_BUILD_REJECTED, message);
    }

    public static JiraBuildInfoResponse failureUnknownIssueKeys(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_UNKNOWN_ISSUE_KEYS(
                        jiraSite, response.getUnknownIssueKeys());
        return new JiraBuildInfoResponse(jiraSite, Status.FAILURE_UNKNOWN_ISSUE_KEYS, message);
    }

    public static JiraBuildInfoResponse failureBuildsApiResponse(
            final String jiraSite, final String errorMessage) {
        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_BUILDS_API_RESPONSE(jiraSite, errorMessage);
        return new JiraBuildInfoResponse(jiraSite, Status.FAILURE_BUILDS_API_RESPONSE, message);
    }

    public static JiraBuildInfoResponse failureUnexpectedResponse(final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_UNEXPECTED_RESPONSE();
        return new JiraBuildInfoResponse(jiraSite, Status.FAILURE_UNEXPECTED_RESPONSE, message);
    }

    public static JiraBuildInfoResponse skippedIssueKeysNotFound(final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_SKIPPED_ISSUE_KEYS_NOT_FOUND();
        return new JiraBuildInfoResponse(jiraSite, Status.SKIPPED_ISSUE_KEYS_NOT_FOUND, message);
    }
}
