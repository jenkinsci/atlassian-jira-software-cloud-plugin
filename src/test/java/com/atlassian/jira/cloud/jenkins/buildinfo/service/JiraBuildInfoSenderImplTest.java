package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildKeyResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.RejectedBuildResponse;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.scm.ChangeLogSet;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SECRET_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SITE_CONFIG_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SITE_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.SKIPPED_ISSUE_KEYS_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraBuildInfoSenderImplTest {

    private static final String SITE = "example.atlassian.com";
    private static final String BRANCH_NAME = "TEST-123-branch-name";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final JiraCloudSiteConfig JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig(SITE, "clientId", "credsId");

    @Mock private JiraSiteConfigRetriever siteConfigRetriever;

    @Mock private SecretRetriever secretRetriever;

    @Mock private IssueKeyExtractor issueKeyExtractor;

    @Mock private CloudIdResolver cloudIdResolver;

    @Mock private AccessTokenRetriever accessTokenRetriever;

    @Mock private JiraApi buildsApi;

    @Mock private RunWrapperProvider runWrapperProvider;

    private JiraBuildInfoSender classUnderTest;

    @Before
    public void setUp() {
        classUnderTest =
                new JiraBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        issueKeyExtractor,
                        cloudIdResolver,
                        accessTokenRetriever,
                        buildsApi,
                        runWrapperProvider);

        setupMocks();
    }

    @Test
    public void testSendBuildInfo_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenIssueKeysNotFound() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(Collections.emptySet());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

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
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenAccessTokenFailure() {
        // given
        when(accessTokenRetriever.getAccessToken(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ACCESS_TOKEN);
    }

    @Test
    public void testSendBuildInfo_whenApiResponseFailure() {
        // given
        setupBuildsApiFailure();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILDS_API_RESPONSE);
    }

    @Test
    public void testSendBuildInfo_whenBuildAccepted() {
        // given
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

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
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILD_REJECTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenUserProvidedBranch() {
        // given
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new JiraBuildInfoRequest(SITE, BRANCH_NAME, mockWorkflowRun());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(jiraBuildInfoRequest);

        verify(issueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenBranchNameIsEmpty() {
        // given
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new JiraBuildInfoRequest(SITE, "", mockWorkflowRun());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(jiraBuildInfoRequest);

        verify(issueKeyExtractor).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenUnknownIssueKeys() {
        // given
        setupBuildApiUnknownIssueKeys();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(createRequest());

        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_UNKNOWN_ISSUE_KEYS);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenIssueKeysInUserCommitTitle() {
        // given
        final WorkflowRun workflowRun = changeSetWithOneChangeSetEntry();
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new JiraBuildInfoRequest(SITE, BRANCH_NAME, workflowRun);
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendBuildInfo(jiraBuildInfoRequest);

        verify(issueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    private JiraBuildInfoRequest createRequest() {
        return new JiraBuildInfoRequest(SITE, null, mockWorkflowRun());
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupIssueKeyExtractor();
        setupCloudIdResolver();
        setupAccessTokenRetriever();
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
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(ImmutableSet.of("TEST-123"));
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }

    private void setupAccessTokenRetriever() {
        when(accessTokenRetriever.getAccessToken(any())).thenReturn(Optional.of("access-token"));
    }

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
            when(mockRunWrapper.getCurrentResult()).thenReturn("SUCCESS_BUILD_ACCEPTED");
            when(runWrapperProvider.getWrapper(any())).thenReturn(mockRunWrapper);
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupBuildsApiFailure() {
        when(buildsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>("Error"));
    }

    private void setupBuildsApiBuildAccepted() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, BUILD_NUMBER);
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        ImmutableList.of(buildKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());
        when(buildsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(buildApiResponse));
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
        when(buildsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(buildApiResponse));
    }

    private void setupBuildApiUnknownIssueKeys() {
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ImmutableList.of("TEST-123"));
        when(buildsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(buildApiResponse));
    }

    private static WorkflowRun mockWorkflowRun() {
        return mock(WorkflowRun.class);
    }

    private WorkflowRun changeSetWithOneChangeSetEntry() {
        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getMsg()).thenReturn("TEST-125 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[] {entry});
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return workflowRun;
    }
}
