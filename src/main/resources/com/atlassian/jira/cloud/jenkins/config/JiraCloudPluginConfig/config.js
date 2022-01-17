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
                var message = "Matches: ";
                var isFirst = true;
                for (var idx in expectedGroupsArray) {
                    var group = expectedGroupsArray[idx];
                    if (!isFirst) {
                        message += ", ";
                    }
                    message += group + "=" + parsed.groups[group];
                    isFirst = false;
                }
                this._renderSuccessMessage(message);
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

