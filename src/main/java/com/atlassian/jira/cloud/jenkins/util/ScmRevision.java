package com.atlassian.jira.cloud.jenkins.util;

import javax.annotation.Nullable;
import java.util.Objects;

public final class ScmRevision {

    private final String head;
    private final String hash;

    public ScmRevision(final String head, @Nullable final String hash) {
        this.head = Objects.requireNonNull(head);
        this.hash = hash;
    }

    public ScmRevision(final String head) {
        this(head, null);
    }

    public String getHead() {
        return head;
    }

    @Nullable
    public String getHash() {
        return hash;
    }
}
