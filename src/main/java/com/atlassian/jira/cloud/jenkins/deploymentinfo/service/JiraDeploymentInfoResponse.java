package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;

import java.util.List;

public class JiraDeploymentInfoResponse extends JiraSendInfoResponse {

    public JiraDeploymentInfoResponse(final Status status, final String message) {
        super(status, message);
    }

    public static JiraSendInfoResponse failureEnvironmentInvalid(
            final String jiraSite, final List<String> errorMessages) {
        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_ENVIRONMENT_INVALID(
                        String.join(" ", errorMessages));
        return new JiraDeploymentInfoResponse(Status.FAILURE_ENVIRONMENT_INVALID, message);
    }

    public static JiraSendInfoResponse skippedIssueKeysNotFoundAndServiceIdsAreEmpty(
            final String jiraSite) {
        final String message =
                Messages
                        .JiraDeploymentInfoResponse_SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_NOT_PROVIDED(
                                jiraSite);
        return new JiraDeploymentInfoResponse(
                Status.SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY, message);
    }

    public static JiraDeploymentInfoResponse successDeploymentAccepted(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_SUCCESS_DEPLOYMENT_ACCEPTED(
                        jiraSite, response.getAcceptedDeployments());
        return new JiraDeploymentInfoResponse(Status.SUCCESS_DEPLOYMENT_ACCEPTED, message);
    }

    public static JiraDeploymentInfoResponse failureDeploymentRejected(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_DEPLOYMENT_REJECTED(
                        jiraSite, response.getRejectedDeployments());
        return new JiraDeploymentInfoResponse(Status.FAILURE_DEPLOYMENT_REJECTED, message);
    }

    public static JiraDeploymentInfoResponse failureUnknownAssociations(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_UNKNOWN_ASSOCIATIONS(
                        jiraSite, response.getUnknownAssociations());
        return new JiraDeploymentInfoResponse(Status.FAILURE_UNKNOWN_ASSOCIATIONS, message);
    }

    public static JiraDeploymentInfoResponse failureUnexpectedResponse() {
        final String message = Messages.JiraDeploymentInfoResponse_FAILURE_UNEXPECTED_RESPONSE();
        return new JiraDeploymentInfoResponse(Status.FAILURE_UNEXPECTED_RESPONSE, message);
    }

    public static JiraDeploymentInfoResponse failureDeploymentsApiResponse(
            final String jiraSite, final String errorMessage) {
        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_DEPLOYMENTS_API_RESPONSE(
                        jiraSite, errorMessage);
        return new JiraDeploymentInfoResponse(Status.FAILURE_DEPLOYMENTS_API_RESPONSE, message);
    }

    public static JiraSendInfoResponse failureStateInvalid(final List<String> errorMessages) {
        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_STATE_INVALID(
                        String.join(" ", errorMessages));
        return new JiraDeploymentInfoResponse(Status.FAILURE_STATE_INVALID, message);
    }
}
