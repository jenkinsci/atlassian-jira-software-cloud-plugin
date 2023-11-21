package com.atlassian.jira.cloud.jenkins.configuration;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.cloudbees.plugins.credentials.Credentials;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import hudson.security.csrf.CrumbIssuer;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
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

//    private JiraCloudSiteConfig jiraCloudSiteConfig = new JiraCloudSiteConfig();

    private String selectedName;
    private String banana;


    // Method to fill items for the dropdown dynamically
    public JSONArray doFillNamesItems() {
        JSONArray items = new JSONArray();
        // Add logic here to populate dropdown items dynamically
        items.add("Option 1");
        items.add("Option 2");
        items.add("Option 3");
        return items;
    }

    // Getter and Setter for selectedName and banana
    public String getSelectedName() {
        return selectedName;
    }

    public void setSelectedName(final String selectedName) {
        this.selectedName = selectedName;
    }

    public String getBanana() {
        return banana;
    }

    public void setBanana(final String banana) {
        this.banana = banana;
    }

//    public static class MyDescriptor extends Descriptor<ManagementLink> {
//
//        // Return items for a dropdown or other configurations
//        public ListBoxModel doFillGoalTypeItems() {
//            ListBoxModel items = new ListBoxModel();
//            items.add("Build Goal", "buildGoal");
//            items.add("SpotBugs goal", "spotBugsGoal");
//            return items;
//        }
//    }
    @Override
    public String getIconFileName() {
        return "gear.png";
    }


    private Category category;

    @DataBoundConstructor
    public CustomManagementLink() {
        super();
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

        public ListBoxModel doFillGoalTypeItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("Build Goal", "buildGoal");
            items.add("SpotBugs goal", "spotBugsGoal");
            return items;
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

    public String credentialsId;

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(final String credentialsId) {
        this.credentialsId = credentialsId;
    }

    @RequirePOST
    public void doSubmitForm(final StaplerRequest req, final StaplerResponse res) throws Descriptor.FormException, IOException, ServletException {
        // Log the message to the console
        LOGGER.info("Form save called!!!!!!!!!!!!!.");
        // Get the instance of JiraCloudPluginConfig
//        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();

        // Retrieve form data using getParameterMap()
        Map<String, String[]> parameterMap = req.getParameterMap();

        // Log the form data
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String[] paramValues = entry.getValue();
            for (String paramValue : paramValues) {
                LOGGER.info(paramName + ": " + paramValue);
            }
        }

        LOGGER.info("parameterMap Data: " + parameterMap);
//
//        LOGGER.info(String.valueOf(formData));
//        config.configure(req, formData);

        // Save the updated config
//        config.save();

        // Redirect back to the configuration page
        res.sendRedirect2(req.getContextPath() + "/manage/customLink");
    }

    public void doIndex(final StaplerRequest req, final StaplerResponse res) {
        // Get the Jenkins instance
        Jenkins jenkins = Jenkins.getInstanceOrNull();

        if (jenkins != null) {
            CrumbIssuer crumbIssuer = jenkins.getCrumbIssuer();

            if (crumbIssuer != null) {
                String crumb = crumbIssuer.getCrumb(req);
                LOGGER.info("CSRF Crumb Value: " + crumb);

                if (crumb != null) {
                    // Store the crumb value in the session or request attribute
                    HttpSession session = req.getSession(true);
                    if (session != null) {
                        session.setAttribute("Jenkins-Crumb", crumb);
                        req.setAttribute("crumbValue", crumb);
                        // Forward to the index page
                        try {
                            //        // Get the instance of JiraCloudPluginConfig
                            JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
                            LOGGER.info("CONFIG DATA: " + config.toString());
                            LOGGER.info("##### config.getSites().get(0).getSite() " + config.getSites().get(0).getSite());
                            LOGGER.info("##### config.getSites().get(0).getWebhookUrl() " + config.getSites().get(0).getWebhookUrl());
                            LOGGER.info("##### config.getSites().get(0).getCredentialsId() " + config.getSites().get(0).getCredentialsId());

                            List<Credentials> credentialsList = jenkins.getExtensionList(com.cloudbees.plugins.credentials.Credentials.class);

                            LOGGER.info("Credential ID: " + credentialsList.toString());
                            for (Credentials credential : credentialsList) {
                                LOGGER.info("Credential ID: " + credential.toString());
                                // Add any other details you want to display
                            }


                    //
                    //        // Pass the JiraCloudPluginConfig instance to the Groovy script
                            req.setAttribute("config", config);
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


//    public ListBoxModel doFillCredentialsIdItems(@QueryParameter final String credentialsId) {
//        return jiraCloudSiteConfig.doFillCredentialsIdItems(credentialsId);
//    }

    @RequirePOST
    public void doSaveSite(final StaplerRequest req, final StaplerResponse res) throws ServletException, IOException {
        LOGGER.info("SAVE SITE HAS BEEN CALLED HURRAY");


        //

        // Set the HTTP status code to 200 OK
        res.setStatus(HttpServletResponse.SC_OK);

        // Complete the response
        res.getOutputStream().close();
    }

    // New method to save the site configuration
    private void saveSite(final Map<String, String[]> parameterMap) {


        LOGGER.info("SAVE SITE HAS BEEN CALLED HURRAY");
        LOGGER.info("SAVE SITE HAS BEEN CALLED HURRAY");
        // Extract the required data from the parameter map
//        String crumb = parameterMap.get("Jenkins-Crumb")[0];
//        String siteName = parameterMap.get("siteName")[0];
//        String webhookUrl = parameterMap.get("webhookUrl")[0];
//        String credentialsId = parameterMap.get("credentialsId")[0];

        // Do whatever you need to do to save the site configuration
        // For example, you can access the JiraCloudPluginConfig instance and update the configuration
//        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
        // Update the config with the extracted data
        // config.setSiteName(siteName);
        // config.setWebhookUrl(webhookUrl);
        // config.setCredentialsId(credentialsId);
        // ...

        // Save the updated config
        // config.save();
    }



}