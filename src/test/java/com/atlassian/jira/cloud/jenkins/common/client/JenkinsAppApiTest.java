package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.GatingStatusApi;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusRequest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentsApi;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;
import com.atlassian.jira.cloud.jenkins.ping.JenkinsAppPingRequest;
import com.atlassian.jira.cloud.jenkins.ping.PingApi;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.atlassian.jira.cloud.jenkins.util.PipelineLogger;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Calendar;

import static com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequestTestData.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

public class JenkinsAppApiTest {

    private final Logger logger = LoggerFactory.getLogger(JenkinsAppApiTest.class);

    /** Generates an example JWT that can be used in other tests. */
    @Test
    public void generateExampleBuildsJwt() throws IOException {
        Calendar farFuture = Calendar.getInstance();
        farFuture.set(2999, 1, 1);

        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        BuildsApi buildsApi = new BuildsApi(client, objectMapper, PipelineLogger.noopInstance());

        String jwt =
                buildsApi.wrapInJwt(
                        jenkinsAppEventRequest(
                                Instant.now(),
                                JenkinsAppEventRequest.EventType.BUILD,
                                builds(Instant.now())),
                        "this is a secret",
                        farFuture.getTime());

        logger.info("Here's your JWT: {}", jwt);
    }

    @Test
    public void generateExampleDeploymentsJwt() throws IOException {
        Calendar farFuture = Calendar.getInstance();
        farFuture.set(2999, 1, 1);

        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        DeploymentsApi deploymentsApi =
                new DeploymentsApi(client, objectMapper, PipelineLogger.noopInstance());

        String jwt =
                deploymentsApi.wrapInJwt(
                        jenkinsAppEventRequest(
                                Instant.now(),
                                JenkinsAppEventRequest.EventType.DEPLOYMENT,
                                deployments(Instant.now())),
                        "this is a secret",
                        farFuture.getTime());

        logger.info("Here's your JWT: {}", jwt);
    }

    @Test
    public void generateExamplePingJwt() throws IOException {
        Calendar farFuture = Calendar.getInstance();
        farFuture.set(2999, 1, 1);

        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        PingApi pingApi = new PingApi(client, objectMapper, PipelineLogger.noopInstance());

        String jwt =
                pingApi.wrapInJwt(
                        new JenkinsAppPingRequest(), "this is a secret", farFuture.getTime());

        logger.info("Here's your JWT: {}", jwt);
    }

    @Test
    public void generateExampleGatingStatusJwt() throws IOException {
        Calendar farFuture = Calendar.getInstance();
        farFuture.set(2999, 1, 1);

        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        GatingStatusApi gatingStatusApi =
                new GatingStatusApi(client, objectMapper, PipelineLogger.noopInstance());

        String jwt =
                gatingStatusApi.wrapInJwt(
                        new GatingStatusRequest("4711", "0815", "prod"),
                        "this is a secret",
                        farFuture.getTime());

        logger.info("Here's your JWT: {}", jwt);
    }

    @Test
    public void wrapsBuildRequestInJwt() throws IOException {
        // given
        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");
        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        BuildsApi buildsApi = new BuildsApi(client, objectMapper, PipelineLogger.noopInstance());

        // when
        buildsApi.sendBuildAsJwt("https://webhook.url", builds(lastUpdated), "this is a secret");

        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        // can extract payload from JWT
        String actualRequestPayload = getPayloadFromJwt(actualRequest);
        String expectedRequestPayload = ClassPathReader.readFromClasspath("build.json");
        JSONAssert.assertEquals(
                expectedRequestPayload, actualRequestPayload, JSONCompareMode.LENIENT);
    }

    @Test
    public void wrapsDeploymentRequestInJwt() throws IOException {
        // given
        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");
        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        DeploymentsApi deploymentsApi =
                new DeploymentsApi(client, objectMapper, PipelineLogger.noopInstance());

        // when
        DeploymentApiResponse response =
                deploymentsApi.sendDeploymentAsJwt(
                        "https://webhook.url", deployments(lastUpdated), "this is a secret");

        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        // can extract payload from JWT
        String actualRequestPayload = getPayloadFromJwt(actualRequest);
        String expectedRequestPayload = ClassPathReader.readFromClasspath("deployment.json");
        JSONAssert.assertEquals(
                expectedRequestPayload, actualRequestPayload, JSONCompareMode.LENIENT);
    }

    @Test
    public void wrapsPingRequestInJwt() throws IOException {
        // given
        OkHttpClient client = mockHttpClient();
        ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        ObjectMapper objectMapper = objectMapperProvider.objectMapper();
        PingApi pingApi = new PingApi(client, objectMapper, PipelineLogger.noopInstance());

        // when
        pingApi.sendPing("https://webhook.url", "this is a secret");

        // then
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(client).newCall(requestCaptor.capture());
        Request actualRequest = requestCaptor.getValue();

        // can extract payload from JWT
        String actualRequestPayload = getPayloadFromJwt(actualRequest);
        String expectedRequestPayload = ClassPathReader.readFromClasspath("ping.json");
        JSONAssert.assertEquals(
                expectedRequestPayload, actualRequestPayload, JSONCompareMode.LENIENT);
    }

    private String getPayloadFromJwt(final Request actualRequest) throws IOException {
        Buffer buffer = new Buffer();
        actualRequest.body().writeTo(buffer);
        String actualPayload =
                JWT.decode(buffer.readString(StandardCharsets.UTF_8))
                        .getClaim("request_body_json")
                        .asString();
        return actualPayload;
    }

    /** Mocks the OkHttpClient to always return a successful response. */
    private OkHttpClient mockHttpClient() throws IOException {
        OkHttpClient client = Mockito.mock(OkHttpClient.class);
        Call call = Mockito.mock(Call.class);
        Response response = Mockito.mock(Response.class);
        ResponseBody responseBody = Mockito.mock(ResponseBody.class);
        given(call.execute()).willReturn(response);
        given(response.code()).willReturn(200);
        given(response.body()).willReturn(responseBody);
        given(response.isSuccessful()).willReturn(true);
        given(responseBody.string()).willReturn("{\"success\": true}");
        given(responseBody.bytes())
                .willReturn("{\"success\": true}".getBytes(StandardCharsets.UTF_8));
        given(client.newCall(any(Request.class))).willReturn(call);
        return client;
    }
}
