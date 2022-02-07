package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * This class should listen to the events from Jenkins pipeline and send 2 builds: one in
 * "IN_PROGRESS" state and the other one in the "final" or "resulting" state.
 *
 * <p>Challenges to keep in mind:
 *
 * <p>- order of the events: we shouldn't send IN_PROGRESS event if the final result has already
 * been sent
 *
 * <p>- uncertainty about issue keys: they appear at some point during the execution of the pipeline
 * and might not be in place at the moment we need them.
 */
public class AutoBuildsListener implements SinglePipelineListener {
    private final WorkflowRun build;
    private final String autoBuildsRegex;

    private final IssueKeyExtractor[] issueKeyExtractors;
    private final PrintStream pipelineLogger;

    private boolean inProgressSent = false;
    private boolean finalResultSent = false;

    private String startFlowNodeId = "";
    private String endFlowNodeId = "";

    private static final Logger systemLogger = LoggerFactory.getLogger(AutoBuildsListener.class);

    public AutoBuildsListener(
            final WorkflowRun run, final PrintStream logger, final String autoBuildsRegex) {
        this.build = run;
        this.pipelineLogger = logger;
        this.autoBuildsRegex = autoBuildsRegex;
        issueKeyExtractors =
                new IssueKeyExtractor[] {
                    new BranchNameIssueKeyExtractor(), new ChangeLogIssueKeyExtractor()
                };
    }

    public String getBuildUrl() {
        return this.build.getUrl();
    }

    public void onCompleted() {
        maybeSendDataToJira(true);
    }

    /**
     * There are 2 cases: with regex and without.
     *
     * <p>Without regex: we are waiting for the first event with issue keys determined to send
     * IN_PROGRESS update to Jira, then we sent the final update with WorkflowRun::result in
     * onComplete().
     *
     * <p>With regex: we are waiting for a StartNode with a display name that matches regex to
     * determine the moment since which we can send "IN_PROGRESS" update to Jira, then we are
     * waiting when the closing EndNode comes, then we wait when it's BallIcon status stops
     * flickering and then we send the update.
     */
    public void onNewHead(final FlowNode flowNode) {
        if (!autoBuildsRegex.trim().isEmpty()) {
            tryToDefineStartAndStopNodeIds(flowNode);
        }

        maybeSendDataToJira(false);
    }

    private void tryToDefineStartAndStopNodeIds(final FlowNode flowNode) {
        final StepStartNode startNode =
                flowNode instanceof StepStartNode ? (StepStartNode) flowNode : null;
        final StepEndNode endNode = flowNode instanceof StepEndNode ? (StepEndNode) flowNode : null;

        if (startNode != null && matchesRegex(autoBuildsRegex, startNode.getDisplayName())) {
            pipelineLogger.println(
                    "[INFO] build start node was determined: "
                            + startNode.getId()
                            + " "
                            + startNode.getDisplayName());
            startFlowNodeId = startNode.getId();
        } else if (endNode != null
                && !startFlowNodeId.isEmpty()
                && startFlowNodeId.equals(endNode.getStartNode().getId())) {
            pipelineLogger.println(
                    "[INFO] build end node was determined: "
                            + endNode.getId()
                            + " "
                            + endNode.getDisplayName());
            endFlowNodeId = endNode.getId();
        }
    }

    /** This method is called periodically while the pipeline is working */
    private void maybeSendDataToJira(final boolean isOnCompleted) {
        if (finalResultSent) {
            return;
        }
        if (extractBuildKeys().size() == 0) {
            // We don't have issueKeys at the start of the execution of the pipeline, need to wait
            // for them first
            return;
        }

        if (autoBuildsRegex.trim().isEmpty()) {
            if (isOnCompleted) {
                finalResultSent = true;
                sendBuildStatusToJira(Optional.empty());
            } else if (!inProgressSent) {
                inProgressSent = true;
                sendBuildStatusToJira(Optional.empty());
            }
        } else {
            if (isOnCompleted) {
                finalResultSent = true;
                sendBuildStatusToJira(Optional.empty());

            } else if (canDetermineFinalResultOfEndNode()) {
                finalResultSent = true;
                sendBuildStatusToJira(Optional.of(endFlowNodeId));

            } else if (!startFlowNodeId.isEmpty() && !inProgressSent) {
                inProgressSent = true;
                sendBuildStatusToJira(Optional.empty());
            }
        }
    }

    private boolean matchesRegex(final String autoBuildsRegex, final String displayName) {
        try {
            return Pattern.compile(autoBuildsRegex).matcher(displayName).matches();
        } catch (final PatternSyntaxException exception) {
            final String message = "PatternSyntaxException: " + exception.getMessage();
            pipelineLogger.println("[WARN] " + message);
            systemLogger.warn(message, exception);
            return false;
        }
    }

    /**
     * Checks if a node with id=endFlowNodeId has finished execution and the result is ready to be
     * sent to Jira
     */
    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "There is a null check, but SpotBugs doesn't recognize it")
    private boolean canDetermineFinalResultOfEndNode() {
        if (endFlowNodeId.isEmpty()) {
            return false;
        }

        try {

            if (build.getExecution() == null
                    || build.getExecution().getNode(endFlowNodeId) == null) {
                final String message =
                        String.format(
                                "cannot determine status from endFlowNode '%s'", endFlowNodeId);
                pipelineLogger.println("[WARN] " + message);
                systemLogger.warn(message);
                return false;
            }

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

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "There is a null check, but SpotBugs doesn't recognize it")
    private void sendBuildStatusToJira(final Optional<String> maybeStatusNodeId) {
        Optional<FlowNode> maybeStatusNode = Optional.empty();
        if (maybeStatusNodeId.isPresent()) {
            try {

                if (build.getExecution() == null
                        || build.getExecution().getNode(endFlowNodeId) == null) {
                    final String message =
                            String.format(
                                    "cannot determine status from endFlowNode '%s'", endFlowNodeId);
                    pipelineLogger.println("[WARN] " + message);
                    systemLogger.warn(message);
                    return;
                }

                maybeStatusNode =
                        Optional.ofNullable(build.getExecution().getNode(maybeStatusNodeId.get()));
                if (!maybeStatusNode.isPresent()) {
                    throw new IOException(
                            "Node with id="
                                    + maybeStatusNodeId.get()
                                    + " was not found, should never happen");
                }
            } catch (final IOException e) {
                final String message =
                        "cannot find node with id="
                                + maybeStatusNodeId.get()
                                + ", data to Jira is not sent! "
                                + e.getMessage();
                pipelineLogger.println("[WARN] " + message);
                systemLogger.warn(message, e);
                return;
            }
        }

        final List<JiraSendInfoResponse> allResponses =
                JiraSenderFactory.getInstance()
                        .getJiraBuildInfoSender()
                        .sendBuildInfo(
                                new MultibranchBuildInfoRequest(null, "", build, maybeStatusNode));
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

    private Set<String> extractBuildKeys() {
        return Arrays.stream(issueKeyExtractors)
                .flatMap(issueKeyExtractor -> issueKeyExtractor.extractIssueKeys(build).stream())
                .collect(Collectors.toSet());
    }
}
