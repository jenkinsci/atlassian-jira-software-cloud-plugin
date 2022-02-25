package com.atlassian.jira.cloud.jenkins.common.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
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

public abstract class JenkinsAppApi<ResponseEntity> {

    private static final Logger log = LoggerFactory.getLogger(JenkinsAppApi.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Inject
    public JenkinsAppApi(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    protected ResponseEntity sendRequest(
            final String webhookUrl,
            final JenkinsAppRequest jenkinsAppRequest,
            final Class<ResponseEntity> responseClass)
            throws ApiUpdateFailedException {
        try {
            final String requestPayload = objectMapper.writeValueAsString(jenkinsAppRequest);
            final Request request = createRequest(webhookUrl, requestPayload);
            final Response response = httpClient.newCall(request).execute();

            checkForErrorResponse(response);

            return handleResponseBody(response, responseClass);
        } catch (NotSerializableException e) {
            throw new ApiUpdateFailedException(
                    String.format("Invalid JSON payload: %s", e.getMessage()));
        } catch (JsonProcessingException e) {
            throw new ApiUpdateFailedException(
                    String.format("Unable to create the request payload: %s", e.getMessage()));
        } catch (IOException e) {
            throw new ApiUpdateFailedException(
                    String.format(
                            "Server exception when submitting update to Jenkins app in Jira: %s",
                            e.getMessage()));
        } catch (RequestNotPermitted e) {
            throw new ApiUpdateFailedException("Rate limit reached " + e.getMessage());
        } catch (Exception e) {
            throw new ApiUpdateFailedException(
                    String.format(
                            "Unexpected error when submitting update to Jira: %s", e.getMessage()));
        }
    }

    private Request createRequest(final String webhookUrl, final String requestPayload) {
        RequestBody body = RequestBody.create(JSON, requestPayload);
        return new Request.Builder().url(webhookUrl).post(body).build();
    }

    private void checkForErrorResponse(final Response response) throws IOException {
        if (!response.isSuccessful()) {
            final String message =
                    String.format(
                            "Error response code %d when submitting update to Jenkins app in Jira",
                            response.code());
            final ResponseBody responseBody = response.body();
            if (responseBody != null) {
                log.error(
                        String.format(
                                "Error response body when submitting update to Jenkins app in Jira: %s",
                                responseBody.string()));
                responseBody.close();
            }

            throw new ApiUpdateFailedException(message);
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
}
