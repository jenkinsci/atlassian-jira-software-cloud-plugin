package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import hudson.model.BallColor;
import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import javax.annotation.Nullable;

public final class JenkinsToJiraStatus {
    /**
     * Maps a Jenkins Build status to Jira API build/deployment status.
     *
     * <p>Note: In scripted pipelines, {@code getResult()} may return {@code null} even after the
     * build has completed successfully (see JENKINS-46325). Use {@link #getState(Result, boolean)}
     * when the completion context is known.
     *
     * @return State for Jira API payload
     */
    public static State getState(@Nullable final Result jenkinsBuildResult) {
        return getState(jenkinsBuildResult, false);
    }

    /**
     * Maps a Jenkins Build status to Jira API build/deployment status, with awareness of whether
     * the build has completed.
     *
     * <p>In scripted pipelines, {@code WorkflowRun.getResult()} returns {@code null} for successful
     * builds even after completion. When {@code isCompleted} is {@code true} and the result is
     * {@code null}, this method correctly returns {@link State#SUCCESSFUL} instead of {@link
     * State#IN_PROGRESS}.
     *
     * @param jenkinsBuildResult the build result (may be null)
     * @param isCompleted whether the build has finished execution
     * @return State for Jira API payload
     */
    public static State getState(
            @Nullable final Result jenkinsBuildResult, final boolean isCompleted) {
        if (jenkinsBuildResult == null) {
            // In Jenkins Pipeline, a null result means either "still running" or
            // "completed successfully" (scripted pipelines don't explicitly set SUCCESS).
            // Use the isCompleted flag to disambiguate.
            return isCompleted ? State.SUCCESSFUL : State.IN_PROGRESS;
        }

        if (jenkinsBuildResult == Result.SUCCESS) {
            return State.SUCCESSFUL;
        }

        if (jenkinsBuildResult == Result.FAILURE) {
            return State.FAILED;
        }

        if (jenkinsBuildResult == Result.ABORTED) {
            return State.CANCELLED;
        }

        return State.UNKNOWN;
    }

    public static State getState(final FlowNode flowNode) {
        final BallColor jenkinsNodeBallColor = flowNode.getIconColor();
        final State state;
        switch (jenkinsNodeBallColor) {
            case RED_ANIME: // anime = in_progress
            case YELLOW_ANIME:
            case BLUE_ANIME:
            case GREY_ANIME:
            case DISABLED_ANIME:
            case ABORTED_ANIME:
            case NOTBUILT_ANIME:
                state = State.IN_PROGRESS;
                break;

            case DISABLED:
            case ABORTED:
                state = State.CANCELLED;
                break;

            case RED:
                state = State.FAILED;
                break;

            case BLUE:
                state = State.SUCCESSFUL;
                break;

            default:
                state = State.UNKNOWN;
                break;
        }
        return state;
    }
}
