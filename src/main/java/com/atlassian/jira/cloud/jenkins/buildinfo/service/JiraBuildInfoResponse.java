package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import hudson.model.Run;

import static java.util.Objects.requireNonNull;

public class JiraBuildInfoResponse {

    public enum Status {
        SUCCESS_BUILD_ACCEPTED,
        FAILURE_BUILD_REJECTED,
        FAILURE_UNKNOWN_ISSUE_KEYS,
        FAILURE_SITE_CONFIG_NOT_FOUND,
        FAILURE_SECRET_NOT_FOUND,
        FAILURE_SCM_REVISION_NOT_FOUND,
        FAILURE_SITE_NOT_FOUND,
        FAILURE_ACCESS_TOKEN,
        FAILURE_BUILDS_API_RESPONSE,
        FAILURE_UNEXPECTED_RESPONSE,
        SKIPPED_ISSUE_KEYS_NOT_FOUND,
    }

    private final Run build;
    private final Status status;
    private final String message;

    private JiraBuildInfoResponse(final Run build, final Status status, final String message) {
        this.build = requireNonNull(build);
        this.status = requireNonNull(status);
        this.message = requireNonNull(message);
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static JiraBuildInfoResponse successBuildAccepted(
            final Run build, final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_SUCCESS_BUILD_ACCEPTED(
                        jiraSite, response.getAcceptedBuilds());
        return new JiraBuildInfoResponse(build, Status.SUCCESS_BUILD_ACCEPTED, message);
    }

    public static JiraBuildInfoResponse failureBuildRejected(
            final Run build, final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_BUILD_REJECTED(
                        jiraSite, response.getRejectedBuilds());
        return new JiraBuildInfoResponse(build, Status.FAILURE_BUILD_REJECTED, message);
    }

    public static JiraBuildInfoResponse failureUnknownIssueKeys(
            final Run build, final String jiraSite, final BuildApiResponse response) {

        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_UNKNOWN_ISSUE_KEYS(
                        jiraSite, response.getUnknownIssueKeys());
        return new JiraBuildInfoResponse(build, Status.FAILURE_UNKNOWN_ISSUE_KEYS, message);
    }

    public static JiraBuildInfoResponse failureSiteConfigNotFound(
            final Run build, final String jiraSite) {
        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_SITE_CONFIG_NOT_FOUND(jiraSite);
        return new JiraBuildInfoResponse(build, Status.FAILURE_SITE_CONFIG_NOT_FOUND, message);
    }

    public static JiraBuildInfoResponse failureSecretNotFound(
            final Run build, final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_SECRET_NOT_FOUND(jiraSite);
        return new JiraBuildInfoResponse(build, Status.FAILURE_SECRET_NOT_FOUND, message);
    }

    public static JiraBuildInfoResponse failureScmRevisionNotFound(final Run build) {
        final String message =
                Messages.JiraBuildInfoResponse_FAILURE_SCM_REVISION_NOT_FOUND(
                        build.getDisplayName());
        return new JiraBuildInfoResponse(build, Status.FAILURE_SCM_REVISION_NOT_FOUND, message);
    }

    public static JiraBuildInfoResponse failureSiteNotFound(
            final Run build, final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_SITE_NOT_FOUND(jiraSite);
        return new JiraBuildInfoResponse(build, Status.FAILURE_SITE_NOT_FOUND, message);
    }

    public static JiraBuildInfoResponse failureAccessToken(final Run build, final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_ACCESS_TOKEN(jiraSite);
        return new JiraBuildInfoResponse(build, Status.FAILURE_ACCESS_TOKEN, message);
    }

    public static JiraBuildInfoResponse failureBuildsApiResponse(
            final Run build, final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_BUILDS_API_RESPONSE(jiraSite);
        return new JiraBuildInfoResponse(build, Status.FAILURE_BUILDS_API_RESPONSE, message);
    }

    public static JiraBuildInfoResponse failureUnexpectedResponse(
            final Run build, final String jiraSite) {
        final String message = Messages.JiraBuildInfoResponse_FAILURE_UNEXPECTED_RESPONSE();
        return new JiraBuildInfoResponse(build, Status.FAILURE_UNEXPECTED_RESPONSE, message);
    }

    public static JiraBuildInfoResponse skippedIssueKeysNotFound(
            final Run build, final String branchName) {
        final String message =
                Messages.JiraBuildInfoResponse_SKIPPED_ISSUE_KEYS_NOT_FOUND(branchName);
        return new JiraBuildInfoResponse(build, Status.SKIPPED_ISSUE_KEYS_NOT_FOUND, message);
    }
}
