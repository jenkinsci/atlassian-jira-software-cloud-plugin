package com.atlassian.jira.cloud.jenkins.featureflagservice;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.LDClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class FeatureFlagServiceTest {

    @Mock private LDClient ldClient;

    @Mock private OkHttpClient httpClient;

    @Mock private FeatureFlagService featureFlagService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetBooleanValue() {
        String featureFlagKey = "my-feature";
        Mockito.when(featureFlagService.getBooleanValue(featureFlagKey)).thenReturn(true);
        boolean result = featureFlagService.getBooleanValue(featureFlagKey);
        assertEquals(true, result);
    }

    @Test
    public void testGetFeatureFlag() throws IOException {
        String featureFlagKey = "my-feature";
        String expectedResponse = "Feature flag response";
        String url = "https://app.launchdarkly.com/api/v2/flags/jenkins-for-jira/my-feature";

        Request request =
                new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer <need-to-pass-in-env-var>")
                        .build();

        Response response =
                new Response.Builder()
                        .request(request)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body(okhttp3.ResponseBody.create(null, expectedResponse))
                        .build();

        Mockito.when(httpClient.newCall(request).execute()).thenReturn(response);
        String result = featureFlagService.getFeatureFlag(featureFlagKey);
        assertEquals(expectedResponse, result);
    }

    @Test
    public void testGetFeatureFlagWithNonSuccessfulResponse() throws IOException {
        String featureFlagKey = "my-feature";
        String url = "https://app.launchdarkly.com/api/v2/flags/jenkins-for-jira/my-feature";

        Request request =
                new Request.Builder()
                        .url(url)
                        .header("Authorization", "Bearer <need-to-pass-in-env-var>")
                        .build();

        Response response =
                new Response.Builder()
                        .request(request)
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(404)
                        .message("Not Found")
                        .body(okhttp3.ResponseBody.create(null, "Not Found"))
                        .build();

        Mockito.when(httpClient.newCall(request).execute()).thenReturn(response);
        String result = featureFlagService.getFeatureFlag(featureFlagKey);
        assertNull(result);
    }
}
