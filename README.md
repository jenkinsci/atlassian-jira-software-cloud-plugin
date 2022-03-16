# Jenkins Plugin for Jira Cloud  

This plugin integrates Jenkins with [Jira Software Cloud](https://www.atlassian.com/software/jira) and [Jira Service Management Cloud](https://www.atlassian.com/software/jira/service-management/features/service-desk).

The integration exists to provide a free, easy, secure, and reliable way to connect your Jenkins server, running behind the firewall, and Jira Software Cloud and/or Jira Service Management Cloud. Simply connect your Jenkins server to your Jira site and the integration will start sending build and deployment data to Jira and associate that information with the relevant Jira issues.

This gives your entire team additional visibility and context on every issue detail view in Jira, showing the latest build status or if that work has been successfully deployed to an environment.

You can also use this information to [search across issues using the Jira Query Language (JQL)](https://confluence.atlassian.com/jirasoftwarecloud/advanced-searching-developer-reference-967312910.html), easily answering questions like “which issues in the current sprint have been deployed to production”. You can even add these as quick filters on your boards!

## Prerequisites

Check the following things before you connect Jenkins to Jira Software Cloud:

- You're a site administrator of your Jira Cloud site.
- You're an admin in Jenkins and you can install, update, and delete plugins.
- Your team should be adding issue keys (e.g. FUSE-123) as part of their commit messages (for deployment information) and branch names (for build information). If your team isn't already following this pattern, [learn more about referencing issues in your development work](https://support.atlassian.com/jira-software-cloud/docs/reference-issues-in-your-development-work/).

## Link Jira Software Cloud with Jenkins

### Install the Jira Cloud plugin for Jenkins

Log in to your Jenkins server and navigate to the Plugin Manager.

Select the 'Available' tab and search for 'Atlassian Jira Software Cloud', then install it.

The open-source plugin is hosted at GitHub. [You can check it out here](https://github.com/jenkinsci/atlassian-jira-software-cloud-plugin).

### Install the "Jenkins for Jira" app in Jira

You can install the "Jenkins for Jira" app into your Jira site by using the [direct installation link](https://developer.atlassian.com/console/install/21696c93-1a1d-4dd0-bac0-dfed80a62aba?signature=0959fc868cbcef86a68217b573a595eda2966abfdfad3dbb13edcec9ef9ff5fd&product=jira).

Alternatively, you can install the app via the Jira Marketplace:

1. Open Jira and go to **Apps > Explore more apps**.
1. Type "Jenkins for Jira" into the search box.
1. Click on the "Jenkins for Jira by Atlassian" app (if it doesn't show up, you might have to remove the "Top trending" filter).
1. Click on "Get app".

### Create a webhook in Jira

After installing the "Jenkins for Jira" app into your Jira site, you can create a webhook in Jira:

1. In Jira, go to **Apps > Manage your apps**.
1. In the left sidebar, under "Apps", click the link to the "Jenkins for Jira" app.
1. Click on "Connect a Jenkins server".
1. The app will remind you to install the Jenkins plugin. Click on "Next".
1. Enter a name for your Jenkins server.
1. Copy the webhook URL for later use.
1. Copy the secret for later use.

### Connect Jenkins to the webhook

With the webhook URL and secret, you can now create a connection between Jenkins and Jira.

1. In Jenkins, go to **Manage Jenkins > Configure System** and scroll to the Jira Software Cloud integration section.
1. Select **Add Jira Cloud Site > Jira Cloud Site**. New fields will appear for your site name, webhook URL, and secret.
1. Enter the following details:
   - Site name: The URL for your Jira Cloud site, for example yourcompany.atlassian.net.
   - Webhook URL: The webhook URL you copied from the Jenkins app in Jira earlier.
   - Secret: Select Add > Jenkins
     - For _Kind_, select **Secret text**.
     - For _Secret_, paste the secret you copied from the Jenkins app in Jira earlier.
     - For _Description_, provide a description that helps you identify the secret.
   - Secret: The secret should now show up in the drop down menu. Select your newly created secret.
1. Select **Test connection** to make sure your credentials are valid for your Jira site.
1. Click **Save**.

## Use the Jira cloud integration with Jenkins

This plugin will send build and deployment events to Jira so that they’re visible in your Jira issues, on the deployments timeline, and in the releases hub.

To send information from Jenkins to Jira, your team must include Jira issue keys (e.g. FUSE-123) in their commit messages (for deployment information) and branch names (for build information). Whenever a pipeline runs in Jenkins, the plugin will look for Jira issue keys in the branch name and commit messages. If it finds issue keys, it will send build and deployment information to Jira. If it doesn’t find issue keys, the Jenkins plugin won’t send anything to Jira.

### Sending builds automatically

To automatically send build events without having to add anything to your `Jenkinsfile`, go to **Manage Jenkins > Configure System** and enable the checkbox “Sends builds automatically”.

![Sending builds automatically](docs/images/auto-builds.png)

When you enable this, the plugin will send an "in progress" build event to Jira once a pipeline run has started and a "success" or "failure" build event once the pipeline has finished successfully or stopped due to an error.

If you also specify a regular expression for builds, the plugin will only send a build event to Jira once a build step with a matching name has been finished.

The regular expression `^build$` would match the `build` stage in the following `Jenkinsfile`, for example:

```groovy
pipeline {
    agent  any
    stages {
        stage('build') {
            steps {
                echo  'build done'
            }
        }
    }
}
```

Whenever the pipeline in this Jenkinsfile runs, it will send build events to all configured Jira Cloud sites on start and finish of the `build` stage.

### Sending deployments automatically

To automatically send deployment events without having to add anything to your `Jenkinsfile`, go to **Manage Jenkins > Configure System** and enable the checkbox “Sends deployments automatically”.

![Sending deployments automatically](docs/images/auto-deployments.png)

When you enable this, the plugin will send an "in progress" deployment event to Jira once a build step with a name matching the specified regular expression has started, and a "success" or "failure" deployment event once that build step has finished.

For this to work, the deployment steps in your `Jenkinsfile` must contain the environment name in their name. The regular expression must contain the fragment `(?<envName>.*)` to match the environment name so that the plugin can extract the environment name from the build step names.

Let's look at an example `Jenkinsfile`:

```groovy
pipeline {
    agent  any
    stages {
        stage('deployments') {
            parallel {
                stage('deploy to stg') {
                    steps {
                        echo 'stg deployment done'
                    }
                }
                stage('deploy to prod') {
                    steps {
                        echo 'prod deployment done'
                    }
                }
            }
        }
    }
}
```

If the checkbox "Send deployments automatically" is enabled and the regular expression is set to `^deploy to (?<envName>.*)$`, a run of the above Jenkinsfile will send "in progress" deployment events for the `stg` and `prod` environments to all configured Jira Cloud sites, followed by respective "success" deployment events once the build steps are finished.

### Sending builds explicitly

If you want more control over when to send build events, you can use the `jiraSendBuildInfo` build step:

```groovy
pipeline {
     agent any
     stages {
         stage('Build') {
             steps {
                 echo 'Building...'
             }
             post {
                 always {
                     // previous to version 2.0.0 you must provide parameters to this command (see below)!
                     jiraSendBuildInfo() 
                 }
             }
         }
     }
 }
```

This will send a "success" or "failure" build event to all configured Jira Cloud sites after the `Build` stage has finished successfully or with an error. 

The Jenkins plugin will automatically extract Jira issue keys from the branch name.

You can also specify a Jira site URL to instruct the plugin to send the build event to only this Jira site instead of to all configured Jira sites:

```groovy
pipeline {
     agent any
     stages {
         stage('Build') {
             steps {
                 echo 'Building...'
             }
             post {
                 always {
                     jiraSendBuildInfo site: 'example.atlassian.net', branch: 'TEST-123-awesome-feature'
                 }
             }
         }
     }
 }
```

### Sending deployments explicitly

If you want more control over when to send deployment events, you can use the `jiraSendDeploymentInfo` build step:

```groovy
pipeline {
     agent any
     stages {
         stage('Deploy - Staging') {
             when {
                 branch 'master'
             }
             steps {
                 echo 'Deploying to Staging from master...'
             }
             post {
                 always {
                     jiraSendDeploymentInfo environmentId: 'us-stg-1', environmentName: 'us-stg-1', environmentType: 'staging'
                 }
             }
         }
         stage('Deploy - Production') {
            when {
                branch 'master'
            }
            steps {
                echo 'Deploying to Production from master...'
            }
            post {
                always {
                    jiraSendDeploymentInfo environmentId: 'us-prod-1', environmentName: 'us-prod-1', environmentType: 'production'
                }
            }
         }
     }
 }
```

This will send a "success" or "failure" deployment event to all configured Jira sites at the end of the stages `Deploy - Staging` and `Deploy - Production`. 

You **must** provide the parameters `environmentId`, `environmentName`, and `environmentType`. The `environmentType` must be one of the following: `unmapped`, `development`, `testing`, `staging`, `production`.

You can also provide the parameter `site` to specify to send the deployment events to a single Jira site instead of all configured Jira sites. 

When multiple Jira sites are connected to a Jenkins server, the `site`
parameter is required for `jiraSendDeploymentInfo` with `enableGating:true`.
More details about Deployment Gating can be found [here](https://support.atlassian.com/jira-service-management-cloud/docs/use-deployment-gating-with-jenkins/).

Also, you can specify a branch with the `branch` parameter to define the branch from which to extract Jira issue keys to connect the deployments with.

### Example of a complete `Jenkinsfile`

You can mix build and deployments as in the `Jenkinsfile` below:

```groovy
pipeline {
     agent any
     stages {
         stage('Build') {
             steps {
                 echo 'Building...'
             }
             post {
                 always {
                     jiraSendBuildInfo site: 'example.atlassian.net'
                 }
             }
         }
         stage('Deploy - Staging') {
             when {
                 branch 'master'
             }
             steps {
                 echo 'Deploying to Staging from master...'
             }
             post {
                 always {
                     jiraSendDeploymentInfo environmentId: 'us-stg-1', environmentName: 'us-stg-1', environmentType: 'staging'
                 }
             }
         }
         stage('Deploy - Production') {
            when {
                branch 'master'
            }
            steps {
                echo 'Deploying to Production from master...'
            }
            post {
                always {
                    jiraSendDeploymentInfo environmentId: 'us-prod-1', environmentName: 'us-prod-1', environmentType: 'production'
                }
            }
         }
     }
 }
```

### Creating change requests in Jira Service Management

To automatically create change requests in Jira Service Management from Jenkins, 
you first need to enable Change management in your Information Technology
Service Management (ITSM) project.

To connect Jenkins to your Jira Service Management Cloud project:

1. First, complete the Jira Cloud and Jenkins set-up steps listed above

2. In your Jira Service Management ITSM project, navigate to 
   **Project settings > Change management**

3. Select **Connect Pipeline > Jenkins**, then copy the Service ID at the 
   end of the set-up flow

4. Go to Jenkins, select the Pipeline you want to associate with this 
   service, and select **Build with Parameters**

5. Paste the Service ID from the Change management set-up flow into the 
   Build with Parameters field

When you run the pipeline, it will automatically create a change request 
in Jira Service Management.

For more information about deployment tracking and deployment gating please 
refer to Atlassian documentation:
 - [Deployment Tracking](https://support.atlassian.com/jira-service-management-cloud/docs/use-deployment-tracking-with-jenkins/)
 - [Deployment Gating](https://support.atlassian.com/jira-service-management-cloud/docs/use-deployment-gating-with-jenkins/)


## Support

Read all about the integration between Jenkins and Jira in the [Atlassian Support docs](https://support.atlassian.com/jira-cloud-administration/docs/integrate-with-jenkins/).

If you are having trouble with this plugin, please reach out to [Atlassian support](https://support.atlassian.com/contact/).

## Contributing

Feel free to raise issues and questions via the [GitHub issue tracker](https://github.com/jenkinsci/atlassian-jira-software-cloud-plugin/issues).

Pull requests are welcome any time!
