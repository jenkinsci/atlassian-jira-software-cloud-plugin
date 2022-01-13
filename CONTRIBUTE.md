# Contribute to this plugin

Some information to get you up and running if you want to contribute to the development of this plugin.

## Prerequisites

Please make sure that you have a JDK 1.8 installed. The build won't work with more modern JDK versions.

You will also need to the most recent version of Maven 3 installed.

## Build the source

Run `mvn clean package` to build everything.

## Run a local Jenkins instance

You can use the [Maven HPI plugin](https://github.com/jenkinsci/maven-hpi-plugin) to run a local Jenkins instance for testing. This local instance will have the most recent version of the Jira Cloud plugin from your local workspace installed already.

When you run `mvn hpi:run`, the plugin will install and run a Jenkins server into the `/work` folder and you can access it via [http://localhost:8080/jenkins](http://localhost:8080/jenkins).


## Using the integration

You can now follow the steps in the [README](./README.md) to use the integration.