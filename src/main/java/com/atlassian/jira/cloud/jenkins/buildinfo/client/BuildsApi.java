package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
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

public class BuildsApi {

    private static final Logger log = LoggerFactory.getLogger(BuildsApi.class);
    private static final String BUILDS_API_URL =
            ATLASSIAN_API_URL + "/jira/builds/0.1/cloud/%s/bulk";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String buildsApiEndpoint;

    @Inject
    public BuildsApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.buildsApiEndpoint = BUILDS_API_URL;
    }

    /**
     * Submits a build to the Atlassian Builds API and returns the response
     *
     * @param cloudId Jira Cloud Id
     * @param accessToken Access token generated from Atlassian API
     * @param jiraSiteUrl Jira site URL
     * @param jiraBuildInfo An assembled JiraBuildInfo
     * @return Response from the API
     */
    public Optional<BuildApiResponse> postBuildUpdate(
            final String cloudId,
            final String accessToken,
            final String jiraSiteUrl,
            final JiraBuildInfo jiraBuildInfo) {
        try {
            final String buildsPayload = objectMapper.writeValueAsString(new Builds(jiraBuildInfo));
            final Request request = getRequest(cloudId, accessToken, buildsPayload);
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

    private BuildApiResponse handleResponseBody(final String jiraSiteUrl, final Response response)
            throws IOException {
        if (response.body() == null) {
            final String message =
                    String.format("Empty response body when submitting builds for %s", jiraSiteUrl);
            handleError(message);
        }

        final BuildApiResponse buildApiResponse =
                objectMapper.readValue(response.body().bytes(), BuildApiResponse.class);

        if (!buildApiResponse.getRejectedBuilds().isEmpty()) {
            log.warn(
                    String.format(
                            "Rejected builds when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getRejectedBuilds()));
        }

        if (!buildApiResponse.getAcceptedBuilds().isEmpty()) {
            log.info(
                    String.format(
                            "Accepted builds when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getAcceptedBuilds()));
        }

        if (!buildApiResponse.getUnknownIssueKeys().isEmpty()) {
            log.warn(
                    String.format(
                            "Unknown issue keys when submitting builds for %s: %s",
                            jiraSiteUrl, buildApiResponse.getUnknownIssueKeys()));
        }

        return buildApiResponse;
    }

    void setBuildsApiEndpoint(final String buildsApiEndpoint) {
        this.buildsApiEndpoint = buildsApiEndpoint;
    }

    private Request getRequest(
            final String cloudId, final String accessToken, final String buildsPayload) {
        RequestBody body = RequestBody.create(JSON, buildsPayload);
        return new Request.Builder()
                .url(String.format(this.buildsApiEndpoint, cloudId))
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();
    }

    private void handleError(final String message) {
        log.error(message);
        throw new BuildUpdateFailedException(message);
    }
}
