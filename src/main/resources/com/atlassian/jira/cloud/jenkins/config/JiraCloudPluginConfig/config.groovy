package com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)
def st = namespace("jelly:stapler")

st.adjunct(includes: "com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig.config")

f.section(title: "Jira Software Cloud Integration") {
    f.entry(title: _("Jira Cloud Sites"),
            help: descriptor.getHelpFile()) {

        f.repeatableHeteroProperty(
                field: instance.FIELD_NAME_SITES,
                hasHeader: "true",
                addCaption: _("Add Jira Cloud Site"))
    }

    f.entry(title: "Logging"){
        f.checkbox(
                title: "Activate debug logging",
                field: instance.FIELD_NAME_DEBUG_LOGGING
        )
    }

    f.optionalBlock(
            title: _("Send builds automatically"),
            name: instance.FIELD_NAME_AUTO_BUILDS,
            checked: instance.autoBuildsEnabled ?: false,
            help: descriptor.getHelpFile("autoBuilds")
    ) {
        f.entry(
                title: _("Pipeline step regex (optional)")
        ) {
            text(_("Leave blank to send a build event at the end of the pipeline."))
            f.textbox(
                    name: instance.FIELD_NAME_AUTO_BUILDS_REGEX,
                    value: instance.autoBuildsRegex,
                    id: "atlBuildsRegex",
            )

            div(id: "atlTestBuildRegexError", class: "error", style: "display:none") {

            }
            div(id: "atlTextBuildRegexSuccess", class: "ok", style: "display:none") {

            }

            button(
                    "style": "float: right;",
                    class: "jenkins-button jira-scp-regex-tester",
                    "data-regex-textbox-id": "atlBuildsRegex",
                    "data-error-div-id": "atlTestBuildRegexError",
                    "data-success-div-id": "atlTextBuildRegexSuccess",
                    "data-prompt-message": "Please enter the test name of your pipeline step/stage:",
                    "data-expected-groups-array": "[]",
                    ("Test Pipeline step regex")
            )
        }
    }

    f.optionalBlock(
            title: _("Send deployments automatically"),
            name: instance.FIELD_NAME_AUTO_DEPLOYMENTS,
            checked: instance.autoDeploymentsEnabled ?: false,
            help: descriptor.getHelpFile("autoDeployments")
    ) {
        f.entry(
                title: _("Pipeline step regex")
        ) {
            f.textbox(
                    name: instance.FIELD_NAME_AUTO_DEPLOYMENTS_REGEX,
                    value: instance.autoDeploymentsRegex,
                    id: "atlDeploymentsRegex",
            )

            div(id: "atlTestDeploymentRegexError", class: "error", style: "display:none") {

            }
            div(id: "atlTestDeploymentRegexBlankError", class: "error", style: "display:none") {

            }
            div(id: "atlTextDeploymentRegexSuccess", class: "ok", style: "display:none") {

            }

            // NOTE: in Java underscores in the names of the groups are forbidden! See:
            // https://stackoverflow.com/questions/21271972/i-cant-use-a-group-name-like-this-abc-def-using-patterns
            button(
                    "style": "float: right;",
                    class: "jenkins-button jira-scp-regex-tester",
                    "data-regex-textbox-id": "atlDeploymentsRegex",
                    "data-error-div-id": "atlTestDeploymentRegexError",
                    "data-success-div-id": "atlTextDeploymentRegexSuccess",
                    "data-prompt-message": "Please enter the test name of your pipeline step/stage:",
                    "data-expected-groups-array": "[\"envName\"]",
                    ("Test Pipeline step regex")
            )
        }
    }
}