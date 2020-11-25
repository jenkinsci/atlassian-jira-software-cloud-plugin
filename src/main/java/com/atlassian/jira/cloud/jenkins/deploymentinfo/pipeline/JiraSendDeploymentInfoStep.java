package com.atlassian.jira.cloud.jenkins.deploymentinfo.pipeline;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoRequest;
import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of the "jiraSendDeploymentInfo" step that can be used in Jenkinsfile to send
 * deployment updates to a Jira site.
 */
public class JiraSendDeploymentInfoStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String site;
    private String environmentId;
    private String environmentName;
    private String environmentType;
    private String state;
    private List<String> serviceIds = new ArrayList<>();
    private Boolean enableGating = Boolean.FALSE;
    private List<String> issueKeys = new ArrayList<>();

    @DataBoundConstructor
    public JiraSendDeploymentInfoStep(
            final String environmentId,
            final String environmentName,
            final String environmentType) {
        this.environmentId = environmentId;
        this.environmentName = environmentName;
        this.environmentType = environmentType;
    }

    public String getSite() {
        return site;
    }

    @DataBoundSetter
    public void setSite(final String site) {
        this.site = site;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    @DataBoundSetter
    public void setEnvironmentId(final String environmentId) {
        this.environmentId = environmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    @DataBoundSetter
    public void setEnvironmentName(final String environmentName) {
        this.environmentName = environmentName;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    @DataBoundSetter
    public void setEnvironmentType(final String environmentType) {
        this.environmentType = environmentType;
    }

    @DataBoundSetter
    public void setState(final String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    @Override
    public StepExecution start(final StepContext stepContext) throws Exception {
        return new JiraSendDeploymentInfoStepExecution(stepContext, this);
    }

    public List<String> getServiceIds() {
        return serviceIds;
    }

    @DataBoundSetter
    public void setServiceIds(final List<String> serviceIds) {
        this.serviceIds = serviceIds;
    }

    public Boolean getEnableGating() {
        return enableGating;
    }

    public List<String> getIssueKeys() {
        return issueKeys;
    }

    @DataBoundSetter
    public void setEnableGating(final Boolean enableGating) {
        this.enableGating = enableGating;
    }

    @DataBoundSetter
    public void setIssueKeys(final List<String> issueKeys) {
        this.issueKeys = issueKeys;
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
            return "jiraSendDeploymentInfo";
        }

        @Override
        public String getDisplayName() {
            return Messages.JiraSendDeploymentInfoStep_DescriptorImpl_DisplayName();
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

        @SuppressWarnings("unused")
        public ListBoxModel doFillEnvironmentTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("development", "development");
            items.add("testing", "testing");
            items.add("staging", "staging");
            items.add("production", "production");

            return items;
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillStateItems() {
            ListBoxModel items = new ListBoxModel();
            for (String state : State.ALLOWED_STATES) {
                items.add(state, state);
            }
            return items;
        }
    }

    public static class JiraSendDeploymentInfoStepExecution
            extends SynchronousNonBlockingStepExecution<JiraSendInfoResponse> {

        private final JiraSendDeploymentInfoStep step;

        public JiraSendDeploymentInfoStepExecution(
                final StepContext context, final JiraSendDeploymentInfoStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected JiraSendInfoResponse run() throws Exception {
            final TaskListener taskListener = getContext().get(TaskListener.class);
            final WorkflowRun workflowRun = getContext().get(WorkflowRun.class);
            final Set<String> serviceIds = ImmutableSet.copyOf(step.getServiceIds());
            final Set<String> issueKeys = ImmutableSet.copyOf(step.getIssueKeys());

            final JiraDeploymentInfoRequest request =
                    new JiraDeploymentInfoRequest(
                            step.getSite(),
                            step.getEnvironmentId(),
                            step.getEnvironmentName(),
                            step.getEnvironmentType(),
                            step.getState(),
                            serviceIds,
                            step.getEnableGating(),
                            issueKeys,
                            workflowRun);
            final JiraSendInfoResponse response =
                    JiraSenderFactory.getInstance()
                            .getJiraDeploymentInfoSender()
                            .sendDeploymentInfo(request);

            logResult(taskListener, response);

            return response;
        }

        private void logResult(
                final TaskListener taskListener, final JiraSendInfoResponse response) {
            taskListener
                    .getLogger()
                    .println(
                            "jiraSendDeploymentInfo: "
                                    + response.getStatus()
                                    + ": "
                                    + response.getMessage());
        }
    }
}
