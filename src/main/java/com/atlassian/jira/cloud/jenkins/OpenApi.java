package com.atlassian.jira.cloud.jenkins;

import static java.util.Objects.requireNonNull;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.config.MyJsonObject;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import hudson.model.RootAction;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.json.JsonHttpResponse;
import org.kohsuke.stapler.verb.GET;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

@Extension
public class OpenApi implements RootAction {

    @CheckForNull
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "custom-api";
    }

    @GET
    @WebMethod(name = "get-example")
    public JsonHttpResponse getExample() {
        JSONObject response = JSONObject.fromObject(new MyJsonObject("I am Jenkins"));
        return new JsonHttpResponse(response, 200);
    }

    @GET
    @WebMethod(name = "get-example-param")
    public JsonHttpResponse getWithParameters(
            @QueryParameter(required = true) final String paramValue) {

        requireNonNull(paramValue);

        // Get sitename and webhookurl from request
        String[] result = paramValue.split("\\,"); // splitting the string at ","
        final String sitename = result[0];
        final String webhookUrl = result[1];

        // Create Secret
        boolean secretCreated = false;
        try {
            CredentialsStore credentialsStore = CredentialsProvider.lookupStores(Jenkins.getInstance()).iterator().next();
            CredentialsScope scope = CredentialsScope.GLOBAL;
            String id = "javatest-atrigueiro-id-new";
            String description = "javatest-atrigueiro-desc";
            Secret secretBytes = Secret.fromString("SuperSecret");
            secretCreated = credentialsStore.addCredentials(credentialsStore.getDomains().get(0), new StringCredentialsImpl(scope, id, description, secretBytes));
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
        }


        // Get current Sites and add a new site to the list
        List<JiraCloudSiteConfig> sites = null;
        ArrayList<JiraCloudSiteConfig> newSites = new ArrayList<JiraCloudSiteConfig>();
        JiraCloudPluginConfig pluginConfig = JiraCloudPluginConfig.get();
        if (pluginConfig != null) {
            sites = pluginConfig.getSites();
            newSites.addAll(sites);

            newSites.add(new JiraCloudSiteConfig(sitename, webhookUrl, "javatest-atrigueiro-id-new"));
            pluginConfig.setSites(newSites);
        }

        MyJsonObject myJsonObject = new MyJsonObject("New config created " + newSites);
        JSONObject response = JSONObject.fromObject(myJsonObject);
        return new JsonHttpResponse(response, 200);
    }

    @GET
    @WebMethod(name = "get-error500")
    public JsonHttpResponse getError500() {
        MyJsonObject myJsonObject = new MyJsonObject("You got an error 500");
        JSONObject jsonResponse = JSONObject.fromObject(myJsonObject);
        JsonHttpResponse error500 = new JsonHttpResponse(jsonResponse, 500);
        throw error500;
    }
}