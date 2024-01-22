package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.model.PluginSiteData;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.atlassian.jira.cloud.jenkins.util.XmlUtils;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.atlassian.jira.cloud.jenkins.util.IpAddressProvider.getIpAddress;

/**
 * A {@code SaveableListener} implementation that listens for changes in saveable objects,
 * specifically instances of {@code com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig}.
 * When changes occur, this listener processes the plugin configuration data and sends it to the
 * plugin API for further processing.
 */
@Extension
public class ConfigurationSaveableListener extends SaveableListener {
    private static final String SITE_CONFIGURATION_TAG = "atl-jsw-site-configuration";
    private final Logger logger = Logger.getLogger(ConfigurationSaveableListener.class.getName());

    private PluginConfigApi pluginConfigApi;
    private SecretRetriever secretRetriever;
    private XmlUtils xmlUtils;
    private static final String AUTO_BUILDS_REGEX_TAG = "autoBuildsRegex";
    private static final String AUTO_BUILDS_ENABLED_TAG = "autoBuildsEnabled";
    private static final String AUTO_DEPLOYMENTS_ENABLED_TAG = "autoDeploymentsEnabled";
    private static final String AUTO_DEPLOYMENTS_REGEX_TAG = "autoDeploymentsRegex";

    @Inject
    public void setSecretRetriever(final SecretRetriever secretRetriever) {
        if (secretRetriever == null) {
            throw new IllegalArgumentException("SecretRetriever cannot be null");
        }
        this.secretRetriever = secretRetriever;
    }

    @Inject
    public void setConnectionDataApi(final PluginConfigApi pluginConfigApi) {
        if (pluginConfigApi == null) {
            throw new IllegalArgumentException("PluginConfigApi cannot be null");
        }
        this.pluginConfigApi = pluginConfigApi;
    }

    @Inject
    public void setXmlUtils(final XmlUtils xmlUtils) {
        if (xmlUtils == null) {
            throw new IllegalArgumentException("XmlUtils cannot be null");
        }
        this.xmlUtils = xmlUtils;
    }

    @Override
    public void onChange(final Saveable saveable, final XmlFile file) {
        if (saveable instanceof com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig) {
            sendPluginConfigData(file);
        }
    }

    void sendPluginConfigData(final XmlFile file) {
        try {
            Element rootElement = this.xmlUtils.parseXmlFile(file);
            List<PluginSiteData> siteDataList = createSiteDataList(rootElement);
            String autoBuildsRegex =
                    this.xmlUtils.extractXmlValue(rootElement, AUTO_BUILDS_REGEX_TAG);
            boolean autoBuildsEnabled =
                    this.xmlUtils.extractBooleanValue(rootElement, AUTO_BUILDS_ENABLED_TAG);
            boolean autoDeploymentsEnabled =
                    this.xmlUtils.extractBooleanValue(rootElement, AUTO_DEPLOYMENTS_ENABLED_TAG);
            String autoDeploymentsRegex =
                    this.xmlUtils.extractXmlValue(rootElement, AUTO_DEPLOYMENTS_REGEX_TAG);

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
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "Error processing plugin configuration data", e);
        }
    }

    List<PluginSiteData> createSiteDataList(final Element rootElement) {
        List<PluginSiteData> siteDataList = new ArrayList<>();
        NodeList siteElements = rootElement.getElementsByTagName(SITE_CONFIGURATION_TAG);
        for (int i = 0; i < siteElements.getLength(); i++) {
            Element siteElement = (Element) siteElements.item(i);
            String webhookUrl = this.xmlUtils.extractXmlValue(siteElement, "webhookUrl");
            String credentialsId = this.xmlUtils.extractXmlValue(siteElement, "credentialsId");
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
                    getIpAddress(),
                    autoBuildsEnabled,
                    autoBuildsRegex,
                    autoDeploymentsEnabled,
                    autoDeploymentsRegex,
                    PipelineLogger.noopInstance());
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "Error sending data to plugin API for site: " + data.getWebhookUrl(),
                    e);
        }
    }
}
