package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Objects;
import java.util.Optional;

import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;

public class DeploymentsApi {

    private static final Logger log = LoggerFactory.getLogger(DeploymentsApi.class);
    private static final String DEPLOYMENTS_API_URL =
            ATLASSIAN_API_URL + "/jira/deployments/0.1/cloud/%s/bulk";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String deploymentsApiEndpoint;

    @Inject
    public DeploymentsApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.deploymentsApiEndpoint = DEPLOYMENTS_API_URL;
    }

    /**
     * Submits a deployment to the Atlassian Deployments API and returns the response
     *
     * @param cloudId Jira Cloud Id
     * @param accessToken Access token generated from Atlassian API
     * @param jiraSiteUrl Jira site URL
     * @param jiraDeploymentInfo An assembled JiraDeploymentInfo
     * @return Response from the API
     */
    public Optional<DeploymentApiResponse> postDeploymentUpdate(
            final String cloudId,
            final String accessToken,
            final String jiraSiteUrl,
            final JiraDeploymentInfo jiraDeploymentInfo) {
        try {
            final String deploymentsPayload =
                    objectMapper.writeValueAsString(new Deployments(jiraDeploymentInfo));
            final Request request = getRequest(cloudId, accessToken, deploymentsPayload);
            final Response response = httpClient.newCall(request).execute();

            checkForErrorResponse(jiraSiteUrl, response);
            return Optional.ofNullable(handleResponseBody(jiraSiteUrl, response));
        } catch (NotSerializableException e) {
            handleError(String.format("Empty body when submitting builds for %s", jiraSiteUrl));
        } catch (JsonMappingException | JsonParseException e) {
            handleError(String.format("Invalid JSON when submitting builds for %s", jiraSiteUrl));
        } catch (JsonProcessingException e) {
            handleError("Unable to build the request payload for Builds API: " + e.getMessage());
        } catch (IOException e) {
            handleError(
                    String.format(
                            "Server exception when submitting builds for %s: %s",
                            jiraSiteUrl, e.getMessage()));
        }

        return Optional.empty();
    }

    private void checkForErrorResponse(final String jiraSiteUrl, final Response response) {
        if (!response.isSuccessful()) {
            final String message =
                    String.format(
                            "Error response code %d when submitting builds for %s",
                            response.code(), jiraSiteUrl);
            final ResponseBody responseBody = response.body();
            if (responseBody != null) {
                log.error(
                        String.format(
                                "Error response body when submitting builds for %s: %s",
                                jiraSiteUrl, responseBody));
            }

            handleError(message);
        }
    }

    private DeploymentApiResponse handleResponseBody(
            final String jiraSiteUrl, final Response response) throws IOException {
        if (response.body() == null) {
            final String message =
                    String.format("Empty response body when submitting builds for %s", jiraSiteUrl);
            handleError(message);
        }

        final DeploymentApiResponse buildApiResponse =
                objectMapper.readValue(response.body().bytes(), DeploymentApiResponse.class);

        if (!buildApiResponse.getRejectedDeployments().isEmpty()) {
            log.warn(
                    String.format(
                            "Rejected deployments when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getRejectedDeployments()));
        }

        if (!buildApiResponse.getAcceptedDeployments().isEmpty()) {
            log.info(
                    String.format(
                            "Accepted builds when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getAcceptedDeployments()));
        }

        if (!buildApiResponse.getUnknownIssueKeys().isEmpty()) {
            log.warn(
                    String.format(
                            "Unknown issue keys when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getUnknownIssueKeys()));
        }

        return buildApiResponse;
    }

    void setDeploymentsApiEndpoint(final String deploymentsApiEndpoint) {
        this.deploymentsApiEndpoint = deploymentsApiEndpoint;
    }

    private Request getRequest(
            final String cloudId, final String accessToken, final String buildsPayload) {
        RequestBody body = RequestBody.create(JSON, buildsPayload);
        return new Request.Builder()
                .url(String.format(this.deploymentsApiEndpoint, cloudId))
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();
    }

    private void handleError(final String message) {
        log.error(message);
        throw new BuildUpdateFailedException(message);
    }
}
