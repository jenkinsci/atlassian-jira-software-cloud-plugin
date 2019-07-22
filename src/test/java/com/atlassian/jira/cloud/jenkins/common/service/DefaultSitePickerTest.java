package com.atlassian.jira.cloud.jenkins.common.service;

import com.atlassian.jira.cloud.jenkins.common.config.DefaultSitePicker;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSitePickerTest implements DefaultSitePicker {

    private static final String SITE_URL = "mysite.atlassian.net";
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();
    private PrintStream mockLogger;

    @Before
    public void setup() {
        mockLogger = mock(PrintStream.class);
        JiraCloudPluginConfig.get().setSites(Collections.emptyList());
    }

    @Test
    public void testPickDefaultSite_useProvidedSite() {
        final Optional<String> site = pickDefaultSite(SITE_URL);

        assertThat(site.get()).isEqualTo("mysite.atlassian.net");
    }

    @Test
    public void testPickDefaultSite_useSiteFromConfig() {
        //given
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(new JiraCloudSiteConfig(SITE_URL, "client_id", "credential_id")));

        // when
        final Optional<String> site = pickDefaultSite(null);

        // then
        assertThat(site.get()).isEqualTo("mysite.atlassian.net");
    }

    @Test
    public void testPickDefaultSite_noSiteInConfig() {
        // when
        final Optional<String> site = pickDefaultSite(null);

        // verify
        verify(mockLogger).println("Could not pick a default site because no site has been configured.");
        assertThat(site).isEmpty();
    }

    @Test
    public void testPickDefaultSite_multipleSitesInConfig() {
        // given
        //given
        JiraCloudPluginConfig.get().setSites(ImmutableList.of(
                new JiraCloudSiteConfig(SITE_URL, "client_id", "credential_id"),
                new JiraCloudSiteConfig("site2.atlassian.net", "client_id", "credential_id")
        ));

        // when
        final Optional<String> site = pickDefaultSite(null);

        // verify
        verify(mockLogger).println("Could not pick a default site because multiple sites have been configured.");
        assertThat(site).isEmpty();
    }

    @Nullable
    @Override
    public PrintStream getLogger() {
        return mockLogger;
    }
}
