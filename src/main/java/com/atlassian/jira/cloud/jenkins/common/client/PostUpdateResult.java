package com.atlassian.jira.cloud.jenkins.common.client;

import javax.annotation.Nullable;
import java.util.Optional;

public class PostUpdateResult<T> {

    private T responseEntity;

    private String errorMessage;

    private PostUpdateResult(
            @Nullable final T responseEntity, @Nullable final String errorMessage) {
        this.responseEntity = responseEntity;
        this.errorMessage = errorMessage;
    }

    public PostUpdateResult(final T responseEntity) {
        this(responseEntity, null);
    }

    public PostUpdateResult(final String errorMessage) {
        this(null, errorMessage);
    }

    public Optional<T> getResponseEntity() {
        return Optional.ofNullable(responseEntity);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
}
