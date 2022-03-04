package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentsApi;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequestTestData.builds;
import static com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequestTestData.deployments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class JenkinsAppApiTest {

    @Test
    public void wrapsBuildRequestInJwt() throws IOException {
        // given
        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");
        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        BuildsApi buildsApi = new BuildsApi(client, objectMapper);

        // when
        buildsApi.sendBuildAsJwt("https://webhook.url", builds(lastUpdated), "this is a secret");

        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        // can extract payload from JWT
        String actualPayload = getPayloadFromJwt(actualRequest);
        String expectedPayload = ClassPathReader.readFromClasspath("build.json");
        JSONAssert.assertEquals(expectedPayload, actualPayload, JSONCompareMode.LENIENT);
    }

    @Test
    public void wrapsDeploymentRequestInJwt() throws IOException {
        // given
        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");
        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        DeploymentsApi deploymentsApi = new DeploymentsApi(client, objectMapper);

        // when
        deploymentsApi.sendDeploymentAsJwt("https://webhook.url", deployments(lastUpdated), "this is a secret");

        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        // can extract payload from JWT
        String actualPayload = getPayloadFromJwt(actualRequest);
        String expectedPayload = ClassPathReader.readFromClasspath("deployment.json");
        JSONAssert.assertEquals(expectedPayload, actualPayload, JSONCompareMode.LENIENT);
    }

    private String getPayloadFromJwt(Request actualRequest) throws IOException {
        Buffer buffer = new Buffer();
        actualRequest.body().writeTo(buffer);
        String actualPayload = JWT.decode(buffer.readString(StandardCharsets.UTF_8)).getClaim("request_body_json").asString();
        return actualPayload;
    }

    /**
     * Mocks the OkHttpClient to always return a successful response.
     */
    private OkHttpClient mockHttpClient() throws IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody responseBody = Mockito.mock(ResponseBody.class);
        given(call.execute()).willReturn(response);
        given(response.code()).willReturn(200);
        given(response.body()).willReturn(responseBody);
        given(response.isSuccessful()).willReturn(true);
        given(responseBody.string()).willReturn("{\"success\": \"true\"}");
        given(responseBody.bytes()).willReturn("{\"success\": \"true\"}".getBytes(StandardCharsets.UTF_8));
        given(client.newCall(any(Request.class))).willReturn(call);
        return client;
    }

}