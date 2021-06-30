package com.atlassian.jira.cloud.jenkins.common.factory;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.FreestyleJiraBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusRetriever;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusRetrieverImpl;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetrieverImpl;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.FreestyleChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSender;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.provider.HttpClientProvider;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProviderImpl;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.FreestyleBranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.OkHttpClient;

import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;

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

        final JiraSiteConfigRetriever siteConfigRetriever = new JiraSiteConfigRetrieverImpl();
        final BranchNameIssueKeyExtractor branchNameIssueKeyExtractor =
                new BranchNameIssueKeyExtractor();
        final FreestyleIssueKeyExtractor freestyleBranchNameIssueKeyExtractor =
                new FreestyleBranchNameIssueKeyExtractor();
        final FreestyleIssueKeyExtractor freestyleChangeLogIssueKeyExtractor =
                new FreestyleChangeLogIssueKeyExtractor();
        final IssueKeyExtractor changeLogIssueKeyExtractor = new ChangeLogIssueKeyExtractor();
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

        final JiraApi gateApi =
                new JiraApi(
                        httpClient,
                        objectMapper,
                        ATLASSIAN_API_URL
                                + "/jira/deployments/0.1"
                                + "/cloud/${cloudId}"
                                + "/pipelines/${pipelineId}"
                                + "/environments/${environmentId}"
                                + "/deployments/${deploymentId}"
                                + "/gating-status");

        this.jiraBuildInfoSender =
                new MultibranchBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        branchNameIssueKeyExtractor,
                        cloudIdResolver,
                        accessTokenRetriever,
                        buildsApi,
                        new RunWrapperProviderImpl());
        this.freestyleBuildInfoSender =
                new FreestyleJiraBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        freestyleBranchNameIssueKeyExtractor,
                        cloudIdResolver,
                        accessTokenRetriever,
                        buildsApi,
                        new RunWrapperProviderImpl(),
                        freestyleChangeLogIssueKeyExtractor);

        this.jiraDeploymentInfoSender =
                new JiraDeploymentInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        accessTokenRetriever,
                        deploymentsApi,
                        changeLogIssueKeyExtractor,
                        new RunWrapperProviderImpl());

        this.jiraGatingStatusRetriever =
                new JiraGatingStatusRetrieverImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        accessTokenRetriever,
                        gateApi);
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
