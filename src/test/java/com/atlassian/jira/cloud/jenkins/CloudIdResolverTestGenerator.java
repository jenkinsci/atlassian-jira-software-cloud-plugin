package com.atlassian.jira.cloud.jenkins;

import okhttp3.mockwebserver.MockResponse;

public class CloudIdResolverTestGenerator {

    public static MockResponse successfulResponse() {
        return new MockResponse().setBody("{\"cloudId\":\"cloud-id\"}").setResponseCode(200);
    }

    public static MockResponse notFound() {
        return new MockResponse().setResponseCode(404);
    }

    public static MockResponse serverError() {
        return new MockResponse().setResponseCode(500);
    }

    public static MockResponse invalidResponseBody() {
        return new MockResponse().setBody("not_json").setResponseCode(200);
    }

    public static MockResponse cloudIdAbsent() {
        return new MockResponse().setBody("{\"foo\":\"bar\"}").setResponseCode(200);
    }

}
