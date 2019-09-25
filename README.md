# Atlassian Jira Software Cloud Plugin for Jenkins

[![Build Status](https://ci.jenkins.io/job/Plugins/job/atlassian-jira-software-cloud-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/atlassian-jira-software-cloud-plugin/job/master/)

The plugin provides a free, easy, secure, and reliable way to connect and send build and deployment information from your Jenkins server to your Jira Software Cloud site. 
This allows your entire team to see build and deployment info on any issue detail view. It will also let you search across issues for things like "show me all the Jira issues that have been deployed to production".

For more information, please visit the [Wiki page](https://wiki.jenkins.io/display/JENKINS/Atlassian+Jira+Software+Cloud+Plugin).

## Integrate Jira Software Cloud with Jenkins

Please follow the instructions on this page: [Integrate Jira Software Cloud with Jenkins](https://confluence.atlassian.com/adminjiracloud/integrate-jira-software-cloud-with-jenkins-972355471.html)

## Development

Start the local Jenkins instance:

    mvn hpi:run


## Jenkins Plugin Maven goals

	hpi:create  Creates a skeleton of a new plugin.
	
	hpi:hpi Builds the .hpi file

	hpi:hpl Generates the .hpl file

	hpi:run Runs Jenkins with the current plugin project

	hpi:upload Posts the hpi file to java.net. Used during the release.
	
## Feedback/Issues

If you would like to report any bugs or share feedback, please create a new issue at https://issues.jenkins-ci.org/ and add `atlassian-jira-software-cloud-plugin` as a component.

[Link to issues](https://issues.jenkins-ci.org/issues/?filter=20841).