package com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig

def f = namespace(lib.FormTagLib);
def c = namespace(lib.CredentialsTagLib)

raw(
"<script>" +
    new File(getClass().getResource(
            "/com/atlassian/jira/cloud/jenkins/config/JiraCloudPluginConfig/config.js"
    ).toURI()).readLines().join("\n") +
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
                title: _("Pipeline step regex (Optional)")
        ) {
            text(_("Leave blank to send Build at the end of the pipeline."))
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
}