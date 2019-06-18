package com.atlassian.jira.cloud.jenkins.buildinfo.pipeline;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSenderFactory;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
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
import java.util.Set;

/**
 * Implementation of the "jiraSendBuildInfo" step that can be used in Jenkinsfile to send build
 * updates to a Jira site.
 */
public class JiraSendBuildInfoStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String site;

    @DataBoundConstructor
    public JiraSendBuildInfoStep(final String site) {
        this.site = site;
    }

    public String getSite() {
        return site;
    }

    @DataBoundSetter
    public void setSite(final String site) {
        this.site = site;
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
            final List<JiraCloudSiteConfig> siteList = globalConfig.getSites();
            for (JiraCloudSiteConfig siteConfig : siteList) {
                items.add(siteConfig.getSite(), siteConfig.getSite());
            }

            return items;
        }
    }

    public static class JiraSendBuildInfoStepExecution
            extends SynchronousNonBlockingStepExecution<JiraBuildInfoResponse> {

        private final JiraSendBuildInfoStep step;

        public JiraSendBuildInfoStepExecution(
                final StepContext context, final JiraSendBuildInfoStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected JiraBuildInfoResponse run() throws Exception {
            final TaskListener taskListener = getContext().get(TaskListener.class);
            final Run build = getContext().get(Run.class);

            final JiraBuildInfoRequest request = new JiraBuildInfoRequest(step.getSite(), build);

            final JiraBuildInfoResponse response =
                    JiraBuildInfoSenderFactory.getInstance()
                            .getJiraBuildInfoSender()
                            .sendBuildInfo(request);

            logResult(taskListener, response);

            return response;
        }

        private void logResult(
                final TaskListener taskListener, final JiraBuildInfoResponse response) {
            taskListener
                    .getLogger()
                    .println(
                            "jiraSendBuildInfo: "
                                    + response.getStatus()
                                    + ": "
                                    + response.getMessage());
        }
    }
}
