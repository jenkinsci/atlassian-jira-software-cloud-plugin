package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetrieverImpl;
import com.atlassian.jira.cloud.jenkins.util.ScmRevisionExtractorImpl;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProviderImpl;
import com.atlassian.jira.cloud.jenkins.provider.HttpClientProvider;
import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import okhttp3.OkHttpClient;

public final class JiraBuildInfoSenderFactory {

    private static JiraBuildInfoSenderFactory INSTANCE;

    private JiraBuildInfoSender jiraBuildInfoSender;

    private JiraBuildInfoSenderFactory() {
        final ObjectMapperProvider objectMapperProvider = new ObjectMapperProvider();
        final HttpClientProvider httpClientProvider = new HttpClientProvider();
        final OkHttpClient httpClient = httpClientProvider.httpClient();
        final ObjectMapper objectMapper = objectMapperProvider.objectMapper();

        final JiraSiteConfigRetriever siteConfigRetriever = new JiraSiteConfigRetrieverImpl();
        final ScmRevisionExtractorImpl scmRevisionExtractor = new ScmRevisionExtractorImpl();
        final SecretRetriever secretRetriever = new SecretRetriever();
        final CloudIdResolver cloudIdResolver = new CloudIdResolver(httpClient, objectMapper);
        final AccessTokenRetriever accessTokenRetriever =
                new AccessTokenRetriever(httpClient, objectMapper);
        final BuildsApi buildsApi = new BuildsApi(httpClient, objectMapper);

        this.jiraBuildInfoSender =
                new JiraBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        scmRevisionExtractor,
                        cloudIdResolver,
                        accessTokenRetriever,
                        buildsApi,
                        new RunWrapperProviderImpl());
    }

    public static synchronized JiraBuildInfoSenderFactory getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JiraBuildInfoSenderFactory();
        }

        return INSTANCE;
    }

    @VisibleForTesting
    public static void setInstance(final JiraBuildInfoSenderFactory instance) {
        INSTANCE = instance;
    }

    public JiraBuildInfoSender getJiraBuildInfoSender() {
        return jiraBuildInfoSender;
    }
}
