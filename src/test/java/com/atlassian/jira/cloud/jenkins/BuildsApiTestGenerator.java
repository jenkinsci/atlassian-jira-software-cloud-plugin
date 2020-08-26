package com.atlassian.jira.cloud.jenkins;

import com.atlassian.jira.cloud.jenkins.common.client.BuildsApiTest;
import okhttp3.mockwebserver.MockResponse;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public class BuildsApiTestGenerator {

    public static void successfulResponse(BuildsApiTest testClass) throws IOException {
        mockResponse(testClass, "builds_api_response/accepted_builds.json", 202);
    }

    public static void failedResponseInvalidURL(BuildsApiTest testClass) throws IOException {
        mockResponse(testClass, "builds_api_response/rejected_builds_invalid_url.json", 202);
    }

    public static void failedResponseUnknownIssueKey(BuildsApiTest testClass) throws IOException {
        mockResponse(testClass, "builds_api_response/rejected_builds_unknown_issue_keys.json", 202);
    }

    public static void unauthorizedResponse(BuildsApiTest testClass) throws IOException {
        mockResponse(testClass, "builds_api_response/unauthorized_response.json", 401);
    }

    public static void serverError(BuildsApiTest testClass) throws IOException {
        testClass.server.enqueue(new MockResponse().setResponseCode(500));
    }

    private static void mockResponse(BuildsApiTest testClass, String filePath, int responseCode) throws IOException {
        String response = IOUtils.toString(testClass.getClass().getResourceAsStream(filePath), "UTF-8");
        testClass.server.enqueue(new MockResponse().setBody(response).setResponseCode(responseCode));
    }

    public static void successfulGateResponse(BuildsApiTest testClass) throws IOException {
        mockResponse(testClass, "builds_api_response/gate_check_success.json", 202);
    }
}
