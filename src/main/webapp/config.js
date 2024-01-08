let initialFormData = {};

let webHookUrlInput;
let siteInput ;

window.addEventListener('DOMContentLoaded', () => {


    webHookUrlInput = document.querySelector('[name="webhookUrl"]');
    siteInput = document.querySelector('[name="site"]');


    const form = document.getElementById('saveConfigurationForm');
    const formData = new FormData(form);

    for (const [key, value] of formData.entries()) {
        const element = form.elements[key];
        if (element.type !== 'hidden') {
            initialFormData[value] = value;
        }
    }

    form.addEventListener('change', handleFormChange);
});

function handleFormChange() {
    const form = document.getElementById('saveConfigurationForm');
    const formData = new FormData(form);
    let currentFormData = {};

    for (const [key, value] of formData.entries()) {
        const element = form.elements[key];
        if (element.type !== 'hidden') {
            currentFormData[value] = value;
        }
    }

    invokeOnChangeChecks();
}

const toggleElementDisplay = (element, display) => {
    if (element) {
        element.style.display = display;
    }
};

const setElementPosition = (element, position) => {
    if (element) {
        element.style.position = position;
    }
};

const getSiteDataContainer = () => document.getElementById('siteDataContainer');
const getShowSiteButton = () => document.getElementById('showSiteButton');

const restoreTableSiteData = () => {
    const sitesRows = document.querySelectorAll('tr[id^="site_"]');
    sitesRows.forEach(site => {
        site.classList.remove('selected');
    });

    const siteData = document.querySelectorAll('[id^="siteData_"] [name="active"]');
    siteData.forEach(site => {
        site.value = 'true';
    });
};

const showSiteInputs = () => {
    restoreTableSiteData();
    const siteDataContainer = getSiteDataContainer();
    const showSiteFormButton = getShowSiteButton();

    setElementPosition(siteDataContainer, 'inherit');
    toggleElementDisplay(showSiteFormButton, 'none');
    toggleSaveSiteForm('true');
    handleFormChange();
};

const hideSiteInputs = () => {
    restoreTableSiteData();
    const siteDataContainer = getSiteDataContainer();
    const showSiteFormButton = getShowSiteButton();

    setElementPosition(siteDataContainer, 'absolute');
    toggleElementDisplay(showSiteFormButton, 'block');
    toggleSaveSiteForm('false');
    setSiteFormContent();
    handleFormChange();
};

const toggleSaveSiteForm = (state) => {
    const activeInput = document.querySelector('#siteDataContainer [name="active"]');
    if (activeInput) {
        activeInput.value = state;
    }
};

// This just removes the data from the form, actual deletion is on save.
const removeSite = (index) => {
    const siteRow = document.getElementById(`site_${index}`);
    const siteData = document.getElementById(`siteData_${index}`);
    if(siteRow) siteRow.remove();
    if(siteData) siteData.remove();
    handleFormChange();
};

const highlightSelectedSiteFromTable = (index) => {
    const siteElement = document.getElementById(`site_${index}`);
    if (siteElement) {
        siteElement.classList.add('selected');
    }
};

const editSite = (index) => {
    const siteDataContainer = document.getElementById('siteDataContainer');
    restoreTableSiteData();
    populateSiteForm(index);
    toggleSaveSiteForm('true');

    const selectedRowDataElement = document.querySelector('[id^="siteData_' + index + '"] [name="active"]');
    selectedRowDataElement.value = 'false';

    siteDataContainer.style.position = 'inherit';
    highlightSelectedSiteFromTable(index);
};

const populateSiteForm = (index) => {
    // Get selected row data
    const siteName = document.getElementById(`siteName_${index}`).textContent;
    const webhookUrl = document.getElementById(`webhookUrl_${index}`).textContent;
    const credentialsId = document.getElementById(`credentialsId_${index}`).textContent;

    setSiteFormContent(siteName, webhookUrl, credentialsId);
}

const setSiteFormContent = (site = "", webhook = "", credentials = "") => {
    const siteDataContainer = document.getElementById('siteDataContainer');

    const siteInput = siteDataContainer.querySelector('[name="site"]');
    const webhookUrlInput = siteDataContainer.querySelector('[name="webhookUrl"]');
    const credentialsInput = siteDataContainer.querySelector('[name="_.credentialsId"]');

    // set site form values
    siteInput.value = site;
    webhookUrlInput.value = webhook;
    credentialsInput.value = credentials;

    // Run input validation
    invokeOnChangeChecks(siteInput, webhookUrlInput);
}

// This forces the serverside validation event after we populate the site form
const invokeOnChangeChecks = (...args) => {
    args.forEach(arg => {
        if (arg && typeof arg.onchange === 'function') {
            arg.onchange();
        }
    });
}

const validateAutoBuildsRegex = () => {
    return (new AtlassianRegexTester('autoBuildsRegex', 'autoBuildsRegexTestResponse', 'autoBuildsRegexTestResponse')).test('Please enter the test name of your pipeline step/stage:', []);
};

const validateAutoDeploymentsRegex = () => {
    return (new AtlassianRegexTester('autoDeploymentsRegex', 'autoDeploymentsRegexTestResponse', 'autoDeploymentsRegexTestResponse')).test('Please enter the test name of your pipeline step/stage:', []);
};
