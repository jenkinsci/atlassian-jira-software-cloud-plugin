package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraGatingStatusRetrieverImplTest {

    public static final String ENVIRONMENT_ID = "prod-east-1";
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final String SITE = "example.atlassian.com";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    private static final JiraCloudSiteConfig JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig(SITE, "clientId", "credsId");

    @Mock private JiraSiteConfigRetriever siteConfigRetriever;
    @Mock private SecretRetriever secretRetriever;
    @Mock private CloudIdResolver cloudIdResolver;
    @Mock private AccessTokenRetriever accessTokenRetriever;
    @Mock private JiraApi jiraApi;
    @Mock private WorkflowRun run;

    private JiraGatingStatusRetrieverImpl classUnderTest;

    @Before
    public void before() throws Exception {
        classUnderTest =
                new JiraGatingStatusRetrieverImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        accessTokenRetriever,
                        jiraApi);

        setupMocks();
    }

    @Test
    public void testGetGateState_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenAccessTokenFailure() {
        // given
        when(accessTokenRetriever.getAccessToken(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ACCESS_TOKEN);
    }

    @Test
    public void testGetGateState_whenApiResponseFailure() {
        // given
        setupApiFailure();

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(JiraSendInfoResponse.Status.FAILURE_GATE_CHECK);
    }

    @Test
    public void testGetGateState_whenApiResponseSuccess() {
        // given
        setupApiSuccess();

        // when
        final JiraGatingStatusResponse response = classUnderTest.getGatingState(createRequest());

        // then
        assertThat(response.getStatus()).isEqualTo(JiraSendInfoResponse.Status.SUCCESS_GATE_CHECK);
    }

    private GatingStatusRequest createRequest() {
        return new GatingStatusRequest(SITE, ENVIRONMENT_ID, run);
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupCloudIdResolver();
        setupAccessTokenRetriever();
        setupRun();
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

    private void setupRun() {
        final WorkflowJob workflowJob = mock(WorkflowJob.class);
        when(run.getNumber()).thenReturn(BUILD_NUMBER);
        when(workflowJob.getFullName()).thenReturn(PIPELINE_ID);
        when(run.getParent()).thenReturn(workflowJob);
    }

    private void setupApiFailure() {
        when(jiraApi.getResult(any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>("Error"));
    }

    private void setupApiSuccess() {
        final GatingStatusResponse gatingStatusResponse =
                new GatingStatusResponse(GatingStatus.AWAITING, Collections.emptyList());
        when(jiraApi.getResult(any(), any(), any(), any()))
                .thenReturn(new PostUpdateResult<>(gatingStatusResponse));
    }
}
