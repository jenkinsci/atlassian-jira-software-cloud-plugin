package com.atlassian.jira.cloud.jenkins;

import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;

import java.io.IOException;

public class BaseUnitTest {

    private Injector injector = Guice.createInjector(binder ->
            // override ObjectMapper binding to use the same setting
            binder.bind(ObjectMapper.class)
                    .toInstance(new ObjectMapperProvider().objectMapper()));

    @Before
    public void setup() throws IOException {
        injector.injectMembers(this);
    }
}