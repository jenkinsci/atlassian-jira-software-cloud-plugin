package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import hudson.model.Run;

import static java.util.Objects.requireNonNull;

public class JiraBuildInfoRequest {

    private final String site;
    private final Run build;

    public JiraBuildInfoRequest(final String site, final Run build) {
        this.site = requireNonNull(site);
        this.build = requireNonNull(build);
    }

    public String getSite() {
        return site;
    }

    public Run getBuild() {
        return build;
    }
}
