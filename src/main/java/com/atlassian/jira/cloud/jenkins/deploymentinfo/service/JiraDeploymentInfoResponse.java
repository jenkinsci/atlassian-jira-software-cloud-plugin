package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;

public class JiraDeploymentInfoResponse extends JiraSendInfoResponse {

    public JiraDeploymentInfoResponse(final Status status, final String message) {
        super(status, message);
    }

    public static JiraSendInfoResponse skippedIssueKeysNotFound(final String jiraSite) {
        final String message =
                Messages.JiraDeploymentInfoResponse_SKIPPED_ISSUE_KEYS_NOT_FOUND(jiraSite);
        return new JiraDeploymentInfoResponse(Status.SKIPPED_ISSUE_KEYS_NOT_FOUND, message);
    }

    public static JiraDeploymentInfoResponse successDeploymentAccepted(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_SUCCESS_DEPLOYMENT_ACCEPTED(
                        jiraSite, response.getAcceptedDeployments());
        return new JiraDeploymentInfoResponse(Status.SUCCESS_DEPLOYMENT_ACCEPTED, message);
    }

    public static JiraDeploymentInfoResponse failureDeploymentdRejected(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_DEPLOYMENT_REJECTED(
                        jiraSite, response.getRejectedDeployments());
        return new JiraDeploymentInfoResponse(Status.FAILURE_DEPLOYMENT_REJECTED, message);
    }

    public static JiraDeploymentInfoResponse failureUnknownIssueKeys(
            final String jiraSite, final DeploymentApiResponse response) {

        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_UNKNOWN_ISSUE_KEYS(
                        jiraSite, response.getUnknownIssueKeys());
        return new JiraDeploymentInfoResponse(Status.FAILURE_UNKNOWN_ISSUE_KEYS, message);
    }

    public static JiraDeploymentInfoResponse failureUnexpectedResponse() {
        final String message = Messages.JiraDeploymentInfoResponse_FAILURE_UNEXPECTED_RESPONSE();
        return new JiraDeploymentInfoResponse(Status.FAILURE_UNEXPECTED_RESPONSE, message);
    }

    public static JiraDeploymentInfoResponse failureDeploymentsApiResponse(final String jiraSite) {
        final String message =
                Messages.JiraDeploymentInfoResponse_FAILURE_DEPLOYMENTS_API_RESPONSE(jiraSite);
        return new JiraDeploymentInfoResponse(Status.FAILURE_DEPLOYMENTS_API_RESPONSE, message);
    }
}
