package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.TestInfo;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

public class JenkinsAppRequestTest {

    @Test
    public void serializeToJSON() throws JsonProcessingException {

        Instant lastUpdated = Instant.parse("2022-02-24T05:24:36.767Z");

        JenkinsAppRequest request =
                new JenkinsAppRequest(
                        JenkinsAppRequest.RequestType.EVENT,
                        JenkinsAppRequest.EventType.BUILD,
                        "pipelineId",
                        "pipelineName",
                        "success",
                        lastUpdated.toString(),
                        new Builds(
                                new JiraBuildInfo(
                                        "pipelineId",
                                        12,
                                        12L,
                                        "pipelineName",
                                        "description",
                                        "label",
                                        "https://url.com",
                                        "success",
                                        lastUpdated.toString(),
                                        new HashSet<>(Arrays.asList("TEST-1")),
                                        Collections.emptyList(),
                                        new TestInfo())));

        ObjectMapper mapper = new ObjectMapperProvider().objectMapper();
        String json = mapper.writeValueAsString(request);
        String expected =
                new BufferedReader(
                                new InputStreamReader(getClass().getResourceAsStream("build.json")))
                        .lines()
                        .collect(Collectors.joining("\n"));
        JSONAssert.assertEquals(expected, json, JSONCompareMode.LENIENT);
    }
}
