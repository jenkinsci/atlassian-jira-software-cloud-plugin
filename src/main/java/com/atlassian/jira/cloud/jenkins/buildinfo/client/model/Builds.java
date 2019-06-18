package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

import java.util.Collections;
import java.util.List;

/**
 * This represents the payload for the API request to submit list of builds
 */
public class Builds {
    private List<JiraBuildInfo> builds;

    public Builds(final JiraBuildInfo jiraBuildInfo) {
        this.builds = Collections.singletonList(jiraBuildInfo);
    }

    public List<JiraBuildInfo> getBuilds() {
        return builds;
    }
}
