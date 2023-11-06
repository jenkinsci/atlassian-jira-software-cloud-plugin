
package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.model.PluginSiteData;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.atlassian.jira.cloud.jenkins.util.XmlUtils;
import hudson.XmlFile;
import hudson.model.Saveable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class ConfigurationSaveableListenerTest {
    @Mock
    private SecretRetriever secretRetriever;

    @Mock
    private PluginConfigApi pluginConfigApi;

    @Mock
    private XmlUtils xmlUtils;

    private ConfigurationSaveableListener listener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        listener = new ConfigurationSaveableListener();
        listener.setSecretRetriever(secretRetriever);
        listener.setConnectionDataApi(pluginConfigApi);
        listener.setXmlUtils(xmlUtils);
    }

    @Test
    public void testOnChange() {
        // Arrange
        Saveable saveable = Mockito.mock(JiraCloudPluginConfig.class);
        XmlFile file = Mockito.mock(XmlFile.class);

        ConfigurationSaveableListener configurationSaveableListener = new ConfigurationSaveableListener();
        ConfigurationSaveableListener spyListener = Mockito.spy(configurationSaveableListener);

        Mockito.doNothing().when(spyListener).sendPluginConfigData(file);

        // Act
        spyListener.onChange(saveable, file);

        // Assert
        Mockito.verify(spyListener).sendPluginConfigData(file);
    }

    @Test
    public void testSendPluginConfigData_SuccessfullySendsDataToPluginApi() throws Exception {
        // Arrange
        XmlFile file = mock(XmlFile.class);
        Element rootElement = mock(Element.class);
        NodeList siteElements = mock(NodeList.class);
        Element siteElement = mock(Element.class);

        when(xmlUtils.parseXmlFile(file)).thenReturn(rootElement);
        when(xmlUtils.extractXmlValue(rootElement, "autoBuildsRegex")).thenReturn("regex");
        when(xmlUtils.extractBooleanValue(rootElement, "autoBuildsEnabled")).thenReturn(true);
        when(xmlUtils.extractBooleanValue(rootElement, "autoDeploymentsEnabled")).thenReturn(true);
        when(xmlUtils.extractXmlValue(rootElement, "autoDeploymentsRegex")).thenReturn("regex");
        when(rootElement.getElementsByTagName("atl-jsw-site-configuration")).thenReturn(siteElements);
        when(siteElements.getLength()).thenReturn(1);
        when(siteElements.item(0)).thenReturn(siteElement);
        when(xmlUtils.extractXmlValue(siteElement, "webhookUrl")).thenReturn("url");
        when(xmlUtils.extractXmlValue(siteElement, "credentialsId")).thenReturn("id");
        when(secretRetriever.getSecretFor(eq("id"))).thenReturn(Optional.of("secret"));
        when(xmlUtils.parseXmlFile(file)).thenReturn(rootElement);
        when(xmlUtils.extractXmlValue(rootElement, "autoBuildsRegex")).thenReturn("regex");
        when(xmlUtils.extractBooleanValue(rootElement, "autoBuildsEnabled")).thenReturn(true);
        when(xmlUtils.extractBooleanValue(rootElement, "autoDeploymentsEnabled")).thenReturn(true);
        when(xmlUtils.extractXmlValue(rootElement, "autoDeploymentsRegex")).thenReturn("regex");

        // Act
        listener.sendPluginConfigData(file);

        // Assert
        verify(pluginConfigApi).sendConnectionData(
                eq("url"),
                eq("secret"),
                any(PipelineLogger.class),
                anyString(),
                eq(true),
                eq("regex"),
                eq(true),
                eq("regex")
        );
    }

}