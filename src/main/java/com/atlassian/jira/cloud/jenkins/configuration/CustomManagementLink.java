package com.atlassian.jira.cloud.jenkins.configuration;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.cloudbees.plugins.credentials.Credentials;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.security.csrf.CrumbIssuer;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


@Extension
public class CustomManagementLink extends ManagementLink implements Describable<CustomManagementLink> {

    private static final Logger LOGGER = Logger.getLogger(CustomManagementLink.class.getName());

    private JiraCloudPluginConfig config;

    @Override
    public String getIconFileName() {
        return "gear.png";
    }

    private Category category;

    @DataBoundConstructor
    public CustomManagementLink() {
        super();
        this.config = JiraCloudPluginConfig.get();
        this.category = Category.MISC;
    }

    public Category getCategory() {
        return category;
    }

    // Override the getDescriptor() method
    @Override
    public Descriptor<CustomManagementLink> getDescriptor() {
        return new DescriptorImpl();
    }

    // Define the descriptor class
    @Extension
    public static class DescriptorImpl extends Descriptor<CustomManagementLink> {

        @Override
        public String getDisplayName() {
            return "Custom Management";
        }

    }

    @Override
    public String getUrlName() {
        return "customManagement";
    }

    @Override
    public String getDisplayName() {
        return "Custom Management";
    }

    public void doIndex(final StaplerRequest req, final StaplerResponse res) {
        // Get the Jenkins instance
        Jenkins jenkins = Jenkins.getInstanceOrNull();

        if (jenkins != null) {
            CrumbIssuer crumbIssuer = jenkins.getCrumbIssuer();

            if (crumbIssuer != null) {
                String crumb = crumbIssuer.getCrumb(req);

                if (crumb != null) {
                    // Store the crumb value in the session or request attribute
                    HttpSession session = req.getSession(true);
                    if (session != null) {
                        session.setAttribute("Jenkins-Crumb", crumb);
                        req.setAttribute("crumbValue", crumb);
                        // Forward to the index page
                        try {
                            //        // Get the instance of JiraCloudPluginConfig

                            req.setAttribute("config", this.config);
                            res.setContentType("text/html");
                            req.getView(this, "/com/atlassian/jira/cloud/jenkins/configuration/basic.jelly").forward(req, res);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        // Handle a situation where the session is null
                    }
                } else {
                    // Handle a situation where crumb is null
                }
            } else {
                // Handle a situation where crumbIssuer is null
            }
        } else {
            // Handle a situation where Jenkins is null
        }
    }

    public JSONObject transformFormData(final JSONObject formData) throws JSONException {
        JSONArray sites = new JSONArray();

        JSONArray siteNames = formData.getJSONArray("site");
        JSONArray webhookUrls = formData.getJSONArray("webhookUrl");
        JSONArray credentialsIds = formData.getJSONArray("credentialsId");

        for (int i = 0; i < siteNames.size(); i++) {
            JSONObject site = new JSONObject();
            site.put("site", siteNames.getString(i));
            site.put("webhookUrl", webhookUrls.getString(i));
            site.put("includeUser", "false"); // Adding "includeUser" as "false" for each site
            site.put("credentialsId", credentialsIds.getString(i));
            sites.add(site);
        }

        // ADD NEW SITE IF EXISTS
        String siteName = formData.optString("siteNameNew");
        String webhookUrl = formData.optString("webhookUrlNew");
        String credentialsId = formData.optString("credentialsIdNew");

        if (siteName != null && !siteName.isEmpty() &&
                webhookUrl != null && !webhookUrl.isEmpty() &&
                credentialsId != null && !credentialsId.isEmpty()) {

            JSONObject site = new JSONObject();
            site.put("site", siteName);
            site.put("webhookUrl", webhookUrl);
            site.put("includeUser", "false");
            site.put("credentialsId", credentialsId);
            sites.add(site);
        }

        // TODO - IF THESE EXIST ITS A NEW SITE
        LOGGER.info("Site Name: " + siteName);
        LOGGER.info("Webhook URL: " + webhookUrl);
        LOGGER.info("Credentials ID: " + credentialsId);

        JSONObject transformedFormData = new JSONObject();
        transformedFormData.put("sites", sites);
        transformedFormData.put("debugLogging", formData.getBoolean("debugLogging"));

        // Check if "autoBuilds" is a boolean or an object
        if (formData.has("autoBuilds") && formData.optString("autoBuilds").equals("true")) {
            JSONObject autoBuilds = new JSONObject();
            autoBuilds.put("autoBuildsRegex", formData.optString("autoBuildsRegex"));
            transformedFormData.put("autoBuilds", autoBuilds);
        }

        // Check if "autoDeployments" is a boolean or an object
        if (formData.has("autoDeployments") && formData.optString("autoDeployments").equals("true")) {
            JSONObject autoDeployments = new JSONObject();
            autoDeployments.put("autoDeploymentsRegex", formData.optString("autoDeploymentsRegex"));
            transformedFormData.put("autoDeployments", autoDeployments);
        }
        return transformedFormData;
    }

    @RequirePOST
    public void doSaveConfiguration(final StaplerRequest req, final StaplerResponse res) throws ServletException, IOException, Descriptor.FormException {
        LOGGER.info("SAVE CONFIG HAS BEEN CALLED HURRAY");

        String siteName = req.getParameter("siteNameX");
        String webhookUrl = req.getParameter("webhookUrlX");
        String credentialsId = req.getParameter("credentialsIdX");

        // TODO - IF THESE EXIST ITS A NEW SITE
        LOGGER.info("Site Name: " + siteName);
        LOGGER.info("Webhook URL: " + webhookUrl);
        LOGGER.info("Credentials ID: " + credentialsId);

        // Get the JSONObject from the request body
        JSONObject formData = req.getSubmittedForm();
        LOGGER.info(formData.toString());


        LOGGER.info("XXXXXXXXXXXXXX");
        LOGGER.info(transformFormData(formData).toString());

        JSONObject configData = transformFormData(formData);

//
//        LOGGER.info(String.valueOf(formData));
        config.configure(req, configData);

        // Save the updated config
        config.save();

        // Set the HTTP status code to 200 OK
        res.setStatus(HttpServletResponse.SC_OK);

        // Complete the response
        res.getOutputStream().close();
    }

}