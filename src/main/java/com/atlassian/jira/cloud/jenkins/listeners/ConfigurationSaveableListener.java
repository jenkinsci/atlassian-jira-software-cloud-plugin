package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.model.PluginSiteData;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.atlassian.jira.cloud.jenkins.util.IpAddressProvider.getIpAddress;
import static com.atlassian.jira.cloud.jenkins.util.XmlUtils.extractXmlValue;
import static com.atlassian.jira.cloud.jenkins.util.XmlUtils.extractBooleanValue;
import static com.atlassian.jira.cloud.jenkins.util.XmlUtils.parseXmlFile;

@Extension
public class ConfigurationSaveableListener extends SaveableListener {
    private static final String SITE_CONFIGURATION_TAG = "atl-jsw-site-configuration";
    private final Logger logger = Logger.getLogger(ConfigurationSaveableListener.class.getName());

    private PluginConfigApi pluginConfigApi;
    private SecretRetriever secretRetriever;

    @Inject
    public void setSecretRetriever(final SecretRetriever secretRetriever) {
        this.secretRetriever = secretRetriever;
    }

    @Inject
    public void setConnectionDataApi(final PluginConfigApi pluginConfigApi) {
        this.pluginConfigApi = pluginConfigApi;
    }

    @Override
    public void onChange(final Saveable saveable, final XmlFile file) {
        if (saveable instanceof com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig) {
            sendPluginConfigData(file);
        }
    }

    void sendPluginConfigData(final XmlFile file) {
        try {
            Element rootElement = parseXmlFile(file);
            List<PluginSiteData> siteDataList = createSiteDataList(rootElement);
            String autoBuildsRegex = extractXmlValue(rootElement, "autoBuildsRegex");
            boolean autoBuildsEnabled = extractBooleanValue(rootElement, "autoBuildsEnabled");
            boolean autoDeploymentsEnabled =
                    extractBooleanValue(rootElement, "autoDeploymentsEnabled");
            String autoDeploymentsRegex = extractXmlValue(rootElement, "autoDeploymentsRegex");

            for (PluginSiteData data : siteDataList) {
                Optional<String> maybeSecret =
                        secretRetriever.getSecretFor(data.getCredentialsId());
                maybeSecret.ifPresent(
                        secret ->
                                sendDataToPluginApi(
                                        data,
                                        autoBuildsRegex,
                                        autoBuildsEnabled,
                                        autoDeploymentsEnabled,
                                        autoDeploymentsRegex,
                                        secret));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing plugin configuration data", e);
            throw new RuntimeException(e);
        }
    }

    List<PluginSiteData> createSiteDataList(final Element rootElement) {
        List<PluginSiteData> siteDataList = new ArrayList<>();
        NodeList siteElements = rootElement.getElementsByTagName(SITE_CONFIGURATION_TAG);
        for (int i = 0; i < siteElements.getLength(); i++) {
            Element siteElement = (Element) siteElements.item(i);
            String webhookUrl = extractXmlValue(siteElement, "webhookUrl");
            String credentialsId = extractXmlValue(siteElement, "credentialsId");
            PluginSiteData siteData = new PluginSiteData(webhookUrl, credentialsId);
            siteDataList.add(siteData);
        }
        return siteDataList;
    }

    private void sendDataToPluginApi(
            final PluginSiteData data,
            final String autoBuildsRegex,
            final boolean autoBuildsEnabled,
            final boolean autoDeploymentsEnabled,
            final String autoDeploymentsRegex,
            final String secret) {
        try {
            pluginConfigApi.sendConnectionData(
                    data.getWebhookUrl(),
                    secret,
                    PipelineLogger.noopInstance(),
                    getIpAddress(),
                    autoBuildsEnabled,
                    autoBuildsRegex,
                    autoDeploymentsEnabled,
                    autoDeploymentsRegex);
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "Error sending data to plugin API for site: " + data.getWebhookUrl(),
                    e);
        }
    }
}
