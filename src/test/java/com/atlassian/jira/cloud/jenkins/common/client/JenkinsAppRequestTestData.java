package com.atlassian.jira.cloud.jenkins.common.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Commit;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Ref;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Reference;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.TestInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.AssociationType;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class JenkinsAppRequestTestData {

    public static JenkinsAppRequest jenkinsAppEventRequest(
            Instant lastUpdated,
            JenkinsAppRequest.EventType eventType,
            JiraRequest payload) {
        return new JenkinsAppRequest(
                JenkinsAppRequest.RequestType.EVENT,
                eventType,
                "pipelineId",
                "pipelineName",
                "success",
                lastUpdated.toString(),
                payload);
    }

    public static Builds builds(Instant lastUpdated) {
        Reference ref = new Reference();
        ref.setCommit(new Commit("cafebabe", "https://repo.url"));
        ref.setRef(new Ref("refname", "https:ref.uri"));

        return new Builds(
                new JiraBuildInfo(
                        "pipelineId",
                        12,
                        12L,
                        "pipelineName",
                        "description",
                        "label",
                        "https://url.com",
                        "successful",
                        lastUpdated.toString(),
                        new HashSet<>(Arrays.asList("JEN-25")),
                        Collections.singletonList(ref),
                        new TestInfo(0, 0, 0, 0)));
    }

    public static Deployments deployments(Instant lastUpdated) {
        Set<String> issueKeys = new HashSet<>();
        issueKeys.add("JEN-25");
        Association association = new Association(AssociationType.ISSUE_KEYS, issueKeys);

        return new Deployments(
                new JiraDeploymentInfo(
                        42,
                        45L,
                        Collections.singleton(association),
                        "pipelineName",
                        "https://url.com",
                        "description",
                        lastUpdated.toString(),
                        "label",
                        "successful",
                        new Pipeline(
                                "pipelineId", "pipelineName", "https://url.com"),
                        new Environment("stg-east", "Staging east", "staging"),
                        Collections.emptyList()));
    }

}
