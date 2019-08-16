# Atlassian Jira Software Cloud Plugin for Jenkins

[![Build Status](https://ci.jenkins.io/job/Plugins/job/atlassian-jira-software-cloud-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/atlassian-jira-software-cloud-plugin/job/master/)

The plugin provides a free, easy, secure, and reliable way to connect and send build and deployment information from your Jenkins server to your Jira Software Cloud site. 
This allows your entire team to see build and deployment info on any issue detail view. It will also let you search across issues for things like "show me all the Jira issues that have been deployed to production".

For more information, please visit the [Wiki page](https://wiki.jenkins.io/display/JENKINS/Atlassian+Jira+Software+Cloud+Plugin).

## How to install

### Early Access Program Signup

In order to start using this Jenkins plugin for Jira Software Cloud, you need to enroll in an early access program. This allows us to enable the required set of functionality in Jira Software Cloud so you can configure the plugin and start receiving data. Please fill out this [form](https://forms.gle/z8QUubZcgy4HyJCs8) to get started!

### Integrate Jira Software Cloud with Jenkins

Once you have signed up for the Early Access Program, please follow the instructions on this page: [Integrate Jira Software Cloud with Jenkins](https://confluence.atlassian.com/adminjiracloud/integrate-jira-software-cloud-with-jenkins-972355471.html)

## Development

Start the local Jenkins instance:

    mvn hpi:run


## Jenkins Plugin Maven goals

	hpi:create  Creates a skeleton of a new plugin.
	
	hpi:hpi Builds the .hpi file

	hpi:hpl Generates the .hpl file

	hpi:run Runs Jenkins with the current plugin project

	hpi:upload Posts the hpi file to java.net. Used during the release.