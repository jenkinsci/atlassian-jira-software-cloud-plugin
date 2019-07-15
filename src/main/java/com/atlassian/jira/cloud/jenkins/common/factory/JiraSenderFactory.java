package com.atlassian.jira.cloud.jenkins.common.factory;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetrieverImpl;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogExtractorImpl;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSender;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.provider.HttpClientProvider;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProviderImpl;
import com.atlassian.jira.cloud.jenkins.util.ScmRevisionExtractorImpl;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.OkHttpClient;

import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;

public final class JiraSenderFactory {

    private static JiraSenderFactory INSTANCE;

    private JiraBuildInfoSender jiraBuildInfoSender;
    private JiraDeploymentInfoSender jiraDeploymentInfoSender;

    private JiraSenderFactory() {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final HttpClientProvider httpClientProvider = new HttpClientProvider();
        final OkHttpClient httpClient = httpClientProvider.httpClient();
        final ObjectMapper objectMapper = objectMapperProvider.objectMapper();

        final JiraSiteConfigRetriever siteConfigRetriever = new JiraSiteConfigRetrieverImpl();
        final ScmRevisionExtractorImpl scmRevisionExtractor = new ScmRevisionExtractorImpl();
        final ChangeLogExtractor changeLogExtractor = new ChangeLogExtractorImpl();
        final SecretRetriever secretRetriever = new SecretRetriever();
        final CloudIdResolver cloudIdResolver = new CloudIdResolver(httpClient, objectMapper);
        final AccessTokenRetriever accessTokenRetriever =
                new AccessTokenRetriever(httpClient, objectMapper);
        final JiraApi buildsApi =
                new JiraApi(
                        httpClient,
                        objectMapper,
                        ATLASSIAN_API_URL + "/jira/builds/0.1/cloud/%s/bulk");
        final JiraApi deploymentsApi =
                new JiraApi(
                        httpClient,
                        objectMapper,
                        ATLASSIAN_API_URL + "/jira/deployments/0.1/cloud/%s/bulk");

        this.jiraBuildInfoSender =
                new JiraBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        scmRevisionExtractor,
                        cloudIdResolver,
                        accessTokenRetriever,
                        buildsApi,
                        new RunWrapperProviderImpl());

        this.jiraDeploymentInfoSender =
                new JiraDeploymentInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        accessTokenRetriever,
                        deploymentsApi,
                        changeLogExtractor,
                        new RunWrapperProviderImpl());
    }

    public static synchronized JiraSenderFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JiraSenderFactory();
        }

        return INSTANCE;
    }

    @VisibleForTesting
    public static void setInstance(final JiraSenderFactory instance) {
        INSTANCE = instance;
    }

    public JiraBuildInfoSender getJiraBuildInfoSender() {
        return jiraBuildInfoSender;
    }

    public JiraDeploymentInfoSender getJiraDeploymentInfoSender() {
        return jiraDeploymentInfoSender;
    }
}
