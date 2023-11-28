package com.atlassian.jira.cloud.jenkins.checkgatingstatus.pipeline;

import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusRetriever;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.collect.ImmutableList;
import hudson.model.Executor;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.Secret;
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class JiraCheckGatingStatusStepTest {
    private static final String SITE = "example.atlassian.net";
    private static final String ENVIRONMENT_ID = UUID.randomUUID().toString();
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CREDENTIAL_ID = UUID.randomUUID().toString();
    private static final String PIPELINE_ID = UUID.randomUUID().toString();
    private static final Integer DEPLOYMENT_NUMBER = 123;

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject
    JiraCheckGatingStatusStep.DescriptorImpl descriptor;

    private JiraGatingStatusRetriever mockRetriever;

    private static BaseStandardCredentials secretCredential() {
        return new StringCredentialsImpl(
                CredentialsScope.GLOBAL, CREDENTIAL_ID, "test-secret", Secret.fromString("secret"));
    }

    @Before
    public void setUp() throws Exception {
        jenkinsRule.getInstance().getInjector().injectMembers(this);
        mockRetriever = mock(JiraGatingStatusRetriever.class);

        // setup Jira site config
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIAL_ID)));

        // setup credentials
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), secretCredential());

        // setup JiraGateStateRetriever mock
        final JiraSenderFactory mockSenderFactory = mock(JiraSenderFactory.class);
        when(mockSenderFactory.getJiraGateStateRetriever()).thenReturn(mockRetriever);
        JiraSenderFactory.setInstance(mockSenderFactory);
    }

    @Test
    public void configRoundTrip() throws Exception {
        final JiraCheckGatingStatusStep jiraCheckGatingStatusStep =
                new JiraCheckGatingStatusStep(ENVIRONMENT_ID);
        jiraCheckGatingStatusStep.setSite(SITE);

        final JiraCheckGatingStatusStep step =
                new StepConfigTester(jenkinsRule).configRoundTrip(jiraCheckGatingStatusStep);

        assertThat(step.getSite()).isEqualTo(SITE);
        assertThat(step.getEnvironmentId()).isEqualTo(ENVIRONMENT_ID);
    }

    @Test
    public void testStepAwaiting() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mock(WorkflowRun.class);
        final TaskListener mockTaskListener = mock(TaskListener.class);
        final Executor executor = mock(Executor.class);

        final Map<String, Object> r = new HashMap<>();
        r.put("site", SITE);
        r.put("environmentId", ENVIRONMENT_ID);
        final JiraCheckGatingStatusStep step =
                (JiraCheckGatingStatusStep) descriptor.newInstance(r);

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        final JiraCheckGatingStatusStep.CheckGatingStatusExecution start =
                (JiraCheckGatingStatusStep.CheckGatingStatusExecution) step.start(ctx);

        final GatingStatusResponse apiResponse =
                new GatingStatusResponse(
                        LocalDateTime.now().toString(),
                        GatingStatus.AWAITING,
                        Collections.emptyList(),
                        PIPELINE_ID,
                        ENVIRONMENT_ID,
                        DEPLOYMENT_NUMBER);
        final JiraGatingStatusResponse gateStatusResponse =
                JiraGatingStatusResponse.success(SITE, apiResponse);
        when(mockRetriever.getGatingStatus(any(), any(), any(), any()))
                .thenReturn(gateStatusResponse);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));
        when(mockWorkflowRun.getExecutor()).thenReturn(executor);

        // when
        final Boolean response = start.run();

        // then
        assertFalse(response);
    }

    @Test
    public void testStepAllowed() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mock(WorkflowRun.class);
        final TaskListener mockTaskListener = mock(TaskListener.class);
        final Executor executor = mock(Executor.class);

        final Map<String, Object> r = new HashMap<>();
        r.put("site", SITE);
        r.put("environmentId", ENVIRONMENT_ID);
        final JiraCheckGatingStatusStep step =
                (JiraCheckGatingStatusStep) descriptor.newInstance(r);

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));
        when(mockWorkflowRun.getExecutor()).thenReturn(executor);

        final JiraCheckGatingStatusStep.CheckGatingStatusExecution start =
                (JiraCheckGatingStatusStep.CheckGatingStatusExecution) step.start(ctx);

        final GatingStatusResponse apiResponse =
                new GatingStatusResponse(
                        LocalDateTime.now().toString(),
                        GatingStatus.ALLOWED,
                        Collections.emptyList(),
                        PIPELINE_ID,
                        ENVIRONMENT_ID,
                        DEPLOYMENT_NUMBER);
        final JiraGatingStatusResponse gateStatusResponse =
                JiraGatingStatusResponse.success(SITE, apiResponse);
        when(mockRetriever.getGatingStatus(any(), any(), any(), any()))
                .thenReturn(gateStatusResponse);

        // when
        final Boolean response = start.run();

        // then
        assertTrue(response);
    }

    @Test
    public void testStepPrevented() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mock(WorkflowRun.class);
        final TaskListener mockTaskListener = mock(TaskListener.class);
        final Executor executor = mock(Executor.class);
        final ArgumentCaptor<CauseOfInterruption> causeCaptor =
                ArgumentCaptor.forClass(CauseOfInterruption.class);
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        final Map<String, Object> r = new HashMap<>();
        r.put("site", SITE);
        r.put("environmentId", ENVIRONMENT_ID);
        final JiraCheckGatingStatusStep step =
                (JiraCheckGatingStatusStep) descriptor.newInstance(r);

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);
        when(mockWorkflowRun.getExecutor()).thenReturn(executor);

        final JiraCheckGatingStatusStep.CheckGatingStatusExecution start =
                (JiraCheckGatingStatusStep.CheckGatingStatusExecution) step.start(ctx);

        final GatingStatusResponse apiResponse =
                new GatingStatusResponse(
                        LocalDateTime.now().toString(),
                        GatingStatus.PREVENTED,
                        Collections.emptyList(),
                        PIPELINE_ID,
                        ENVIRONMENT_ID,
                        DEPLOYMENT_NUMBER);
        final JiraGatingStatusResponse gateStatusResponse =
                JiraGatingStatusResponse.success(SITE, apiResponse);
        when(mockRetriever.getGatingStatus(any(), any(), any(), any()))
                .thenReturn(gateStatusResponse);

        // when
        final Boolean response = start.run();

        // then
        assertFalse(response);
        verify(mockWorkflowRun).getExecutor();
        verify(executor).interrupt(eq(Result.ABORTED), causeCaptor.capture());
        final List<CauseOfInterruption> allValues = causeCaptor.getAllValues();
        assertThat(allValues).hasSize(1);
        final CauseOfInterruption causeOfInterruption = allValues.get(0);
        assertThat(causeOfInterruption.getShortDescription())
                .isEqualTo("The deployment was prevented by Jira Service Desk.");
    }
}
