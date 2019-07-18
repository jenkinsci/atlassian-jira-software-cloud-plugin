package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import hudson.model.Run;

public class JiraBuildInfoResponse extends JiraSendInfoResponse {

    public JiraBuildInfoResponse(final Status status, final String message) {
        super(status, message);
    }

    public static JiraBuildInfoResponse successBuildAccepted(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_SUCCESS_BUILD_ACCEPTED(
                        jiraSite, response.getAcceptedBuilds());
        return new JiraBuildInfoResponse(Status.SUCCESS_BUILD_ACCEPTED, message);
    }

    public static JiraBuildInfoResponse failureBuildRejected(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_BUILD_REJECTED(
                        jiraSite, response.getRejectedBuilds());
        return new JiraBuildInfoResponse(Status.FAILURE_BUILD_REJECTED, message);
    }

    public static JiraBuildInfoResponse failureUnknownIssueKeys(
            final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_UNKNOWN_ISSUE_KEYS(
                        jiraSite, response.getUnknownIssueKeys());
        return new JiraBuildInfoResponse(Status.FAILURE_UNKNOWN_ISSUE_KEYS, message);
    }

    public static JiraBuildInfoResponse failureScmRevisionNotFound(final Run build) {
        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_SCM_REVISION_NOT_FOUND(
                        build.getDisplayName());
        return new JiraBuildInfoResponse(Status.FAILURE_SCM_REVISION_NOT_FOUND, message);
    }

    public static JiraBuildInfoResponse failureBuildsApiResponse(final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_BUILDS_API_RESPONSE(jiraSite);
        return new JiraBuildInfoResponse(Status.FAILURE_BUILDS_API_RESPONSE, message);
    }

    public static JiraBuildInfoResponse failureUnexpectedResponse() {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_UNEXPECTED_RESPONSE();
        return new JiraBuildInfoResponse(Status.FAILURE_UNEXPECTED_RESPONSE, message);
    }

    public static JiraBuildInfoResponse skippedIssueKeysNotFound() {
        final String message = Messages.JiraBuildInfoResponse_SKIPPED_ISSUE_KEYS_NOT_FOUND();
        return new JiraBuildInfoResponse(Status.SKIPPED_ISSUE_KEYS_NOT_FOUND, message);
    }
}
