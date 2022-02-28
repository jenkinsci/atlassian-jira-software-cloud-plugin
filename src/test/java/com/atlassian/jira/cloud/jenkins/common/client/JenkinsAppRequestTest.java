package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.time.Instant;

import static com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequestTestData.*;

public class JenkinsAppRequestTest {

    @Test
    public void serializeBuildEventToJSON() throws JsonProcessingException {

        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");

        JenkinsAppRequest request = jenkinsAppEventRequest(
                lastUpdated,
                JenkinsAppRequest.EventType.BUILD,
                builds(lastUpdated));

        ObjectMapper mapper = new ObjectMapperProvider().objectMapper();
        String json = mapper.writeValueAsString(request);
        String expected = ClassPathReader.readFromClasspath("build.json");
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }

    @Test
    public void serializeDeploymentEventToJSON() throws JsonProcessingException {

        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");

        JenkinsAppRequest request = jenkinsAppEventRequest(
                lastUpdated,
                JenkinsAppRequest.EventType.DEPLOYMENT,
                deployments(lastUpdated));

        ObjectMapper mapper = new ObjectMapperProvider().objectMapper();
        String json = mapper.writeValueAsString(request);
        System.out.println(json);
        String expected = ClassPathReader.readFromClasspath("deployment.json");
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }
}
