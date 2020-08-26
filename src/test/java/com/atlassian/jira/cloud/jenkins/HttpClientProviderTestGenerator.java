package com.atlassian.jira.cloud.jenkins;

import com.atlassian.jira.cloud.jenkins.provider.HttpClientProviderTest;
import okhttp3.mockwebserver.MockResponse;

public class HttpClientProviderTestGenerator {

    public static void succeedWith2XXOnInitialAttempt(HttpClientProviderTest testClass) {
        mockResponse(testClass, 202);
    }

    public static void failWith4XXOnInitialAttempt(HttpClientProviderTest testClass) {
        mockResponse(testClass, 404);
    }

    public static void failWith503AndThenSucceed2XX(HttpClientProviderTest testClass) {
        mockResponse(testClass, 503);
        mockResponse(testClass, 202);
    }

    public static void failWith503ForAllAttempts(HttpClientProviderTest testClass) {
        mockResponse(testClass, 503);
        mockResponse(testClass, 503);
        mockResponse(testClass, 503);
        mockResponse(testClass, 503);
    }

    private static void mockResponse(HttpClientProviderTest testClass, int responseCode) {
        testClass.server.enqueue(
                new MockResponse().setBody("test-response").setResponseCode(responseCode));
    }

    public static void failWith404ForAllAttempts(HttpClientProviderTest testClass) {
        mockResponse(testClass, 404);
        mockResponse(testClass, 404);
        mockResponse(testClass, 404);
        mockResponse(testClass, 404);
    }

    public static void failWith404AndThenSucceed2XX(HttpClientProviderTest testClass) {
        mockResponse(testClass, 404);
        mockResponse(testClass, 200);
    }

}
