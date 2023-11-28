function showSiteInputs() {
    restoreAllTableSites();
    const addNewSiteContainer = document.getElementById('addNewSiteContainer');
    const showSiteFormButton = document.getElementById('showSiteForm');

    addNewSiteContainer.style.position = 'inherit';
    showSiteFormButton.style.display = 'none;';

    toggleSaveSiteForm("true");
}

function xx() {
    console.log('s');
}

function hideSiteInputs() {
    restoreAllTableSites();
    const addNewSiteContainer = document.getElementById('addNewSiteContainer');

    addNewSiteContainer.style.position = 'absolute';
    toggleSaveSiteForm("false");
}

function toggleSaveSiteForm(state) {
    const activeInput = addNewSiteContainer.querySelector('[name="active"]');
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

    const addNewSiteContainer = document.getElementById('addNewSiteContainer');
    const siteInput = addNewSiteContainer.querySelector('[name="site"]');
    const webhookUrlInput = addNewSiteContainer.querySelector('[name="webhookUrl"]');
    const credentialsInput = addNewSiteContainer.querySelector('[name="_.credentialsId"]');
    toggleSaveSiteForm("true")

    const siteData = document.querySelector('[id^="siteData_' + index + '"] [name="active"]');
    console.log(siteData);
    siteData.value = "false";

    siteInput.value = siteName;
    webhookUrlInput.value = webhookUrl;
    credentialsInput.value = credentialsId;

    addNewSiteContainer.style.position = 'inherit';
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
