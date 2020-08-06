package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.AssociationType;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Command;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentKeyResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.RejectedDeploymentResponse;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SECRET_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SITE_CONFIG_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.FAILURE_SITE_NOT_FOUND;
import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraDeploymentInfoSenderImplTest {

    private static final String SITE = "example.atlassian.com";
    public static final String ENVIRONMENT_ID = "prod-east-1";
    public static final String ENVIRONMENT_NAME = "prod-east-1";
    public static final String ENVIRONMENT_TYPE = "production";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final JiraCloudSiteConfig JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig(SITE, "clientId", "credsId");

    @Mock private JiraSiteConfigRetriever siteConfigRetriever;

    @Mock private SecretRetriever secretRetriever;

    @Mock private CloudIdResolver cloudIdResolver;

    @Mock private AccessTokenRetriever accessTokenRetriever;

    @Mock private JiraApi deploymentsApi;

    @Mock private IssueKeyExtractor issueKeyExtractor;

    @Mock private RunWrapperProvider runWrapperProvider;

    private JiraDeploymentInfoSender classUnderTest;

    @Before
    public void setUp() {
        classUnderTest =
                new JiraDeploymentInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        accessTokenRetriever,
                        deploymentsApi,
                        issueKeyExtractor,
                        runWrapperProvider);

        setupMocks();
    }

    @Test
    public void testSendDeploymentInfo_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenIssueKeysNotFound() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(Collections.emptySet());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY);
        final String message = response.getMessage();
        assertThat(message)
                .startsWith(
                        "No issue keys found in the change log and service ids were not provided");
    }

    @Test
    public void testSendDeploymentInfo_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenAccessTokenFailure() {
        // given
        when(accessTokenRetriever.getAccessToken(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ACCESS_TOKEN);
    }

    @Test
    public void testSendDeploymentInfo_whenApiResponseFailure() {
        // given
        setupDeploymentsApiFailure();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_DEPLOYMENTS_API_RESPONSE);
    }

    @Test
    public void testSendDeploymentInfo_whenDeploymentAccepted() {
        // given
        setupDeploymentsApiDeploymentAccepted();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenDeploymentRejected() {
        // given
        setupDeploymentsApiDeploymentRejected();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_DEPLOYMENT_REJECTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenUnknownAssociations() {
        // given
        setupDeploymentApiUnknownIssueKeys();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(createRequest());

        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_UNKNOWN_ASSOCIATIONS);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenMissingEnvironmentIdAndEnvironmentName() {
        // given
        final JiraDeploymentInfoRequest request = createRequest(null, null, null);

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ENVIRONMENT_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "The deployment environment is not valid. "
                                + "The parameter environmentId is required. The parameter environmentName is required.");
    }

    @Test
    public void testSendDeploymentInfo_whenNotAllowedEnvironmentType() {
        // given
        final JiraDeploymentInfoRequest request =
                createRequest("prod-east", "Production East", "foobar");

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ENVIRONMENT_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "The deployment environment is not valid. "
                                + "The parameter environmentType is not valid. "
                                + "Allowed values are: [development, testing, staging, production, unmapped]");
    }

    @Test
    public void testSendDeploymentInfo_whenNotAllowedDeploymentState() {
        // given
        final JiraDeploymentInfoRequest request = createRequest("test");
        final WorkflowRun mockWorkflowRun = request.getDeployment();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        verify(mockWorkflowRun, never()).getResult();
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_STATE_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "The deployment state is not valid. "
                                + "The parameter state is not valid. "
                                + "Allowed values are: [unknown, pending, in_progress, cancelled, failed, rolled_back, successful]");
    }

    @Test
    public void testSendDeploymentInfo_whenNotAllowedDeploymentStateForGate() {
        // given
        final JiraDeploymentInfoRequest request =
                new JiraDeploymentInfoRequest(
                        SITE,
                        ENVIRONMENT_ID,
                        ENVIRONMENT_NAME,
                        ENVIRONMENT_TYPE,
                        "in_progress",
                        Collections.emptySet(),
                        Boolean.TRUE,
                        mockWorkflowRun());

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_STATE_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "Deployment gating available only for 'pending' deployment state. Current state: in_progress");
    }

    @Test
    public void testSendDeploymentInfo_whenGateEnabled() {
        // given
        setupDeploymentsApiDeploymentAccepted();
        final JiraDeploymentInfoRequest request =
                new JiraDeploymentInfoRequest(
                        SITE,
                        ENVIRONMENT_ID,
                        ENVIRONMENT_NAME,
                        ENVIRONMENT_TYPE,
                        "pending",
                        Collections.emptySet(),
                        Boolean.TRUE,
                        mockWorkflowRun());

        final ArgumentCaptor<Deployments> deploymentsArgumentCaptor =
                ArgumentCaptor.forClass(Deployments.class);

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
        verify(deploymentsApi)
                .postUpdate(any(), any(), any(), deploymentsArgumentCaptor.capture(), any());
        final JiraDeploymentInfo jiraDeploymentInfo =
                deploymentsArgumentCaptor.getValue().getDeployments().get(0);
        assertThat(jiraDeploymentInfo.getCommands())
                .contains(new Command("initiate_deployment_gating"));
    }

    @Test
    public void getDeploymentState_whenUsedJenkinsRunState() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(Collections.emptySet());
        JiraDeploymentInfoRequest request = createRequest();
        final WorkflowRun mockWorkflowRun = request.getDeployment();

        // when
        final JiraSendInfoResponse response = classUnderTest.sendDeploymentInfo(request);

        // then
        verify(mockWorkflowRun, times(1)).getResult();
    }

    private JiraDeploymentInfoRequest createRequest() {
        return new JiraDeploymentInfoRequest(
                SITE,
                ENVIRONMENT_ID,
                ENVIRONMENT_NAME,
                ENVIRONMENT_TYPE,
                null,
                Collections.emptySet(),
                Boolean.FALSE,
                mockWorkflowRun());
    }

    private JiraDeploymentInfoRequest createRequest(final String state) {
        return new JiraDeploymentInfoRequest(
                SITE,
                ENVIRONMENT_ID,
                ENVIRONMENT_NAME,
                ENVIRONMENT_TYPE,
                state,
                Collections.emptySet(),
                Boolean.FALSE,
                mockWorkflowRun());
    }

    private JiraDeploymentInfoRequest createRequest(
            final String environmentId,
            final String environmentName,
            final String environmentType) {
        return new JiraDeploymentInfoRequest(
                SITE,
                environmentId,
                environmentName,
                environmentType,
                null,
                Collections.emptySet(),
                Boolean.FALSE,
                mockWorkflowRun());
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupCloudIdResolver();
        setupChangeLogExtractor();
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

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }

    private void setupAccessTokenRetriever() {
        when(accessTokenRetriever.getAccessToken(any())).thenReturn(Optional.of("access-token"));
    }

    private void setupChangeLogExtractor() {
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(ImmutableSet.of("TEST-123"));
    }

    private void setupRunWrapperProvider() {
        try {
            final RunWrapper mockRunWrapper = mock(RunWrapper.class);
            when(mockRunWrapper.getFullProjectName())
                    .thenReturn("multibranch-1/TEST-123-branch-name");
            when(mockRunWrapper.getDisplayName()).thenReturn("#1");
            when(mockRunWrapper.getAbsoluteUrl())
                    .thenReturn(
                            "http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");
            when(runWrapperProvider.getWrapper(any())).thenReturn(mockRunWrapper);
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupDeploymentsApiFailure() {
        when(deploymentsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>("Error"));
    }

    private void setupDeploymentsApiDeploymentAccepted() {
        final DeploymentKeyResponse deploymentKeyResponse =
                new DeploymentKeyResponse(PIPELINE_ID, ENVIRONMENT_ID, BUILD_NUMBER);
        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        ImmutableList.of(deploymentKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());
        when(deploymentsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(deploymentApiResponse));
    }

    private void setupDeploymentsApiDeploymentRejected() {
        final DeploymentKeyResponse deploymentKeyResponse =
                new DeploymentKeyResponse(PIPELINE_ID, ENVIRONMENT_ID, BUILD_NUMBER);
        final ApiErrorResponse errorResponse = new ApiErrorResponse("Error message");
        final RejectedDeploymentResponse deploymentResponse =
                new RejectedDeploymentResponse(
                        deploymentKeyResponse, ImmutableList.of(errorResponse));

        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        Collections.emptyList(),
                        ImmutableList.of(deploymentResponse),
                        Collections.emptyList());
        when(deploymentsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(deploymentApiResponse));
    }

    private void setupDeploymentApiUnknownIssueKeys() {
        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.singletonList(
                                Association.builder()
                                        .withAssociationType(AssociationType.ISSUE_KEYS)
                                        .withValues(ImmutableSet.of("TEST-123"))
                                        .build()));
        when(deploymentsApi.postUpdate(any(), any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(deploymentApiResponse));
    }

    private static WorkflowRun mockWorkflowRun() {
        return mock(WorkflowRun.class);
    }
}
