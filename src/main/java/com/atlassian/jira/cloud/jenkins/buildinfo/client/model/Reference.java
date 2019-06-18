package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

/**
 * Represents the commit and the repository it is present in.
 * This is included as part of a build.
 */
public class Reference {
    private Commit commit;
    private Ref ref;

    public Commit getCommit() {
        return commit;
    }

    public Ref getRef() {
        return ref;
    }

    public Reference setCommit(final Commit commit) {
        this.commit = commit;
        return this;
    }

    public Reference setRef(final Ref ref) {
        this.ref = ref;
        return this;
    }
}
