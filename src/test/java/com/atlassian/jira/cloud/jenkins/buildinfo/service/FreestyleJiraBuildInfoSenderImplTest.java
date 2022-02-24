package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildKeyResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.RejectedBuildResponse;
import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2Retriever;
import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.FreestyleChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.FreestyleBranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FreestyleJiraBuildInfoSenderImplTest {
    private static final String SITE = "example.atlassian.com";
    private static final String BRANCH_NAME = "TEST-123-branch-name";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final JiraCloudSiteConfig2 JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig2(
                    SITE, "https://webhook.url?jenkins_server_uuid=foo", "credsId");

    @Mock private JiraSiteConfig2Retriever siteConfigRetriever;

    @Mock private SecretRetriever secretRetriever;

    @Mock private FreestyleIssueKeyExtractor freestyleIssueKeyExtractor;

    @Mock private FreestyleChangeLogIssueKeyExtractor freestyleChangeLogIssueKeyExtractor;

    @Mock private CloudIdResolver cloudIdResolver;

    @Mock private BuildsApi buildsApi;

    @Mock private RunWrapperProvider runWrapperProvider;

    private JiraBuildInfoSender classUnderTest;

    @Mock private FreestyleBranchNameIssueKeyExtractor freestyleBranchNameIssueKeyExtractor;

    @Before
    public void setUp() {
        classUnderTest =
                new FreestyleJiraBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        freestyleIssueKeyExtractor,
                        cloudIdResolver,
                        buildsApi,
                        runWrapperProvider,
                        freestyleChangeLogIssueKeyExtractor);

        setupMocks();
    }

    @Test
    public void testSendBuildInfo_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenIssueKeysNotFound() {
        // given
        when(freestyleIssueKeyExtractor.extractIssueKeys(any())).thenReturn(Collections.emptySet());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(SKIPPED_ISSUE_KEYS_NOT_FOUND);
        final String message = response.getMessage();
        assertThat(message).startsWith("No issue keys found in the current branch name");
    }

    @Test
    public void testSendBuildInfo_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenApiResponseFailure() {
        // given
        setupBuildsApiFailure();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILDS_API_RESPONSE);
    }

    @Test
    public void testSendBuildInfo_whenBuildAccepted() {
        // given
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenBuildRejected() {
        // given
        setupBuildApiBuildRejected();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILD_REJECTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendFreestyleBuildInfo_whenUserProvidedBranch() {
        // given
        final FreestyleBuildInfoRequest jiraBuildInfoRequest =
                new FreestyleBuildInfoRequest(SITE, BRANCH_NAME, mockAbstractBuild());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        verify(freestyleIssueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfoFreestyle_whenBranchNameIsEmpty() {
        // given
        final FreestyleBuildInfoRequest jiraBuildInfoRequest =
                new FreestyleBuildInfoRequest(SITE, "", mockAbstractBuild());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        // verify(freestyleIssueKeyExtractor).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testFreestyleSendBuildInfo_whenIssueKeysInUserCommitTitle() {
        // given
        final AbstractBuild build = changeSetFreestyle();
        final FreestyleBuildInfoRequest jiraBuildInfoRequest =
                new FreestyleBuildInfoRequest(SITE, BRANCH_NAME, build);
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        verify(freestyleIssueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testFreestyleSendBuildInfo_whenUnknownIssueKeys() {
        // given
        setupBuildApiUnknownIssueKeys();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createFreestyleRequest()).get(0);

        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_UNKNOWN_ISSUE_KEYS);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    private void setupBuildApiUnknownIssueKeys() {
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ImmutableList.of("TEST-123"));
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupIssueKeyExtractor();
        setupCloudIdResolver();
        setupRunWrapperProvider();
    }

    private void setupSiteConfigRetriever() {
        when(siteConfigRetriever.getJiraSiteConfig(any()))
                .thenReturn(Optional.of(JIRA_SITE_CONFIG));
    }

    private void setupSecretRetriever() {
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));
    }

    private void setupIssueKeyExtractor() {
        when(freestyleIssueKeyExtractor.extractIssueKeys(any()))
                .thenReturn(ImmutableSet.of("TEST-123"));
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupRunWrapperProvider() {
        try {
            final RunWrapper mockRunWrapper = mock(RunWrapper.class);
            when(mockRunWrapper.getFullProjectName())
                    .thenReturn("multibranch-1/TEST-123-branch-name");
            when(mockRunWrapper.getNumber()).thenReturn(1);
            when(mockRunWrapper.getDisplayName()).thenReturn("#1");
            when(mockRunWrapper.getAbsoluteUrl())
                    .thenReturn(
                            "http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");

            final Run run = mock(Run.class);
            when(mockRunWrapper.getRawBuild()).thenReturn(run);

            when(runWrapperProvider.getWrapper(any())).thenReturn(mockRunWrapper);
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupBuildsApiFailure() {
        when(buildsApi.sendBuild(any(), any())).thenThrow(new ApiUpdateFailedException("Error"));
    }

    private FreestyleBuildInfoRequest createRequest() {
        return new FreestyleBuildInfoRequest(SITE, null, mockAbstractBuild());
    }

    private static AbstractBuild<?, ?> mockAbstractBuild() {
        return mock(AbstractBuild.class);
    }

    private void setupBuildsApiBuildAccepted() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, BUILD_NUMBER);
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        ImmutableList.of(buildKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private void setupBuildApiBuildRejected() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, BUILD_NUMBER);
        final ApiErrorResponse errorResponse = new ApiErrorResponse("Error message");
        final RejectedBuildResponse buildResponse =
                new RejectedBuildResponse(buildKeyResponse, ImmutableList.of(errorResponse));

        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        Collections.emptyList(),
                        ImmutableList.of(buildResponse),
                        Collections.emptyList());
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private AbstractBuild changeSetFreestyle() {
        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        // when(entry.getMsg()).thenReturn("TEST-125 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        // when(changeLogSet.getItems()).thenReturn(new Object[] {entry});
        final AbstractBuild<?, ?> build = mock(AbstractBuild.class);

        // when(build.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return build;
    }

    private FreestyleBuildInfoRequest createFreestyleRequest() {
        return new FreestyleBuildInfoRequest(SITE, "TEST-123-branch-name", mockAbstractBuild());
    }
}
