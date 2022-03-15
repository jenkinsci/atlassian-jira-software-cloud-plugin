package com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig

def f = namespace(lib.FormTagLib)
def c = namespace(lib.CredentialsTagLib)

f.entry(title: _("Site Name"), field: "site") {
    f.textbox(default: JiraCloudSiteConfig.DEFAULT_SITE)
}

f.entry(title: _("Webhook URL"), field: "webhookUrl") {
    f.textbox()
}

f.entry(title: _("Secret"), field: "credentialsId") {
    c.select(context: app, includeUser: false, expressionAllowed: false)
}

f.block() {
    f.validateButton(
            title: _("Test settings"),
            progress: _("Testing..."),
            method: "testConnection",
            with: "site,webhookUrl,credentialsId"
    )
}

