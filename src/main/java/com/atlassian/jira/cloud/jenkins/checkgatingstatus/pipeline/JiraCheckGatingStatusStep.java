package com.atlassian.jira.cloud.jenkins.checkgatingstatus.pipeline;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.GatingStatusRequest;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.service.JiraGatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.CauseOfInterruption;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the "checkGatingStatus" step that can be used in Jenkinsfile to check gating
 * status managed by Jira Service Desk.
 */
public class JiraCheckGatingStatusStep extends Step implements Serializable {
    private static final long serialVersionUID = 1L;

    private String site;
    private String environmentId;

    @DataBoundConstructor
    public JiraCheckGatingStatusStep(final String environmentId) {
        this.environmentId = environmentId;
    }

    public String getSite() {
        return site;
    }

    @DataBoundSetter
    public void setSite(final String site) {
        this.site = site;
    }

    @Override
    public StepExecution start(final StepContext context) {
        return new CheckGatingStatusExecution(context, this);
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Inject
        private transient JiraCloudPluginConfig globalConfig;

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, WorkflowRun.class);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillSiteItems() {
            ListBoxModel items = new ListBoxModel();
            final List<JiraCloudSiteConfig> siteList = globalConfig.getSites();
            for (JiraCloudSiteConfig siteConfig : siteList) {
                items.add(siteConfig.getSite(), siteConfig.getSite());
            }

            return items;
        }

        @Override
        public String getFunctionName() {
            return "checkGatingStatus";
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraCheckGateStatusStep_DescriptorImpl_DisplayName();
        }
    }

    public static class CheckGatingStatusExecution
            extends SynchronousNonBlockingStepExecution<Boolean> {

        private static final long serialVersionUID = 1L;

        private final JiraCheckGatingStatusStep step;

        public CheckGatingStatusExecution(
                final StepContext context, final JiraCheckGatingStatusStep step) {
            super(context);
            this.step = step;
        }

        /**
         * This execution returns
         *
         * @return {@code true}, if deployment has been approved {@code false}, if gating status is
         * unknown or not approved/rejected yet
         * @throws AbortException if deployment has been rejected, or client has reached limits
         */
        @Override
        protected Boolean run() throws Exception {
            final TaskListener taskListener = requireNonNull(getContext().get(TaskListener.class));
            final WorkflowRun run = requireNonNull(getContext().get(WorkflowRun.class));

            final GatingStatusRequest gatingStatusRequest =
                    new GatingStatusRequest(step.getSite(), step.getEnvironmentId(), run);

            final JiraGatingStatusResponse response =
                    JiraSenderFactory.getInstance()
                            .getJiraGateStateRetriever()
                            .getGatingStatus(gatingStatusRequest);

            logResult(taskListener, response);

            final GatingStatus gatingStatus =
                    response.getGatingStatus()
                            .orElseThrow(() -> new AbortException(response.getMessage()));

            switch (gatingStatus) {
                case ALLOWED:
                    return true;
                case EXPIRED:
                case PREVENTED:
                    Optional.ofNullable(run.getExecutor())
                            .ifPresent(executor -> executor.interrupt(Result.ABORTED, new DeploymentAborted()));
                    return false;
                case AWAITING:
                    return false;
                case INVALID:
                default:
                    throw new AbortException(response.getMessage());
            }
        }

        private void logResult(
                final TaskListener taskListener, final JiraSendInfoResponse response) {
            taskListener
                    .getLogger()
                    .println(
                            "checkGatingStatus: "
                                    + response.getStatus()
                                    + ": "
                                    + response.getMessage());
        }
    }

    private static final class DeploymentAborted extends CauseOfInterruption {

        @Override
        public String getShortDescription() {
            return Messages.JiraCheckGateStatusStep_CauseOfInterruption_Description();
        }
    }
}
