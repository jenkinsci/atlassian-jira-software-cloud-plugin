package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppApi;
import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

public class DeploymentsApi extends JenkinsAppApi<DeploymentApiResponse> {

    public DeploymentsApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        super(httpClient, objectMapper);
    }

    /**
     * Sends a build event to the Jenkins app in Jira.
     *
     * @param webhookUrl         URL to the Jenkins app webhook.
     * @param deploymentsRequest the payload of the builds request.
     * @return the response of the Jenkins app webhook.
     * @throws ApiUpdateFailedException if the webhook responded with a non-successful HTTP status
     *                                  code.
     */
    public DeploymentApiResponse sendDeployment(
            final String webhookUrl, final Deployments deploymentsRequest)
            throws ApiUpdateFailedException {

        JenkinsAppRequest request = createRequest(deploymentsRequest);
        return this.sendRequest(webhookUrl, request, DeploymentApiResponse.class);
    }

    public DeploymentApiResponse sendDeploymentAsJwt(
            final String webhookUrl,
            final Deployments deploymentsRequest,
            String secret) throws ApiUpdateFailedException {

        JenkinsAppRequest request = createRequest(deploymentsRequest);
        return this.sendRequestAsJwt(webhookUrl, secret, request, DeploymentApiResponse.class);
    }

    @NotNull
    private JenkinsAppRequest createRequest(Deployments deploymentsRequest) {
        return new JenkinsAppRequest(
                JenkinsAppRequest.RequestType.EVENT,
                JenkinsAppRequest.EventType.DEPLOYMENT,
                deploymentsRequest.getDeployment().getPipeline().getId(),
                deploymentsRequest.getDeployment().getDisplayName(),
                deploymentsRequest.getDeployment().getState(),
                deploymentsRequest.getDeployment().getLastUpdated(),
                deploymentsRequest);
    }

}
