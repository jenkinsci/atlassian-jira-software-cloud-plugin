package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoRequest;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SinglePipelineSingleDeploymentListener implements SinglePipelineListener {

    private static final Logger systemLogger =
            LoggerFactory.getLogger(SinglePipelineSingleDeploymentListener.class);

    private final WorkflowRun build;
    private final String environmentName;
    private final PrintStream pipelineLogger;

    private boolean inProgressSent = false;
    private boolean finalResultSent = false;

    private final String startFlowNodeId;
    private String endFlowNodeId = "";

    private final IssueKeyExtractor[] issueKeyExtractors;

    public SinglePipelineSingleDeploymentListener(
            final WorkflowRun build,
            final PrintStream pipelineLogger,
            final String startFlowNodeId,
            final String environmentName) {
        this.build = build;
        this.pipelineLogger = pipelineLogger;
        this.startFlowNodeId = startFlowNodeId;
        this.environmentName = environmentName;

        issueKeyExtractors =
                new IssueKeyExtractor[] {
                    new BranchNameIssueKeyExtractor(), new ChangeLogIssueKeyExtractor()
                };
    }

    @Override
    public String getBuildUrl() {
        return build.getUrl();
    }

    @Override
    public void onCompleted() {
        maybeSendDataToJira(true);
    }

    @Override
    public void onNewHead(final FlowNode flowNode) {
        if (finalResultSent) {
            return;
        }

        if (flowNode instanceof StepEndNode) {
            final StepEndNode endNode = (StepEndNode) flowNode;
            if (endNode.getStartNode().getId().equals(startFlowNodeId)) {
                endFlowNodeId = flowNode.getId();
            }
        }

        maybeSendDataToJira(false);
    }

    private void maybeSendDataToJira(final boolean isOnCompleted) {
        if (finalResultSent) {
            return;
        }

        if (extractIssueKeys().size() == 0) {
            // We don't have issueKeys at the start of the execution of the pipeline, need to wait
            // for them first
            return;
        }

        if (isOnCompleted) {
            finalResultSent = true;
            sendDeploymentsDataToJira(Optional.empty());

        } else if (canDetermineFinalResultOfEndNode()) {
            finalResultSent = true;
            sendDeploymentsDataToJira(Optional.of(endFlowNodeId));

        } else if (!startFlowNodeId.isEmpty() && !inProgressSent) {
            inProgressSent = true;
            sendDeploymentsDataToJira(Optional.empty());
        }
    }

    private static class EnvTypeMappingEntry {
        final String jiraEnvType;
        final String[] customEnvTypes;

        EnvTypeMappingEntry(final String jiraEnvType, final String[] customEnvTypes) {
            this.jiraEnvType = jiraEnvType;
            this.customEnvTypes = customEnvTypes;
        }

        boolean matches(final String testStr) {
            return Arrays.stream(customEnvTypes)
                            .filter(customEnvType -> testStr.toLowerCase().contains(customEnvType))
                            .collect(Collectors.toSet())
                            .size()
                    > 0;
        }
    }

    // We use same mapping as GitHub app does:
    // https://github.com/atlassian/github-for-jira/blob/9ce28a0cc128ec12893f28004bd74ea8bee006b4/src/transforms/deployment.ts#L49-L49
    private static final EnvTypeMappingEntry[] ENV_TYPE_MAPPING =
            new EnvTypeMappingEntry[] {
                new EnvTypeMappingEntry(
                        "development", new String[] {"development", "dev", "trunk"}),
                new EnvTypeMappingEntry(
                        "testing",
                        new String[] {
                            "testing",
                            "test",
                            "tests",
                            "tst",
                            "integration",
                            "integ",
                            "intg",
                            "int",
                            "acceptance",
                            "accept",
                            "acpt",
                            "qa",
                            "qc",
                            "control",
                            "quality"
                        }),
                new EnvTypeMappingEntry(
                        "staging",
                        new String[] {"staging", "stage", "stg", "preprod", "model", "internal"}),
                new EnvTypeMappingEntry(
                        "production", new String[] {"production", "prod", "prd", "live"})
            };

    private static String mapEnvNameToType(final String environmentName) {
        return Arrays.stream(ENV_TYPE_MAPPING)
                .filter(mapping -> mapping.matches(environmentName))
                .map(mapping -> mapping.jiraEnvType)
                .findFirst()
                .orElse("production");
    }

    private void sendDeploymentsDataToJira(final Optional<String> maybeStateNodeId) {
        final Optional<FlowNode> maybeNode =
                maybeStateNodeId.map(
                        nodeId -> {
                            try {
                                return build.getExecution().getNode(nodeId);
                            } catch (final IOException e) {
                                final String message =
                                        "cannot find node " + nodeId + ": " + e.getMessage();
                                pipelineLogger.println("[WARN] " + message);
                                systemLogger.warn(message, e);
                                return null;
                            }
                        });
        final List<JiraSendInfoResponse> allResponses =
                JiraSenderFactory.getInstance()
                        .getJiraDeploymentInfoSender()
                        .sendDeploymentInfo(
                                new JiraDeploymentInfoRequest(
                                        null,
                                        environmentName,
                                        environmentName,
                                        mapEnvNameToType(environmentName),
                                        maybeNode
                                                .map(
                                                        node ->
                                                                JenkinsToJiraStatus.getState(node)
                                                                        .value)
                                                .orElse(null),
                                        Collections.emptySet(),
                                        false,
                                        extractIssueKeys(),
                                        build));
        allResponses.forEach(
                response -> {
                    final String message = response.getStatus() + ": " + response.getMessage();
                    if (response.getStatus().isFailure) {
                        systemLogger.warn(message);
                        pipelineLogger.println("[WARN] " + message);
                    } else {
                        systemLogger.info(message);
                        pipelineLogger.println("[INFO] " + message);
                    }
                });
    }

    private boolean canDetermineFinalResultOfEndNode() {
        if (endFlowNodeId.isEmpty()) {
            return false;
        }
        try {
            final State state =
                    JenkinsToJiraStatus.getState(build.getExecution().getNode(endFlowNodeId));
            return state != State.IN_PROGRESS;
        } catch (final IOException e) {
            final String message = "cannot determine status: " + e.getMessage();
            pipelineLogger.println("[WARN] " + message);
            systemLogger.warn(message, e);
            return false;
        }
    }

    private Set<String> extractIssueKeys() {
        return Arrays.stream(issueKeyExtractors)
                .flatMap(issueKeyExtractor -> issueKeyExtractor.extractIssueKeys(build).stream())
                .collect(Collectors.toSet());
    }
}
