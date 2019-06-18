package com.atlassian.jira.cloud.jenkins.auth;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;

public class AccessTokenRetriever {

    private static final Logger log = LoggerFactory.getLogger(AccessTokenRetriever.class);

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private String accessTokenEndpoint;
    private static final String ACCESS_TOKEN_ENDPOINT = ATLASSIAN_API_URL + "/oauth/token";
    private static final String GRANT_TYPE = "client_credentials";

    @Inject
    public AccessTokenRetriever(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.accessTokenEndpoint = ACCESS_TOKEN_ENDPOINT;
    }

    /**
     * Submit the Client ID, Secret and Grant type to retrieve an access token
     *
     * @param appCredential Contains Client ID and Secret
     * @return Access token
     */
    public Optional<String> getAccessToken(final AppCredential appCredential) {
        RequestBody formBody =
                new FormBody.Builder()
                        .add("client_id", appCredential.getClientId())
                        .add("client_secret", appCredential.getClientSecret())
                        .add("grant_type", GRANT_TYPE)
                        .build();
        Request request = new Request.Builder().url(accessTokenEndpoint).post(formBody).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn(
                        String.format(
                                "Error response code %d when retrieving access token for %s",
                                response.code(), appCredential.getClientId()));
                return Optional.empty();
            }

            if (response.body() == null) {
                log.warn(
                        String.format(
                                "Empty response when retrieving access token for %s",
                                appCredential.getClientId()));
                return Optional.empty();
            }

            AccessTokenResponse accessTokenResponse =
                    objectMapper.readValue(response.body().bytes(), AccessTokenResponse.class);

            return Optional.ofNullable(accessTokenResponse.getAccessToken());
        } catch (JsonMappingException | JsonParseException e) {
            log.error(
                    String.format(
                            "Invalid JSON when retrieving access token for %s",
                            appCredential.getClientId()),
                    e);
            return Optional.empty();
        } catch (IOException e) {
            log.error(
                    String.format(
                            "Server exception when access token for %s",
                            appCredential.getClientId()),
                    e);
            return Optional.empty();
        }
    }

    @VisibleForTesting
    protected void setAccessTokenEndpoint(final String accessTokenEndpoint) {
        this.accessTokenEndpoint = accessTokenEndpoint;
    }
}
