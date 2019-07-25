package com.atlassian.jira.cloud.jenkins.config;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraCloudPluginConfigTest {

    private static final String SITE = "example.atlassian.net";
    private static final String CLIENT_ID = "clientId";
    private static final String CREDENTIALS_ID = "credsId";

    @Rule public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void testDoesNotGetSiteConfig_whenSiteIsNotConfigured() {
        // when
        final Optional<JiraCloudSiteConfig> config =
                JiraCloudPluginConfig.getJiraCloudSiteConfig(SITE);

        // then
        assertThat(config.isPresent()).isFalse();
    }

    @Test
    public void testGetSiteConfig_whenSiteIsConfigured() {
        // given
        JiraCloudSiteConfig siteConfig = new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIALS_ID);
        JiraCloudPluginConfig.get().setSites(ImmutableList.of(siteConfig));

        // when
        final Optional<JiraCloudSiteConfig> config =
                JiraCloudPluginConfig.getJiraCloudSiteConfig(SITE);

        // then
        assertSiteConfig(config.get(), SITE, CREDENTIALS_ID);
    }

    @Test
    public void testGetSiteConfig_whenSiteIsNotProvided() {
        // given
        JiraCloudSiteConfig siteConfig = new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIALS_ID);
        JiraCloudPluginConfig.get().setSites(ImmutableList.of(siteConfig));

        // when
        final Optional<JiraCloudSiteConfig> config =
                JiraCloudPluginConfig.getJiraCloudSiteConfig(null);

        // then
        assertSiteConfig(config.get(), SITE, CREDENTIALS_ID);
    }

    @Test
    public void testGetSiteConfig_whenMultipleSitesConfigured() {
        // given
        JiraCloudSiteConfig siteConfig1 = new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIALS_ID);
        JiraCloudSiteConfig siteConfig2 =
                new JiraCloudSiteConfig("foobar.atlassian.net", "clientId", "credsId");
        JiraCloudPluginConfig.get().setSites(ImmutableList.of(siteConfig1, siteConfig2));

        // when
        // when
        final Optional<JiraCloudSiteConfig> config =
                JiraCloudPluginConfig.getJiraCloudSiteConfig(SITE);

        // then
        assertSiteConfig(config.get(), SITE, CREDENTIALS_ID);
    }

    private void assertSiteConfig(
            final JiraCloudSiteConfig config,
            final String expectedSite,
            final String expectedCredentialsId) {
        assertThat(config.getSite()).isEqualTo(expectedSite);
        assertThat(config.getCredentialsId()).isEqualTo(expectedCredentialsId);
    }
}
