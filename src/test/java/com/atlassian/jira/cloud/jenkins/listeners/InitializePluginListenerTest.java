
package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import jenkins.model.Jenkins;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        // Given
        JiraCloudPluginConfig config = mock(JiraCloudPluginConfig.class);
        when(config.getAutoBuildsRegex()).thenReturn("Cat");
        when(config.getAutoBuildsEnabled()).thenReturn(true);
        when(config.getAutoDeploymentsRegex()).thenReturn("Dog");
        when(config.getAutoDeploymentsEnabled()).thenReturn(true);

        JiraCloudSiteConfig siteConfig = mock(JiraCloudSiteConfig.class);
        when(siteConfig.getWebhookUrl()).thenReturn("https://example.com/webhook");
        when(siteConfig.getCredentialsId()).thenReturn("creds");

        List<JiraCloudSiteConfig> sites = new ArrayList<>();
        sites.add(siteConfig);
        when(config.getSites()).thenReturn(sites);

        when(secretRetriever.getSecretFor(anyString())).thenReturn(Optional.of("secret"));

        // When
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

    @Test
    public void testSendConfigDataToJira_skipsWhenSecretIsEmpty() {
        JiraCloudPluginConfig config = mock(JiraCloudPluginConfig.class);
        JiraCloudSiteConfig siteConfig = mock(JiraCloudSiteConfig.class);
        when(siteConfig.getWebhookUrl()).thenReturn("https://example.com/webhook");
        when(siteConfig.getCredentialsId()).thenReturn("creds");
        when(config.getSites()).thenReturn(Collections.singletonList(siteConfig));
        when(secretRetriever.getSecretFor("creds")).thenReturn(Optional.empty());

        initializePluginListener.sendConfigDataToJira(config);

        verifyNoMoreInteractions(pluginConfigApi);
    }

    @Test
    public void testOnLoaded_isDeferredAfterJobLoadedMilestone() throws NoSuchMethodException {
        Method onLoaded = InitializePluginListener.class.getDeclaredMethod("onLoaded");
        Initializer initializer = onLoaded.getAnnotation(Initializer.class);

        assertThat(initializer).isNotNull();
        assertThat(initializer.after()).isEqualTo(InitMilestone.JOB_LOADED);
        assertThat(InitializePluginListener.class.getDeclaredMethods())
                .extracting(Method::getName)
                .doesNotContain("postInitialize");
    }
}
