
package com.atlassian.jira.cloud.jenkins.configuration

def f = namespace(lib.FormTagLib)

f.form(action: "submitForm", method: "post",name: "jji", autocomplete: "off") {

    def instance = request.getAttribute("config")
    def crumbValue = request.getAttribute("crumbValue")



    f.section(title: "Jira Software Cloud Integration") {
        f.entry(title: _("Jira Cloud Sites")) {
            f.repeatableHeteroProperty(
                    field: instance.FIELD_NAME_SITES,
                    hasHeader: "true",
                    addCaption: _("Add Jira Cloud Site")
            )
        }

        f.entry(title: "Logging") {
            f.checkbox(
                    title: "Activate debug logging",
                    field: instance.FIELD_NAME_DEBUG_LOGGING
            )
        }

        f.optionalBlock(
                title: _("Send builds automatically"),
                name: instance.FIELD_NAME_AUTO_BUILDS,
                checked: instance.getAutoBuildsEnabled()
        ) {
            f.entry(
                    title: _("Pipeline step regex (optional)")
            ) {
                text(_("Leave blank to send a build event at the end of the pipeline."))
                f.textbox(
                        name: instance.FIELD_NAME_AUTO_BUILDS_REGEX,
                        value: instance.getAutoBuildsRegex(),
                        id: "atlBuildsRegex"
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
                checked: instance.getAutoDeploymentsEnabled()
        ) {
            f.entry(
                    title: _("Pipeline step regex")
            ) {
                f.textbox(
                        name: instance.FIELD_NAME_AUTO_DEPLOYMENTS_REGEX,
                        value: instance.getAutoDeploymentsRegex(),
                        id: "atlDeploymentsRegex",
                )

                div(id: "atlTestDeploymentRegexError", class: "error", style: "display:none") {

                }
                div(id: "atlTestDeploymentRegexBlankError", class: "error", style: "display:none") {

                }
                div(id: "atlTextDeploymentRegexSuccess", class: "ok", style: "display:none") {

                }

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

    f.input(type: "hidden", name: "Jenkins-Crumb", value: crumbValue)

    f.input(type: "submit", value: "Savessseee", style: "color: #ff00ff")

}