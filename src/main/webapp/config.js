function toggleElementDisplay(element, display) {
    if (element) {
        element.style.display = display;
    }
}

function setElementPosition(element, position) {
    if (element) {
        element.style.position = position;
    }
}

function getSiteDataContainer() {
    return document.getElementById('siteDataContainer');
}

function getShowSiteButton() {
    return document.getElementById('showSiteButton');
}

function showSiteInputs() {
    restoreAllTableSites();
    const siteDataContainer = getSiteDataContainer();
    const showSiteFormButton = getShowSiteButton();

    setElementPosition(siteDataContainer, 'inherit');
    toggleElementDisplay(showSiteFormButton, 'none');
    toggleSaveSiteForm("true");
}

function hideSiteInputs() {
    restoreAllTableSites();
    const siteDataContainer = getSiteDataContainer();
    const showSiteFormButton = getShowSiteButton();

    setElementPosition(siteDataContainer, 'absolute');
    toggleElementDisplay(showSiteFormButton, 'block');
    toggleSaveSiteForm("false");
}

function toggleSaveSiteForm(state) {
    const activeInput = document.querySelector('#siteDataContainer [name="active"]');
    if (activeInput) {
        activeInput.value = state;
    }
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

function highlightSiteFromTable(index) {
    const siteElement = document.getElementById(`site_${index}`);
    if (siteElement) {
        siteElement.classList.add('selected');
    }
}

function restoreAllTableSites() {
    const sitesRows = document.querySelectorAll('tr[id^="site_"]');
    sitesRows.forEach(site => {
        site.classList.remove('selected');
    });

    const siteData = document.querySelectorAll('[id^="siteData_"] [name="active"]');
    siteData.forEach(site => {
        site.value = "true";
    });
}

function restoreAllTableSites() {
    const sitesRows = document.querySelectorAll('tr[id^="site_"]');
    sitesRows.forEach(site => {
        site.classList.remove('selected');
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
    highlightSiteFromTable(index);
    invokeOnChangeChecks();
}

function invokeOnChangeChecks(s, w) {
    const webHookUrlInput = s || document.querySelector('[name="webhookUrl"]');
    const siteInput = w || document.querySelector('[name="site"]');

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