package com.atlassian.jira.cloud.jenkins;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;

public class BaseMockServerTest extends BaseUnitTest {
    protected MockWebServer server;

    @Before
    public void setup() throws IOException {
        super.setup();
        server = new MockWebServer();
        server.start();
    }

    @After
    public void teardown() throws IOException {
        server.shutdown();
    }

}
