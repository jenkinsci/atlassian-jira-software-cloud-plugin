package com.atlassian.jira.cloud.jenkins.configuration;

import hudson.Extension;
import hudson.model.ManagementLink;
import hudson.security.csrf.CrumbIssuer;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.http.HttpSession;
import java.util.logging.Logger;

@Extension
public class CustomManagementLink extends ManagementLink {

    private static final Logger LOGGER = Logger.getLogger(CustomManagementLink.class.getName());

    @Override
    public String getIconFileName() {
        return "gear.png";
    }

    @Override
    public String getUrlName() {
        return "customManagement";
    }

    @Override
    public String getDisplayName() {
        return "Custom Management";
    }

    @RequirePOST
    public void doSubmitForm(final StaplerRequest req, final StaplerResponse res) {
//        String formData = req.getParameter("data");

        // Process the form data as needed
        try {
            res.sendRedirect("successPage");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                            res.setContentType("text/html");
                            req.getView(this, "/com/atlassian/jira/cloud/jenkins/configuration/index.jelly").forward(req, res);
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


//    public void doIndex(final StaplerRequest req, final StaplerResponse res) {
//        try {
//            res.setContentType("text/html");
//            req.getView(this, "/com/atlassian/jira/cloud/jenkins/configuration/index.jelly").forward(req, res);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}

//
//package com.atlassian.jira.cloud.jenkins.configuration;
//
//import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
//import hudson.Extension;
//import hudson.model.Descriptor;
//import hudson.model.ManagementLink;
//import net.sf.json.JSONObject;
//import org.kohsuke.stapler.StaplerRequest;
//import org.kohsuke.stapler.StaplerResponse;
//import org.kohsuke.stapler.interceptor.RequirePOST;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.ServletException;
//import java.io.IOException;
//
//@Extension
//public class JiraIntegrationManagementLink extends ManagementLink {
//
//    private final Logger logger = LoggerFactory.getLogger(JiraIntegrationManagementLink.class);
//    @Override
//    public String getIconFileName() {
//        return "notepad.png";
//    }
//
//    @Override
//    public String getUrlName() {
//        return "jiraintegration";
//    }
//
//    @Override
//    public String getDisplayName() {
//        return "Jira Integration";
//    }
//
//    // Handle the custom URL path
//    public void doIndex(final StaplerRequest req, final StaplerResponse res) throws IOException, ServletException {
//
//        logger.info("SHOW PAGE");
//        res.setContentType("text/html");
//
//        // Get the instance of JiraCloudPluginConfig
//        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
//
//        // Pass the JiraCloudPluginConfig instance to the Groovy script
//        req.setAttribute("config", config);
//
//        // Forward the request to the Groovy script
//        req.getView(config, "/com/atlassian/jira/cloud/jenkins/configuration/indexx.jelly").forward(req, res);
//    }
//
//
//
//    @RequirePOST
//    public void doSubmit(final StaplerRequest req, final StaplerResponse res) throws IOException, ServletException, Descriptor.FormException {
//
//        // Log the message to the console
//        logger.info("Form save called!!!!!!!!!!!!!.");
//        // Get the instance of JiraCloudPluginConfig
//        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
//
//        // Update the config with the form data
//        JSONObject formData = req.getSubmittedForm();
//
//        logger.info(String.valueOf(formData));
//        config.configure(req, formData);
//
//        // Save the updated config
//        config.save();
//
//        // Redirect back to the configuration page
//        res.sendRedirect2(req.getContextPath() + "/manage/jiraintegration");
//    }
//
//    @Override
//    public boolean getRequiresConfirmation() {
//        logger.info("requires conf");
//        return true;
//    }
//
//    public String getSubmitUrl() {
//        return getUrlName() + "/submit";
//    }
//
//}