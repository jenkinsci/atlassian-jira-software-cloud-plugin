package com.atlassian.jira.cloud.jenkins;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;

import java.io.IOException;

public class BaseUnitTest {

    private Injector injector = Guice.createInjector();

    @Before
    public void setup () throws IOException {
        injector.injectMembers(this);
    }
}