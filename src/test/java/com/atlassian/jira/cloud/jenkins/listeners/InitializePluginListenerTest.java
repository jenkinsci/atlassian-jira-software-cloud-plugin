
package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class InitializePluginListenerTest {

    @Mock
    private PluginConfigApi pluginConfigApi;

    @Mock
    private SecretRetriever secretRetriever;

    private InitializePluginListener initializePluginListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        initializePluginListener = new InitializePluginListener();
        initializePluginListener.pluginConfigApi = pluginConfigApi;
        initializePluginListener.secretRetriever = secretRetriever;
    }

    @Test
    public void testSendConfigDataToJira() throws Exception {
        // Create a mock JiraCloudPluginConfig
        JiraCloudPluginConfig config = mock(JiraCloudPluginConfig.class);
        when(config.getAutoBuildsRegex()).thenReturn("Cat");
        when(config.getAutoBuildsEnabled()).thenReturn(true);
        when(config.getAutoDeploymentsRegex()).thenReturn("Dog");
        when(config.getAutoDeploymentsEnabled()).thenReturn(true);

        // Create a mock JiraCloudSiteConfig
        JiraCloudSiteConfig siteConfig = mock(JiraCloudSiteConfig.class);
        when(siteConfig.getWebhookUrl()).thenReturn("https://example.com/webhook");
        when(siteConfig.getCredentialsId()).thenReturn("creds");

        // Add the mock site config to the plugin config
        List<JiraCloudSiteConfig> sites = new ArrayList<>();
        sites.add(siteConfig);
        when(config.getSites()).thenReturn(sites);

        // Mock the behavior of the secret retriever
        when(secretRetriever.getSecretFor(anyString())).thenReturn(Optional.of("secret"));

        // Fire the "test"
        initializePluginListener.sendConfigDataToJira(config);

        // Then
        verify(pluginConfigApi, times(1)).sendConnectionData(
                eq("https://example.com/webhook"),
                eq("secret"),
                anyString(),
                eq(true),
                eq("Cat"),
                eq(true),
                eq("Dog"),
                any(PipelineLogger.class)
        );
    }
}