package com.atlassian.jira.cloud.jenkins.tenantinfo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

/**
 * Resolves the Jira site URL provided by the Jenkins user into Cloud ID. Cloud ID is required to
 * submit build updates via API.
 */
public class CloudIdResolver {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(CloudIdResolver.class);
    public static final String TENANT_INFO_ENDPOINT = "/_edge/tenant_info";

    @Inject
    public CloudIdResolver(final OkHttpClient httpClient, final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public Optional<String> getCloudId(final String jiraSiteUrl) {
        Request request =
                new Request.Builder()
                        .url(String.format("%s%s", jiraSiteUrl, TENANT_INFO_ENDPOINT))
                        .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.warn(
                        String.format(
                                "Error response code %d when retrieving tenant info for %s",
                                response.code(), jiraSiteUrl));
                return Optional.empty();
            }

            ResponseBody body = response.body();
            if (body == null) {
                log.warn(
                        String.format(
                                "Empty response when retrieving tenant info for %s", jiraSiteUrl));
                return Optional.empty();
            }

            TenantInfo tenantInfo = objectMapper.readValue(body.bytes(), TenantInfo.class);

            return Optional.of(tenantInfo.getCloudId());
        } catch (JsonMappingException | JsonParseException e) {
            log.error("Invalid JSON when retrieving tenant info for " + jiraSiteUrl, e);
            return Optional.empty();
        } catch (IOException e) {
            log.error("Server exception when retrieving tenant info for " + jiraSiteUrl, e);
            return Optional.empty();
        }
    }
}
