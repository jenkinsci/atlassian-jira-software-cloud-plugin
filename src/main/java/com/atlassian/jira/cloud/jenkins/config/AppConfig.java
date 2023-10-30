package com.atlassian.jira.cloud.jenkins.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE =
            "com/atlassian/jira/cloud/jenkins/config/JiraCloudPluginConfig/config.properties";

    public static String getNodeEnv() {
        Properties properties = new Properties();
        try (InputStream config = AppConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (config != null) {
                properties.load(config);
                return properties.getProperty("NODE_ENV");
            } else {
                // Handle if the resource is not found
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
