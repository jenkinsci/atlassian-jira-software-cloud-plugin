Atlassian Jira Software Cloud - Jenkins Plugin
===================== 


Development
===========

Start the local Jenkins instance:

    mvn hpi:run


Jenkins Plugin Maven goals
--------------------------

	hpi:create  Creates a skeleton of a new plugin.
	
	hpi:hpi Builds the .hpi file

	hpi:hpl Generates the .hpl file

	hpi:run Runs Jenkins with the current plugin project

	hpi:upload Posts the hpi file to java.net. Used during the release.
	
	
How to install
--------------

Run 

	mvn hpi:hpi
	
to create the plugin .hpi file.


To install:

1. copy the resulting ./target/rdoc.hpi file to the $JENKINS_HOME/plugins directory. Don't forget to restart Jenkins afterwards.
	
2. or use the plugin management console (http://example.com:8080/pluginManager/advanced) to upload the hpi file. You have to restart Jenkins in order to find the plugin in the installed plugins list.

Early Access Program Signup
-----------------------------------------------------------

In order to start using this Jenkins plugin for Jira Software Cloud, you need to enroll in an early access program. This allows us to enable the required set of functionality in Jira Software Cloud so you can configure the plugin and start receiving data. Please fill out this [form](https://forms.gle/z8QUubZcgy4HyJCs8) to get started.