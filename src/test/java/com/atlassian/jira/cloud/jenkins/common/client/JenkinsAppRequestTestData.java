package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.TestInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class JenkinsAppRequestTestData {

    public static JenkinsAppRequest jenkinsAppEventRequest(
            Instant lastUpdated,
            JenkinsAppEventRequest.EventType eventType,
            JiraRequest payload) {
        return new JenkinsAppEventRequest(
                eventType,
                "pipelineId",
                "pipelineName",
                "success",
                lastUpdated.toString(),
                payload);
    }

    public static Builds builds(Instant lastUpdated) {
        return new Builds(
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
                        new TestInfo()));
    }

    public static Deployments deployments(Instant lastUpdated) {
        return new Deployments(
                new JiraDeploymentInfo(
                        42,
                        45L,
                        Collections.emptySet(),
                        "pipelineName",
                        "https://url.com",
                        "description",
                        lastUpdated.toString(),
                        "label",
                        "success",
                        new Pipeline(
                                "pipelineId", "pipelineName", "https://url.com"),
                        new Environment("stg-east", "Staging east", "staging"),
                        Collections.emptyList()));
    }

}
