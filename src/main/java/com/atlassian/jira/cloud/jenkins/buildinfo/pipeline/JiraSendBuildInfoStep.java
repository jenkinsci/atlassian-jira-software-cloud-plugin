package com.atlassian.jira.cloud.jenkins.buildinfo.pipeline;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

/**
 * Implementation of the "jiraSendBuildInfo" step that can be used in Jenkinsfile to send build
 * updates to a Jira site.
 */
public class JiraSendBuildInfoStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String site;
    private String branch;

    @DataBoundConstructor
    public JiraSendBuildInfoStep() {
        // Empty constructor
    }

    @Nullable
    public String getSite() {
        return site;
    }

    @Nullable
    public String getBranch() {
        return branch;
    }

    @DataBoundSetter
    public void setSite(final String site) {
        this.site = site;
    }

    @DataBoundSetter
    public void setBranch(final String branch) {
        this.branch = branch;
    }

    @Override
    public StepExecution start(final StepContext stepContext) throws Exception {
        return new JiraSendBuildInfoStepExecution(stepContext, this);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Inject private transient JiraCloudPluginConfig globalConfig;

        @Override
        public Set<Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return "jiraSendBuildInfo";
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraSendBuildInfoStep_DescriptorImpl_DisplayName();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillSiteItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("All", null);
            final List<JiraCloudSiteConfig> siteList = globalConfig.getSites();
            for (JiraCloudSiteConfig siteConfig : siteList) {
                items.add(siteConfig.getSite(), siteConfig.getSite());
            }

            return items;
        }
    }

    public static class JiraSendBuildInfoStepExecution
            extends SynchronousNonBlockingStepExecution<List<JiraSendInfoResponse>> {

        private final JiraSendBuildInfoStep step;

        public JiraSendBuildInfoStepExecution(
                final StepContext context, final JiraSendBuildInfoStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected List<JiraSendInfoResponse> run() throws Exception {
            final TaskListener taskListener = getContext().get(TaskListener.class);
            final WorkflowRun workflowRun = getContext().get(WorkflowRun.class);

            final JiraBuildInfoRequest request =
                    new MultibranchBuildInfoRequest(step.getSite(), step.getBranch(), workflowRun);

            final List<JiraSendInfoResponse> allResponses =
                    JiraSenderFactory.getInstance().getJiraBuildInfoSender().sendBuildInfo(request);

            allResponses.forEach(response -> logResult(taskListener, response));

            return allResponses;
        }

        private void logResult(
                final TaskListener taskListener, final JiraSendInfoResponse response) {
            taskListener
                    .getLogger()
                    .println(
                            "jiraSendBuildInfo("
                                    + response.getJiraSite()
                                    + "): "
                                    + response.getStatus()
                                    + ": "
                                    + response.getMessage());
        }
    }
}
