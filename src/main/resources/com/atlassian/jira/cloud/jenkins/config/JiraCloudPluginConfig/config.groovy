package com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)


f.section(title: "Jira Software Cloud Integration") {
    f.entry(title: _("Jira Cloud Sites"),
            help: descriptor.getHelpFile()) {

        f.repeatableHeteroProperty(
                field: "sites",
                hasHeader: "true",
                addCaption: _("Add Jira Cloud Site"))
    }
}