package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

public final class RunUrlProvider {

    /**
     * Resolves the absolute URL for a build run, ensuring path segments (such as branch names
     * containing '/' in multibranch pipelines) are properly URL-encoded.
     */
    public static String getRunUrl(final RunWrapper runWrapper) {
        if (runWrapper == null) {
            return "";
        }
        try {
            final Jenkins jenkins = Jenkins.getInstanceOrNull();
            final Run<?, ?> rawBuild = runWrapper.getRawBuild();
            if (jenkins != null && jenkins.getRootUrl() != null && rawBuild != null) {
                return jenkins.getRootUrl() + rawBuild.getUrl();
            }
        } catch (final Throwable ignored) {
            // Fall back if Jenkins instance is unavailable (e.g. unit tests or detached execution)
        }

        try {
            return runWrapper.getAbsoluteUrl();
        } catch (final Exception e) {
            return "";
        }
    }

    /**
     * Resolves the absolute URL for the parent job (pipeline), ensuring path segments are properly
     * URL-encoded.
     */
    public static String getPipelineUrl(final RunWrapper runWrapper) {
        if (runWrapper == null) {
            return "";
        }
        try {
            final Jenkins jenkins = Jenkins.getInstanceOrNull();
            final Run<?, ?> rawBuild = runWrapper.getRawBuild();
            if (jenkins != null
                    && jenkins.getRootUrl() != null
                    && rawBuild != null
                    && rawBuild.getParent() != null) {
                return jenkins.getRootUrl() + rawBuild.getParent().getUrl();
            }
        } catch (final Throwable ignored) {
            // Fall back
        }

        try {
            final Run<?, ?> rawBuild = runWrapper.getRawBuild();
            if (rawBuild != null && rawBuild.getParent() != null) {
                return rawBuild.getParent().getAbsoluteUrl();
            }
            return runWrapper.getAbsoluteUrl();
        } catch (final Exception e) {
            return "";
        }
    }
}
