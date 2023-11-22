package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.IpAddressProvider;
import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ConfigurationSaveableListener extends SaveableListener {

    private static final Logger LOGGER =
            Logger.getLogger(ConfigurationSaveableListener.class.getName());

    private transient PluginConfigApi pluginConfigApi;

    @Inject
    public void setConnectionDataApi(final PluginConfigApi pluginConfigApi) {
        this.pluginConfigApi = pluginConfigApi;
    }

    @Override
    public void onChange(final Saveable saveable, final XmlFile file) {
        if (!(saveable instanceof com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig)) {
            return;
        }
        sendPluginConfigData(file);
    }

    public void sendPluginConfigData(final XmlFile file) {
        try {
            LOGGER.log(Level.INFO, "Error processing XML content", file.asString());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing XML content", e);
        }
    }
}
