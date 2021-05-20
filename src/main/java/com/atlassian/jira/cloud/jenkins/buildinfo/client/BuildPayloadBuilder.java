package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.TestInfo;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.AbortException;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class BuildPayloadBuilder {

    private static final Logger log = LoggerFactory.getLogger(BuildPayloadBuilder.class);

    /**
     * Assembles a JiraBuildInfo with necessary parameters from the Jenkins context
     *
     * @param buildWrapper Jenkins context that provides project and build details
     * @param issueKeys Jira issue keys to associate the build info with
     * @return an assembled Builds payload
     */
    public static Builds getBuildPayload(
            final RunWrapper buildWrapper, final Set<String> issueKeys) {

        try {

            TestInfo testInfo = Optional.ofNullable(buildWrapper.getRawBuild())
                    .map(r -> r.getAction(TestResultAction.class))
                    .map(a -> {
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

            return new Builds(JiraBuildInfo.builder()
                    .withPipelineId(buildWrapper.getFullProjectName())
                    .withBuildNumber(buildWrapper.getNumber())
                    .withDisplayName(buildWrapper.getFullProjectName())
                    .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                    .withLabel(buildWrapper.getDisplayName())
                    .withUrl(buildWrapper.getAbsoluteUrl())
                    .withState(JenkinsToJiraStatus.getStatus(buildWrapper.getCurrentResult()))
                    .withLastUpdated(Instant.now().toString())
                    .withIssueKeys(issueKeys)
                    .withTestInfo(testInfo)
                    .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }
}
