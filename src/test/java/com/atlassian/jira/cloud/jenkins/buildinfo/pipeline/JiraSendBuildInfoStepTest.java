package com.atlassian.jira.cloud.jenkins.buildinfo.pipeline;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.common.client.DefaultSiteLookupFailureException;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.collect.ImmutableList;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraSendBuildInfoStepTest {

    private static final String SITE = "example.atlassian.net";
    private static final String ISSUE_KEY = "TEST-123";
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CREDENTIAL_ID = UUID.randomUUID().toString();

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule jenkinsRule = new JenkinsRule();
    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Inject JiraSendBuildInfoStep.DescriptorImpl descriptor;

    @Before
    public void setUp() throws Exception {
        jenkinsRule.getInstance().getInjector().injectMembers(this);

        // setup Jira site config
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIAL_ID)));

        // setup credentials
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), secretCredential());

        // setup JiraBuildInfoSender mock
        final JiraSenderFactory mockSenderFactory = mock(JiraSenderFactory.class);
        final JiraBuildInfoSender mockSender = mock(JiraBuildInfoSender.class);
        when(mockSenderFactory.getJiraBuildInfoSender()).thenReturn(mockSender);
        JiraSenderFactory.setInstance(mockSenderFactory);
        final BuildApiResponse response =
                new BuildApiResponse(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        final JiraBuildInfoResponse buildAccepted =
                JiraBuildInfoResponse.successBuildAccepted(SITE, response);
        when(mockSender.sendBuildInfo(any())).thenReturn(buildAccepted);
    }

    @Test
    public void configRoundTrip() throws Exception {
        final JiraSendBuildInfoStep build = new JiraSendBuildInfoStep();
        build.setSite(SITE);
        final JiraSendBuildInfoStep step = new StepConfigTester(jenkinsRule).configRoundTrip(build);

        assertThat(step.getSite()).isEqualTo(SITE);
    }

    @Test
    public void configRoundTripPicksUpDefaultSite() throws Exception {
        final JiraSendBuildInfoStep step = new StepConfigTester(jenkinsRule).configRoundTrip(new JiraSendBuildInfoStep());

        assertThat(step.getSite()).isEqualTo(SITE);
    }

    @Test
    public void testStep() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mockWorkflowRun();
        final TaskListener mockTaskListener = mockTaskListener();
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        final Map<String, Object> r = new HashMap<>();
        r.put("site", SITE);
        final JiraSendBuildInfoStep step = (JiraSendBuildInfoStep) descriptor.newInstance(r);

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        final JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution start =
                (JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution) step.start(ctx);

        // when
        final JiraSendInfoResponse response = start.run();

        // then
        assertThat(response.getStatus()).isEqualTo(SUCCESS_BUILD_ACCEPTED);
    }

    @Test
    public void testStepPicksupDefaultSite() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mockWorkflowRun();
        final TaskListener mockTaskListener = mockTaskListener();
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        final JiraSendBuildInfoStep step = (JiraSendBuildInfoStep) descriptor.newInstance(new HashMap<>());

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        final JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution start =
                (JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution) step.start(ctx);

        // when
        final JiraSendInfoResponse response = start.run();

        // then
        assertThat(response.getStatus()).isEqualTo(SUCCESS_BUILD_ACCEPTED);
    }

    @Test
    public void testStepWithNoSitesConfigured() throws Exception {
        // given
        final WorkflowRun mockWorkflowRun = mockWorkflowRun();
        final TaskListener mockTaskListener = mockTaskListener();
        JiraCloudPluginConfig.get().setSites(Collections.emptyList()); // clear site configs
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));

        final JiraSendBuildInfoStep step = (JiraSendBuildInfoStep) descriptor.newInstance(new HashMap<>());

        final StepContext ctx = mock(StepContext.class);
        when(ctx.get(Node.class)).thenReturn(jenkinsRule.getInstance());
        when(ctx.get(WorkflowRun.class)).thenReturn(mockWorkflowRun);
        when(ctx.get(TaskListener.class)).thenReturn(mockTaskListener);

        final JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution stepExecution =
                (JiraSendBuildInfoStep.JiraSendBuildInfoStepExecution) step.start(ctx);

        // expect exception
        expectedException.expect(DefaultSiteLookupFailureException.class);
        expectedException.expectMessage(
                "Unable to determine default site. Please specify site parameter in the Jenkinsfile.");

        // when
        stepExecution.run();

        // verify
        assertThat(stepExecution.getStatus()).isEqualTo(Result.FAILURE);
    }

    private static BaseStandardCredentials secretCredential() {
        return new StringCredentialsImpl(
                CredentialsScope.GLOBAL, CREDENTIAL_ID, "test-secret", Secret.fromString("secret"));
    }

    private static WorkflowRun mockWorkflowRun() {
        return mock(WorkflowRun.class);
    }

    private static TaskListener mockTaskListener() {
        return mock(TaskListener.class);
    }
}
