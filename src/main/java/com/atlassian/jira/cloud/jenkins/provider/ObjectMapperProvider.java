package com.atlassian.jira.cloud.jenkins.provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Provides;

/**
 * ObjectMapper with a configuration not to fail on unknown properties.
 * To gracefully handle any new fields added in the API later.
 */
public class ObjectMapperProvider {

    private final ObjectMapper objectMapper;

    public ObjectMapperProvider() {
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Provides
    public ObjectMapper objectMapper() {
        return objectMapper;
    }
}
