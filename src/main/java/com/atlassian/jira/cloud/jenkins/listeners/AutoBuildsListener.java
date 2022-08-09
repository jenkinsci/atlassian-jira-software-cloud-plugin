package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jenkinsci.plugins.workflow.cps.nodes.StepEndNode;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    private final IssueKeyExtractor issueKeyExtractor;
    private final PipelineLogger pipelineLogger;

    private boolean inProgressSent = false;
    private boolean finalResultSent = false;

    private String startFlowNodeId = "";
    private String endFlowNodeId = "";

    public AutoBuildsListener(
            final WorkflowRun run,
            final PipelineLogger logger,
            final String autoBuildsRegex,
            final IssueKeyExtractor issueKeyExtractor) {
        this.build = run;
        this.pipelineLogger = logger;
        this.autoBuildsRegex = autoBuildsRegex;
        this.issueKeyExtractor = issueKeyExtractor;
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
            pipelineLogger.debug(
                    "build start node was determined: "
                            + startNode.getId()
                            + " "
                            + startNode.getDisplayName());
            startFlowNodeId = startNode.getId();
        } else if (endNode != null
                && !startFlowNodeId.isEmpty()
                && startFlowNodeId.equals(endNode.getStartNode().getId())) {
            pipelineLogger.debug(
                    "build end node was determined: "
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
        pipelineLogger.debug("Checking for issue keys for this build ... ");
        if (issueKeyExtractor.extractIssueKeys(this.build, pipelineLogger).isEmpty()) {
            // We don't have issueKeys at the start of the execution of the pipeline, need to wait
            // for them first
            pipelineLogger.debug(
                    "No issue keys could be extracted from this build yet, not sending build event to Jira (yet).");
            return;
        }

        pipelineLogger.debug(
                "Found issue keys for this build! Deciding whether to send information to Jira now.");

        if (autoBuildsRegex.trim().isEmpty()) {
            pipelineLogger.debug("Pipeline step regex for builds is empty!");
            if (isOnCompleted) {
                pipelineLogger.debug("Sending final build event (isOnCompleted == true))");
                finalResultSent = true;
                sendBuildStatusToJira(Optional.empty());
            } else if (!inProgressSent) {
                pipelineLogger.debug(
                        "Sending in-progress build event (isOnCompleted == false, inProgressSent == false))");
                inProgressSent = true;
                sendBuildStatusToJira(Optional.empty());
            } else {
                pipelineLogger.debug(
                        "Not sending any build event (isOnCompleted == false, inProgressSent == true))");
            }
        } else {
            pipelineLogger.debug(
                    String.format(
                            "Pipeline step regex for builds is set to '%s'", autoBuildsRegex));
            if (isOnCompleted) {
                pipelineLogger.debug("Sending final build event (isOnCompleted == true))");
                finalResultSent = true;
                sendBuildStatusToJira(Optional.empty());
            } else if (canDetermineFinalResultOfEndNode()) {
                pipelineLogger.debug(
                        "Sending final build event (isOnCompleted == false, canDetermineFinalResultOfEndNode() == true))");
                finalResultSent = true;
                sendBuildStatusToJira(Optional.of(endFlowNodeId));
            } else if (!startFlowNodeId.isEmpty() && !inProgressSent) {
                pipelineLogger.debug(
                        "Sending in-progress build event (isOnCompleted == false, canDetermineFinalResultOfEndNode() == false, inProgressSent == false))");
                inProgressSent = true;
                sendBuildStatusToJira(Optional.empty());
            } else {
                pipelineLogger.debug(
                        "Not sending any build event (isOnCompleted == false, canDetermineFinalResultOfEndNode() == false, inProgressSent == true)))");
            }
        }
    }

    private boolean matchesRegex(final String autoBuildsRegex, final String displayName) {
        try {
            return Pattern.compile(autoBuildsRegex).matcher(displayName).matches();
        } catch (final PatternSyntaxException exception) {
            final String message = "PatternSyntaxException: " + exception.getMessage();
            pipelineLogger.warn(message, exception);
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
                pipelineLogger.warn(message);
                return false;
            }

            final State state =
                    JenkinsToJiraStatus.getState(build.getExecution().getNode(endFlowNodeId));
            return state != State.IN_PROGRESS;
        } catch (final IOException e) {
            final String message = "cannot determine status: " + e.getMessage();
            pipelineLogger.warn(message, e);
            return false;
        }
    }

    @SuppressFBWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "There is a null check, but SpotBugs doesn't recognize it")
    private void sendBuildStatusToJira(final Optional<String> maybeStatusNodeId) {

        if (!autoBuildsRegex.trim().isEmpty() && startFlowNodeId.isEmpty()) {
            // no node matched the regex, so we're not going to send any events to Jira
            pipelineLogger.warn(
                    String.format(
                            "No build step matched the pipeline step regex for builds ('%s'). Not sending any events to Jira",
                            autoBuildsRegex));
            return;
        }

        Optional<FlowNode> maybeStatusNode = Optional.empty();
        if (maybeStatusNodeId.isPresent()) {
            try {

                if (build.getExecution() == null
                        || build.getExecution().getNode(endFlowNodeId) == null) {
                    final String message =
                            String.format(
                                    "cannot determine status from endFlowNode '%s'", endFlowNodeId);
                    pipelineLogger.warn(message);
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
                pipelineLogger.warn(message, e);
                return;
            }
        }

        final List<JiraSendInfoResponse> allResponses =
                new JiraSenderFactory(pipelineLogger)
                        .getJiraBuildInfoSender()
                        .sendBuildInfo(
                                new MultibranchBuildInfoRequest(null, "", build, maybeStatusNode),
                                pipelineLogger);
        allResponses.forEach(
                response -> {
                    final String message = response.getStatus() + ": " + response.getMessage();
                    if (response.getStatus().isFailure) {
                        pipelineLogger.warn(message);
                    } else {
                        pipelineLogger.info(message);
                    }
                });
    }
}
