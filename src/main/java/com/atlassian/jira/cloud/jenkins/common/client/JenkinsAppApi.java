package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import javax.inject.Inject;
import java.io.IOException;
import java.io.NotSerializableException;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public abstract class JenkinsAppApi<ResponseEntity> {

    private static final MediaType JSON_CONTENT_TYPE =
            MediaType.get("application/json; charset=utf-8");
    private static final MediaType JWT_CONTENT_TYPE = MediaType.get("application/jwt");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final int JWT_EXPIRY_SECONDS = 5 * 60;

    private final PipelineLogger pipelineLogger;

    public JenkinsAppApi(
            final OkHttpClient httpClient,
            final ObjectMapper objectMapper,
            final PipelineLogger pipelineLogger) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.pipelineLogger = pipelineLogger;
    }

    @Inject
    public JenkinsAppApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.pipelineLogger = null;
    }

    protected ResponseEntity sendRequest(
            final String webhookUrl,
            final JenkinsAppRequest jenkinsAppRequest,
            final Class<ResponseEntity> responseClass)
            throws ApiUpdateFailedException {
        try {
            final String requestPayload = objectMapper.writeValueAsString(jenkinsAppRequest);
            RequestBody body = RequestBody.create(JSON_CONTENT_TYPE, requestPayload);
            Request request = new Request.Builder().url(webhookUrl).post(body).build();
            final Response response = httpClient.newCall(request).execute();
            checkForErrorResponse(response);
            return handleResponseBody(response, responseClass);
        } catch (Exception e) {
            throw handleError(e);
        }
    }

    protected ResponseEntity sendRequestAsJwt(
            final String webhookUrl,
            final String secret,
            final JenkinsAppRequest jenkinsAppRequest,
            final Class<ResponseEntity> responseClass)
            throws ApiUpdateFailedException {
        try {
            final String requestPayload =
                    wrapInJwt(
                            jenkinsAppRequest,
                            secret,
                            Date.from(Instant.now().plusSeconds(JWT_EXPIRY_SECONDS)));
            RequestBody body = RequestBody.create(JWT_CONTENT_TYPE, requestPayload);
            Request request = new Request.Builder().url(webhookUrl).post(body).build();
            final Response response = httpClient.newCall(request).execute();
            checkForErrorResponse(response);
            return handleResponseBody(response, responseClass);
        } catch (Exception e) {
            throw handleError(e);
        }
    }

    private ApiUpdateFailedException handleError(final Exception e) {
        if (e instanceof ApiUpdateFailedException) {
            return (ApiUpdateFailedException) e;
        } else if (e instanceof NotSerializableException) {
            return new ApiUpdateFailedException(
                    String.format("Invalid JSON payload: %s", e.getMessage()), e);
        } else if (e instanceof JsonProcessingException) {
            return new ApiUpdateFailedException(
                    String.format("Unable to create the request payload: %s", e.getMessage()), e);
        } else if (e instanceof IOException) {
            return new ApiUpdateFailedException(
                    String.format(
                            "Server exception when submitting update to Jenkins app in Jira: %s",
                            e.getMessage()),
                    e);
        } else if (e instanceof RequestNotPermitted) {
            return new ApiUpdateFailedException("Rate limit reached " + e.getMessage(), e);
        } else {
            return new ApiUpdateFailedException(
                    String.format(
                            "Unexpected error when submitting update to Jira: %s", e.getMessage()),
                    e);
        }
    }

    private void checkForErrorResponse(final Response response) throws IOException {
        if (!response.isSuccessful()) {

            // log the error response
            final ResponseBody responseBody = response.body();
            String responseBodyString = null;
            if (responseBody != null) {
                responseBodyString = responseBody.string();
                pipelineLogger.error(
                        String.format(
                                "HTTP status %d when calling Jenkins app in Jira: %s",
                                response.code(), responseBodyString));
                responseBody.close();
            }

            // on a 400 we want to expose the error message to the user
            if (response.code() >= 400 && response.code() < 500 && responseBodyString != null) {
                throw new BadRequestException(responseBodyString);
            }

            // otherwise, we only want to expose the error code
            final String errorCodeMessage =
                    String.format(
                            "Error response code %d when calling Jenkins app in Jira",
                            response.code());

            throw new ApiUpdateFailedException(errorCodeMessage);
        }
    }

    private ResponseEntity handleResponseBody(
            final Response response, final Class<ResponseEntity> responseClass) throws IOException {
        ResponseBody body = response.body();
        if (body == null) {
            final String message =
                    "Empty response body when submitting update to Jenkins app in Jira";

            throw new ApiUpdateFailedException(message);
        }

        return objectMapper.readValue(
                body.bytes(), objectMapper.getTypeFactory().constructType(responseClass));
    }

    @VisibleForTesting
    protected String wrapInJwt(
            final JenkinsAppRequest request, final String secret, final Date expiryDate)
            throws JsonProcessingException {
        final String body = objectMapper.writeValueAsString(request);
        if (pipelineLogger != null) {
            pipelineLogger.info(String.format("sending request to Jenkins app in Jira: %s", body));
        }
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.create()
                .withIssuer("jenkins-plugin")
                .withAudience("jenkins-forge-app")
                .withIssuedAt(new Date())
                .withExpiresAt(expiryDate)
                .withClaim("request_body_json", body)
                .sign(algorithm);
    }
}
