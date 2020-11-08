package com.atlassian.jira.cloud.jenkins.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Common HTTP client to talk to Jira Build and Deployment APIs in Jira */
public class JiraApi {

    private static final Logger log = LoggerFactory.getLogger(JiraApi.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String apiEndpoint;

    @Inject
    public JiraApi(
            final OkHttpClient httpClient, final ObjectMapper objectMapper, final String apiUrl) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.apiEndpoint = apiUrl;
    }

    /**
     * Submits an update to the Atlassian Builds or Deployments API and returns the response
     *
     * @param <ResponseEntity> Response entity, which can be either BuildApiResponse or
     *     DeploymentApiResponse
     * @param cloudId Jira Cloud Id
     * @param accessToken Access token generated from Atlassian API
     * @param clientId oAuth client id
     * @param jiraRequest An assembled payload to be submitted to Jira
     * @return Response from the API
     */
    public <ResponseEntity> PostUpdateResult<ResponseEntity> postUpdate(
            final String cloudId,
            final String accessToken,
            final String clientId,
            final JiraRequest jiraRequest,
            final Class<ResponseEntity> responseClass) {
        try {
            final String requestPayload = objectMapper.writeValueAsString(jiraRequest);
            final Request request = getRequest(cloudId, accessToken, requestPayload, clientId);
            final Response response = httpClient.newCall(request).execute();

            checkForErrorResponse(response);

            final ResponseEntity responseEntity = handleResponseBody(response, responseClass);
            return new PostUpdateResult<>(responseEntity);
        } catch (NotSerializableException e) {
            return handleError(String.format("Invalid JSON payload: %s", e.getMessage()));
        } catch (JsonProcessingException e) {
            return handleError(
                    String.format("Unable to create the request payload: %s", e.getMessage()));
        } catch (IOException e) {
            return handleError(
                    String.format(
                            "Server exception when submitting update to Jira: %s", e.getMessage()));
        } catch (ApiUpdateFailedException e) {
            return handleError(e.getMessage());
        } catch (RequestNotPermitted e) {
            return handleError("Your OAuth client reached Jira's limits " + e.getMessage());
        } catch (Exception e) {
            return handleError(
                    String.format(
                            "Unexpected error when submitting update to Jira: %s", e.getMessage()));
        }
    }

    /**
     * Submits an update to the Atlassian Builds or Deployments API and returns the response
     *
     * @param <ResponseEntity> Response entity, which can be either BuildApiResponse or
     *     DeploymentApiResponse
     * @param accessToken Access token generated from Atlassian API
     * @param pathParams Params to be injected to the url
     * @return Response from the API
     */
    public <ResponseEntity> PostUpdateResult<ResponseEntity> getResult(
            final String accessToken,
            final Map<String, String> pathParams,
            final String clientId,
            final Class<ResponseEntity> responseClass) {
        try {
            final Request request = getRequest(accessToken, pathParams, clientId);
            final Response response = httpClient.newCall(request).execute();

            checkForErrorResponse(response);

            final ResponseEntity responseEntity = handleResponseBody(response, responseClass);
            return new PostUpdateResult<>(responseEntity);
        } catch (NotSerializableException e) {
            return handleError(String.format("Invalid JSON payload: %s", e.getMessage()));
        } catch (JsonProcessingException e) {
            return handleError(
                    String.format("Unable to create the request payload: %s", e.getMessage()));
        } catch (IOException e) {
            return handleError(
                    String.format(
                            "Server exception when submitting update to Jira: %s", e.getMessage()));
        } catch (ApiUpdateFailedException e) {
            return handleError(e.getMessage());
        } catch (RequestNotPermitted e) {
            return handleError("Your OAuth client reached Jira's limits " + e.getMessage());
        } catch (Exception e) {
            return handleError(
                    String.format(
                            "Unexpected error when submitting update to Jira: %s", e.getMessage()));
        }
    }

    private void checkForErrorResponse(final Response response) throws IOException {
        if (!response.isSuccessful()) {
            final String message =
                    String.format(
                            "Error response code %d when submitting update to Jira",
                            response.code());
            final ResponseBody responseBody = response.body();
            if (responseBody != null) {
                log.error(
                        String.format(
                                "Error response body when submitting update to Jira: %s",
                                responseBody.string()));
                responseBody.close();
            }

            throw new ApiUpdateFailedException(message);
        }
    }

    private <ResponseEntity> ResponseEntity handleResponseBody(
            final Response response, final Class<ResponseEntity> responseClass) throws IOException {
        if (response.body() == null) {
            final String message = "Empty response body when submitting update to Jira";

            throw new ApiUpdateFailedException(message);
        }

        return objectMapper.readValue(
                response.body().bytes(),
                objectMapper.getTypeFactory().constructType(responseClass));
    }

    @VisibleForTesting
    void setApiEndpoint(final String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    private Request getRequest(
            final String cloudId,
            final String accessToken,
            final String requestPayload,
            final String clientId) {
        RequestBody body = RequestBody.create(JSON, requestPayload);
        return new Request.Builder()
                .url(String.format(this.apiEndpoint, cloudId))
                .addHeader("Authorization", "Bearer " + accessToken)
                .tag(String.class, clientId)
                .post(body)
                .build();
    }

    /*
     * Replaces placeholders with values from map
     * */
    private Request getRequest(
            final String accessToken, final Map<String, String> pathParams, final String clientId) {
        final HttpUrl url = HttpUrl.parse(this.apiEndpoint);
        // workaround to encode path segments
        final List<String> segments = url.pathSegments();
        final HttpUrl.Builder builder = url.newBuilder();
        for (int i = 0; i < segments.size(); i++) {
            builder.setPathSegment(i, StrSubstitutor.replace(segments.get(i), pathParams));
        }

        return new Request.Builder()
                .url(builder.build())
                .addHeader("Authorization", "Bearer " + accessToken)
                .tag(String.class, clientId)
                .get()
                .build();
    }

    private <T> PostUpdateResult<T> handleError(final String errorMessage) {
        return new PostUpdateResult<>(errorMessage);
    }
}
