//
// Please maintain tests in config.js.test.html
//

var AtlassianRegexTester = function(regexTextboxId, errorDivId, successDivId) {
    this._regexTextboxId = regexTextboxId;
    this._errorDivId = errorDivId;
    this._successDivId = successDivId;
};

AtlassianRegexTester.prototype._escapeHtml = function(str) {
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
};

AtlassianRegexTester.prototype._renderMessage = function(elementId, safeMessage) {
    var elt = document.getElementById(elementId);
    elt.innerHTML = safeMessage;
    elt.style.display = 'block';
};

AtlassianRegexTester.prototype._renderErrorMessage = function(safeMessage) {
    var elt = document.getElementById(this._errorDivId);
    elt.innerHTML = safeMessage;
    elt.style.display = 'block';
};

AtlassianRegexTester.prototype._renderSuccessMessage = function(safeMessage) {
    var elt = document.getElementById(this._successDivId);
    elt.innerHTML = safeMessage;
    elt.style.display = 'block';
};

AtlassianRegexTester.prototype._hideElt = function(elementId) {
    var elt = document.getElementById(elementId);
    elt.style.display = 'none';
};

AtlassianRegexTester.prototype._getTextRegex = function() {
    return document.getElementById(this._regexTextboxId).value;
};

AtlassianRegexTester.prototype._isCapturingGroupsSupported = function() {
    try {
        new RegExp("Hello (?<world>.*)");
        return true;
    } catch (e) {
        return false;
    }
};

AtlassianRegexTester.prototype._getUserInput = function(message) {
    return prompt(message);
};

AtlassianRegexTester.prototype.test = function(promptMessage, expectedGroupsArray) {
    this._hideElt(this._errorDivId);
    this._hideElt(this._successDivId);

    if (expectedGroupsArray.length > 0 && !this._isCapturingGroupsSupported()) {
        this._renderErrorMessage( "Your browser does not support \"Named capturing groups\", cannot execute the test!")
        return false;
    }

    var regexStr = this._getTextRegex();
    if (!regexStr || !regexStr.trim()) {
        this._renderErrorMessage("Empty RegEx, nothing to test");
        return false;
    }

    var regex = null;

    try {
        regex = new RegExp(regexStr);
    } catch (e) {
        console.warn(e);
        this._renderErrorMessage('"' + this._escapeHtml(regexStr) + '" is not a valid RegEx string. ' + this._escapeHtml(e.toString()));
        return false;
    }

    var userInput = this._getUserInput(promptMessage);
    if (!userInput) {
        return false;
    }

    if (regex.test(userInput)) {
        if (expectedGroupsArray.length > 0) {
            try {
                var parsed = regex.exec(userInput);
                if (!parsed.groups) {
                    parsed.groups = {};
                }
                var notFoundGroups = [];
                var message = "Matches: ";
                var isFirst = true;
                for (var idx = 0; idx < expectedGroupsArray.length; idx ++) {
                    var group = expectedGroupsArray[idx];
                    if (!isFirst) {
                        message += ", ";
                    }
                    message += (group + "=" + parsed.groups[group]);
                    if (!parsed.groups[group]) {
                        notFoundGroups.push(group);
                    }
                    isFirst = false;
                }
                if (notFoundGroups.length > 0) {
                    this._renderErrorMessage("Error: the values for the following groups were not found: " + notFoundGroups.join(", "));
                } else {
                    this._renderSuccessMessage(message);
                }
                return false;
            } catch (e) {
                console.warn(e);
                this._renderErrorMessage('Browser error. ' + this._escapeHtml(e.toString()));
                return false;
            }
        } else {
            this._renderSuccessMessage('Matches!');
            return false;
        }
    } else {
        this._renderErrorMessage('"' + this._escapeHtml(userInput) + '" does not match!');
    }
    return false;
};

function atlWatchNotEmpty(inputId, errorDivId, errorMessage) {
    setInterval(function() {
        var inputElt = document.getElementById(inputId);
        if (inputElt) {
            var errorDiv = document.getElementById(errorDivId);
            if (!inputElt.value) {
                errorDiv.innerHTML = AtlassianRegexTester.prototype._escapeHtml(errorMessage);
                errorDiv.style.display = 'block';
            } else {
                errorDiv.style.display = 'none';
            }
        }
    }, 100);
}

document.addEventListener("DOMContentLoaded", () => {
    atlWatchNotEmpty('atlDeploymentsRegex', 'atlTestDeploymentRegexBlankError', 'Pipeline step regex cannot be empty!');

    document.querySelectorAll(".jira-scp-regex-tester").forEach((button) => {
        button.addEventListener("click", (event) => {
            event.preventDefault();

            const data = event.target.dataset;
            const { regexTextboxId, errorDivId, successDivId, promptMessage } = data;
            const expectedGroupsArray = JSON.parse(data.expectedGroupsArray);

            new AtlassianRegexTester(regexTextboxId, errorDivId, successDivId)
                .test(promptMessage, expectedGroupsArray);
        });
    });
});
