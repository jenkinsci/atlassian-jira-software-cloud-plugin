package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.model.Descriptor;
import hudson.model.ManagementLink;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ConfigManagementLinkTest {
    @Mock private StaplerRequest mockRequest;

    @Mock private StaplerResponse mockResponse;
    @Mock private JiraCloudPluginConfig mockedConfig;

    @Rule public JenkinsRule jRule = new JenkinsRule();

    private SecretRetriever secretRetriever;

    private ConfigManagementLink classUnderTest;

    private PluginConfigApi pluginConfigApi;

    @Before
    public void setUp() throws InstantiationException, IllegalAccessException {
        secretRetriever = mock(SecretRetriever.class);
        pluginConfigApi = mock(PluginConfigApi.class);
        final ConfigManagementLink configManagementLink = new ConfigManagementLink();
        classUnderTest = configManagementLink.getClass().newInstance();
        classUnderTest.setSecretRetriever(secretRetriever);
        classUnderTest.setPluginConfigApi(pluginConfigApi);

        setupMocks();
    }

    @Test
    public void testGetCategory() {
        // when
        final ManagementLink.Category category = classUnderTest.getCategory();

        // then
        assertThat(category).isEqualTo(ManagementLink.Category.CONFIGURATION);
    }

    @Test
    public void testGetDisplayName() {
        // when
        final String displayName = classUnderTest.getDisplayName();

        // then
        assertThat(displayName).isEqualTo("Atlassian Jira Software Cloud");
    }

    @Test
    public void testGetUrlName() {
        // when
        final String urlName = classUnderTest.getUrlName();

        // then
        assertThat(urlName).isEqualTo("atlassian-jira-software-cloud");
    }

    @Test
    public void testGetIconFileName() {
        // when
        final String iconFileName = classUnderTest.getIconFileName();

        // then
        assertThat(iconFileName).isEqualTo("notepad.png");
    }

    @Test
    public void testRemoveInvalidSites() {
        // given
        JSONObject formData = new JSONObject();
        JSONArray sitesArray = new JSONArray();
        JSONObject site1 = new JSONObject();
        site1.put("active", "true");
        JSONObject site2 = new JSONObject();
        site2.put("active", "false");
        JSONObject site3 = new JSONObject();
        site3.put("active", "true");
        sitesArray.add(site1);
        sitesArray.add(site2);
        sitesArray.add(site3);
        formData.put("sites", sitesArray);

        // when
        JSONObject transformedData = classUnderTest.removeInvalidSites(formData);

        // then
        JSONArray updatedSitesArray = transformedData.getJSONArray("sites");
        assertThat(updatedSitesArray.size()).isEqualTo(2);
        assertThat(updatedSitesArray.getJSONObject(0).optString("active")).isEqualTo("true");
        assertThat(updatedSitesArray.getJSONObject(1).optString("active")).isEqualTo("true");
    }

    @Test
    public void testGetRegexFromFormDataWhenKeyExists() {
        // given
        JSONObject formData = new JSONObject();
        JSONObject autoBuilds = new JSONObject();
        autoBuilds.put("autoBuildsRegex", "buildRegex");
        formData.put("autoBuilds", autoBuilds);

        // when
        String regex = classUnderTest.getRegexFromFormData(formData, "autoBuilds");

        // then
        assertThat(regex).isEqualTo("buildRegex");
    }

    @Test
    public void testGetRegexFromFormDataWhenKeyDoesNotExist() {
        // given
        JSONObject formData = new JSONObject();

        // when
        String regex = classUnderTest.getRegexFromFormData(formData, "nonExistingKey");

        // then
        assertThat(regex).isEmpty();
    }

    @Test
    public void testGetRegexFromFormDataWhenKeyExistsButNoRegex() {
        // given
        JSONObject formData = new JSONObject();
        JSONObject autoBuilds = new JSONObject();
        formData.put("autoBuilds", autoBuilds);

        // when
        String regex = classUnderTest.getRegexFromFormData(formData, "autoBuilds");

        // then
        assertThat(regex).isEmpty();
    }

    @Test
    public void testDoIndex() throws IOException {

        // when
        classUnderTest.doIndex(mockRequest, mockResponse);

        // then
        verify(mockRequest).setAttribute(eq("config"), any());
        verify(mockResponse).setContentType("text/html");
    }

    @Test
    public void testDoSaveConfiguration()
            throws ServletException, IOException, Descriptor.FormException {
        // given
        JSONObject formData = new JSONObject();
        formData.put("sites", new JSONArray());

        when(mockRequest.getSubmittedForm()).thenReturn(formData);

        //        PowerMockito.mockStatic(Stapler.class);
        StaplerResponse mockStaplerResponse = mock(StaplerResponse.class);

        // when
        classUnderTest.doSaveConfiguration(mockRequest, mockResponse);

        // then
        verify(mockResponse).sendRedirect("/jenkins/manage/");
    }

    private void setupMocks() {
        MockitoAnnotations.openMocks(this);
        setPluginConfigApi();
        JiraCloudPluginConfig mockedConfig = Mockito.mock(JiraCloudPluginConfig.class);

        doNothing().when(mockRequest).setAttribute(eq("config"), any());
        doNothing().when(mockResponse).setContentType(eq("text/html"));
    }

    private void setPluginConfigApi() {
        when(pluginConfigApi.sendConnectionData(
                        any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
    }
}
