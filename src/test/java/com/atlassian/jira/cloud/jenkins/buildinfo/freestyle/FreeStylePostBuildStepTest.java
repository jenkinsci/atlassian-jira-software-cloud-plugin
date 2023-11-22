package com.atlassian.jira.cloud.jenkins.buildinfo.freestyle;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.FreestyleJiraBuildInfoSenderImpl;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.collect.ImmutableList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FreeStylePostBuildStepTest {

    private static final String SITE = "example.atlassian.net";
    private static final String BRANCH_NAME = "TEST-123-test-feature";
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CREDENTIAL_ID = UUID.randomUUID().toString();

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule jenkinsRule = new JenkinsRule();

    @Inject FreeStylePostBuildStep.DescriptorImpl descriptor;

    @Before
    public void setUp() throws Exception {
        jenkinsRule.getInstance().getInjector().injectMembers(this);

        // setup Jira site config
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIAL_ID, "false")));

        // setup credentials
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), secretCredential());

        // setup JiraBuildInfoSender mock
        final JiraSenderFactory mockSenderFactory = mock(JiraSenderFactory.class);
        final JiraBuildInfoSender mockSender = mock(FreestyleJiraBuildInfoSenderImpl.class);
        when(mockSenderFactory.getFreestyleBuildInfoSender()).thenReturn(mockSender);
        JiraSenderFactory.setInstance(mockSenderFactory);
        final BuildApiResponse response =
                new BuildApiResponse(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        final JiraBuildInfoResponse buildAccepted =
                JiraBuildInfoResponse.successBuildAccepted(SITE, response);
        when(mockSender.sendBuildInfo(any(), any()))
                .thenReturn(Collections.singletonList(buildAccepted));
    }

    @Test
    public void configRoundTrip() throws Exception {
        final FreeStylePostBuildStep step = new FreeStylePostBuildStep();
        step.setSite(SITE);
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublisher(descriptor);
        project = jenkinsRule.configRoundtrip(project);
        // jenkinsRule.assertEqualDataBoundBeans(step.getSite(),project.getDescriptor().get);
        assertThat(step.getSite()).isEqualTo(SITE);
    }

    @Test
    public void configRoundTripWithBranch() throws Exception {
        final FreeStylePostBuildStep step = new FreeStylePostBuildStep();
        step.setBranch(BRANCH_NAME);
        FreeStyleProject project = jenkinsRule.createFreeStyleProject();
        project.getPublisher(descriptor);
        project = jenkinsRule.configRoundtrip(project);

        assertThat(step.getBranch()).isEqualTo(BRANCH_NAME);
    }

    private static BaseStandardCredentials secretCredential() {
        return new StringCredentialsImpl(
                CredentialsScope.GLOBAL, CREDENTIAL_ID, "test-secret", Secret.fromString("secret"));
    }

    @Test
    public void testStep() throws Exception {
        // given
        final AbstractBuild<?, ?> mockAbstractBuild = mockAbstractBuild();
        final TaskListener mockTaskListener = mockBuildListener();
        when(mockTaskListener.getLogger()).thenReturn(mock(PrintStream.class));
        when(mockAbstractBuild.getWorkspace()).thenReturn(mock(FilePath.class));

        final FreeStylePostBuildStep step = new FreeStylePostBuildStep();

        // when
        final boolean response =
                step.perform(mockAbstractBuild, mockLauncher(), mockBuildListener());

        // then
        assertThat(response).isEqualTo(true);
    }

    private static AbstractBuild<?, ?> mockAbstractBuild() {
        return mock(AbstractBuild.class);
    }

    private static BuildListener mockBuildListener() {
        return mock(BuildListener.class);
    }

    private static Launcher mockLauncher() {
        return mock(Launcher.class);
    }
}
