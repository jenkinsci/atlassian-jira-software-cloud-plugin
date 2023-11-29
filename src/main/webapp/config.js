function showSiteInputs() {
    restoreAllTableSites();
    const siteDataContainer = document.getElementById('siteDataContainer');
    const showSiteFormButton = document.getElementById('showSiteButton');

    siteDataContainer.style.position = 'inherit';
    showSiteFormButton.style.display = 'none';

    toggleSaveSiteForm("true");
}

function hideSiteInputs() {
    restoreAllTableSites();
    const siteDataContainer = document.getElementById('siteDataContainer');
    const showSiteFormButton = document.getElementById('showSiteButton');

    siteDataContainer.style.position = 'absolute';
    showSiteFormButton.style.display = 'block';
    toggleSaveSiteForm("false");
}

function toggleSaveSiteForm(state) {
    const activeInput = document.querySelector('#siteDataContainer [name="active"]');
    activeInput.value = state;
}

function removeSite(index) {
    const siteRow = document.getElementById(`site_${index}`);
    const siteData = document.getElementById(`siteData_${index}`);
    if (siteRow) {
        siteRow.remove();
    }
    if (siteData) {
        siteData.remove();
    }
}

function hideSiteFromTable(index) {
    const siteElement = document.getElementById(`site_${index}`);
    if (siteElement) {
        siteElement.style.display = 'none';
    }
}

function restoreAllTableSites() {
    const sitesRows = document.querySelectorAll('tr[id^="site_"]');
    sitesRows.forEach(site => {
        site.style.display = 'table-row';
    });
    const siteData = document.querySelectorAll('[id^="siteData_"] [name="active"]');
    siteData.forEach(site => {
        site.value = "true";
    });
}

function editSiteData(index) {
    restoreAllTableSites();

    const siteName = document.getElementById(`siteName_${index}`).textContent;
    const webhookUrl = document.getElementById(`webhookUrl_${index}`).textContent;
    const credentialsId = document.getElementById(`credentialsId_${index}`).textContent;

    const siteDataContainer = document.getElementById('siteDataContainer');
    const siteInput = siteDataContainer.querySelector('[name="site"]');
    const webhookUrlInput = siteDataContainer.querySelector('[name="webhookUrl"]');
    const credentialsInput = siteDataContainer.querySelector('[name="_.credentialsId"]');
    toggleSaveSiteForm("true")

    const siteData = document.querySelector('[id^="siteData_' + index + '"] [name="active"]');
    console.log(siteData);
    siteData.value = "false";

    siteInput.value = siteName;
    webhookUrlInput.value = webhookUrl;
    credentialsInput.value = credentialsId;

    siteDataContainer.style.position = 'inherit';
    hideSiteFromTable(index);
    invokeOnChangeChecks();
}

function invokeOnChangeChecks() {
    const webHookUrlInput = document.querySelector('[name="webhookUrl"]');
    const siteInput = document.querySelector('[name="site"]');

    triggerChangeEvent(webHookUrlInput);
    triggerChangeEvent(siteInput);
}

function triggerChangeEvent(element) {
    if (element) {
        element.dispatchEvent(new Event('change'));
    }
}

function validateAutoBuildsRegex() {
    return (new AtlassianRegexTester('autoBuildsRegex', 'autoBuildsRegexTestResponse', 'autoBuildsRegexTestResponse')).test('Please enter the test name of your pipeline step/stage:', []);
}

function validateAutoDeploymentsRegex() {
    return (new AtlassianRegexTester('autoDeploymentsRegex', 'autoDeploymentsRegexTestResponse', 'autoDeploymentsRegexTestResponse')).test('Please enter the test name of your pipeline step/stage:', []);
}