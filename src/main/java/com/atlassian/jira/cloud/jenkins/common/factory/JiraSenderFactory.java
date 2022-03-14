package com.atlassian.jira.cloud.jenkins.common.factory;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.FreestyleJiraBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.GatingStatusApi;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusRetriever;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusRetrieverImpl;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2Retriever;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2RetrieverImpl;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentsApi;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.FreestyleChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSender;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.provider.HttpClientProvider;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.FreestyleBranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProviderImpl;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.OkHttpClient;

public final class JiraSenderFactory {

    private static JiraSenderFactory INSTANCE;

    private JiraBuildInfoSender jiraBuildInfoSender;
    private JiraDeploymentInfoSender jiraDeploymentInfoSender;
    private JiraGatingStatusRetriever jiraGatingStatusRetriever;
    private JiraBuildInfoSender freestyleBuildInfoSender;

    private JiraSenderFactory() {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final HttpClientProvider httpClientProvider = new HttpClientProvider();
        final OkHttpClient httpClient = httpClientProvider.httpClient();
        final ObjectMapper objectMapper = objectMapperProvider.objectMapper();

        final JiraSiteConfig2Retriever siteConfig2Retriever = new JiraSiteConfig2RetrieverImpl();
        final BranchNameIssueKeyExtractor branchNameIssueKeyExtractor =
                new BranchNameIssueKeyExtractor();
        final FreestyleIssueKeyExtractor freestyleBranchNameIssueKeyExtractor =
                new FreestyleBranchNameIssueKeyExtractor();
        final FreestyleIssueKeyExtractor freestyleChangeLogIssueKeyExtractor =
                new FreestyleChangeLogIssueKeyExtractor();
        final IssueKeyExtractor changeLogIssueKeyExtractor = new ChangeLogIssueKeyExtractor();
        final SecretRetriever secretRetriever = new SecretRetriever();
        final CloudIdResolver cloudIdResolver = new CloudIdResolver(httpClient, objectMapper);
        final BuildsApi buildsApi = new BuildsApi(httpClient, objectMapper);
        final DeploymentsApi deploymentsApi = new DeploymentsApi(httpClient, objectMapper);
        final GatingStatusApi gatingStatusApi = new GatingStatusApi(httpClient, objectMapper);

        this.jiraBuildInfoSender =
                new MultibranchBuildInfoSenderImpl(
                        siteConfig2Retriever,
                        secretRetriever,
                        branchNameIssueKeyExtractor,
                        cloudIdResolver,
                        buildsApi,
                        new RunWrapperProviderImpl());

        this.freestyleBuildInfoSender =
                new FreestyleJiraBuildInfoSenderImpl(
                        siteConfig2Retriever,
                        secretRetriever,
                        freestyleBranchNameIssueKeyExtractor,
                        cloudIdResolver,
                        buildsApi,
                        new RunWrapperProviderImpl(),
                        freestyleChangeLogIssueKeyExtractor);

        this.jiraDeploymentInfoSender =
                new JiraDeploymentInfoSenderImpl(
                        siteConfig2Retriever,
                        secretRetriever,
                        cloudIdResolver,
                        deploymentsApi,
                        changeLogIssueKeyExtractor,
                        new RunWrapperProviderImpl());

        this.jiraGatingStatusRetriever =
                new JiraGatingStatusRetrieverImpl(
                        siteConfig2Retriever, secretRetriever, cloudIdResolver, gatingStatusApi);
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

    public JiraBuildInfoSender getFreestyleBuildInfoSender() {
        return freestyleBuildInfoSender;
    }

    public JiraDeploymentInfoSender getJiraDeploymentInfoSender() {
        return jiraDeploymentInfoSender;
    }

    public JiraGatingStatusRetriever getJiraGateStateRetriever() {
        return jiraGatingStatusRetriever;
    }
}
