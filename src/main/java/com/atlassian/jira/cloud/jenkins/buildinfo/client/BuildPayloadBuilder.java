package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.TestInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class BuildPayloadBuilder {

    /**
     * Assembles a JiraBuildInfo with necessary parameters from the Jenkins context
     *
     * @param buildWrapper Jenkins context that provides project and build details
     * @param issueKeys Jira issue keys to associate the build info with
     * @return an assembled Builds payload
     */
    public static Builds getBuildPayload(
            final State jiraState, final RunWrapper buildWrapper, final Set<String> issueKeys) {

        try {

            Run<?, ?> build = buildWrapper.getRawBuild();
            if (build == null) {
                throw new RuntimeException("no build object available!");
            }

            TestInfo testInfo =
                    Optional.ofNullable(build)
                            .map(r -> r.getAction(TestResultAction.class))
                            .map(
                                    a -> {
                                        TestInfo info = new TestInfo();
                                        info.setTotalNumber(a.getTotalCount());
                                        info.setNumberPassed(
                                                a.getTotalCount()
                                                        - a.getFailCount()
                                                        - a.getSkipCount());
                                        info.setNumberFailed(a.getFailCount());
                                        info.setNumberSkipped(a.getSkipCount());

                                        return info;
                                    })
                            .orElse(null);

            return new Builds(
                    JiraBuildInfo.builder()
                            .withPipelineId(buildWrapper.getFullProjectName())
                            .withBuildNumber(buildWrapper.getNumber())
                            .withDisplayName(buildWrapper.getFullProjectName())
                            .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                            .withLabel(buildWrapper.getDisplayName())
                            .withUrl(buildWrapper.getAbsoluteUrl())
                            .withState(jiraState.value)
                            .withLastUpdated(Instant.now().toString())
                            .withIssueKeys(issueKeys)
                            .withTestInfo(testInfo)
                            .build());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
