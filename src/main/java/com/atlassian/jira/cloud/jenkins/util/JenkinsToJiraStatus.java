package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import hudson.model.BallColor;
import hudson.model.Result;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.graph.FlowNode;

import javax.annotation.Nullable;

public final class JenkinsToJiraStatus {
    /**
     * Maps a Jenkins Build status to Jira API build/deployment status
     *
     * @return State for Jira API payload
     */
    public static State getState(@Nullable final Result jenkinsBuildResult) {
        if (jenkinsBuildResult == null) {
            return State.IN_PROGRESS;
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
