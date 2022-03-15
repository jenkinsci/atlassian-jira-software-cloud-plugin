package com.atlassian.jira.cloud.jenkins.buildinfo.freestyle;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.atlassian.jira.cloud.jenkins.buildinfo.service.FreestyleBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.util.Constants;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class FreeStylePostBuildStep extends Recorder implements Serializable, SimpleBuildStep {

    private static final long serialVersionUID = 1L;

    private String site;
    private String branch;

    @DataBoundConstructor
    public FreeStylePostBuildStep() {
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean perform(
            final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        final FreestyleBuildInfoRequest request =
                new FreestyleBuildInfoRequest(this.getSite(), this.branch, build);
        JiraSenderFactory.getInstance().getFreestyleBuildInfoSender().sendBuildInfo(request);
        return super.perform(build, launcher, listener);
    }

    @SuppressWarnings("unused")
    @Override
    public void perform(
            @Nonnull final Run<?, ?> run,
            @Nonnull final FilePath filePath,
            @Nonnull final Launcher launcher,
            @Nonnull final TaskListener taskListener)
            throws InterruptedException, IOException {
        // perform method from SimpleBuildStep interface
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Inject private transient JiraCloudPluginConfig globalConfig;

        @Override
        public String getDisplayName() {
            return Constants.SEND_BUILD_INFORMATION_TO_JIRA;
        }

        @Override
        public boolean isApplicable(final Class aClass) {
            return true;
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws FormException {
            return super.configure(req, json);
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillSiteItems() {
            ListBoxModel items = new ListBoxModel();
            final List<JiraCloudSiteConfig2> siteList = globalConfig.getSites2();
            items.add("All", null);
            for (JiraCloudSiteConfig2 siteConfig : siteList) {
                items.add(siteConfig.getSite(), siteConfig.getSite());
            }

            return items;
        }
    }
}
