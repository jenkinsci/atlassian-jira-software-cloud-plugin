package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Command;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;
import hudson.AbortException;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class DeploymentPayloadBuilder {

    /**
     * Assembles a JiraDeploymentInfo with necessary parameters from the Jenkins context
     *
     * @param runWrapper Jenkins context that provides project and build details
     * @param environment Deployment environment
     * @param associations Jira issue keys or Service Ids to associate the build info with
     * @param state deployment state
     * @return an assembled Deployments payload
     */
    public static Deployments getDeploymentInfo(
            final RunWrapper runWrapper,
            final Environment environment,
            final Set<Association> associations,
            final String state,
            final List<Command> commands) {
        try {
            return new Deployments(
                    JiraDeploymentInfo.builder()
                            .withDeploymentSequenceNumber(runWrapper.getNumber())
                            .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                            .withAssociations(associations)
                            .withDisplayName(runWrapper.getDisplayName())
                            .withUrl(runWrapper.getAbsoluteUrl())
                            .withDescription(runWrapper.getDisplayName())
                            .withLastUpdated(Instant.now().toString())
                            .withLabel(runWrapper.getDisplayName())
                            .withState(state)
                            .withPipeline(getPipeline(runWrapper))
                            .withEnvironment(environment)
                            .withCommands(commands)
                            .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pipeline getPipeline(final RunWrapper runWrapper) throws AbortException {
        final Optional<? extends Run<?, ?>> build = Optional.ofNullable(runWrapper.getRawBuild());

        return Pipeline.builder()
                .withId(runWrapper.getFullProjectName())
                .withDisplayName(runWrapper.getFullProjectName())
                .withUrl(build.map(b -> b.getParent().getAbsoluteUrl()).orElse(runWrapper.getAbsoluteUrl()))
                .build();
    }
}
