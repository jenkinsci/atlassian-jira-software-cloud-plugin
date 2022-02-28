package com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)

raw(
"<script>" +
    getClass().getResourceAsStream(
            "/com/atlassian/jira/cloud/jenkins/config/JiraCloudPluginConfig/config.js"
    ).readLines().join("\n") +
"</script>"
)


f.section(title: "Jira Software Cloud Integration") {
    f.entry(title: _("Jira Cloud Sites"),
            help: descriptor.getHelpFile()) {

        f.repeatableHeteroProperty(
                field: instance.FIELD_NAME_SITES,
                hasHeader: "true",
                addCaption: _("Add Jira Cloud Site"))
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

            span(
                    class: "yui-button",
                    onClick: "return (new AtlassianRegexTester('atlBuildsRegex', 'atlTestBuildRegexError', 'atlTextBuildRegexSuccess'))" +
                                ".test('Please enter the test name of your pipeline step/stage:', []);"
            ) {
                button(
                        onClick: "return false;"
                ) {
                    text(_("Test Pipeline step regex"))
                }
            }
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
            span(
                    class: "yui-button",
                    onClick: "return (new AtlassianRegexTester('atlDeploymentsRegex', 'atlTestDeploymentRegexError', 'atlTextDeploymentRegexSuccess'))" +
                            ".test('Please enter the test name of your pipeline step/stage:', ['envName']);"
            ) {
                button(
                        onClick: "return false;"
                ) {
                    text(_("Test Pipeline step regex"))
                }
            }

            raw(
                    "<script>atlWatchNotEmpty('atlDeploymentsRegex', 'atlTestDeploymentRegexBlankError', 'Pipeline step regex cannot be empty!');</script>"
            )
        }
    }
}