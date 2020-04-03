Scraper - A Composable Workflow Framework
=========================================

![version](https://img.shields.io/badge/version-0.11.4-green.svg)
![language](https://img.shields.io/badge/language-java12-blue.svg)
![build](https://img.shields.io/badge/build-gradle-yellowgreen.svg)

[![pipeline status](https://git.server1.link/scraper/scraper/badges/master/pipeline.svg)](https://git.server1.link/scraper/scraper/commits/master)
[![coverage report](https://git.server1.link/scraper/scraper/badges/master/coverage.svg)](https://git.server1.link/scraper/scraper/commits/master)

Scraper is a framework which enables flow-based programming in a declarative way. 
It is based on two main components: 
the core which translates the declarative description (JSON or YAML) into a format that is understood by
the framework, and the actual nodes which can be used to construct a workflow.
The architecture is plugin-based, so nodes can be implemented on their own and provided
to the framework.

The main goal of this framework is to facilitate reuse of code (nodes) and help
managing control flow of programs in an easy way (declarative workflow specification).

# Links

* [Scraper Node Documentation](https://docs.scraper.server1.link)
* [Scraper Wiki](https://wiki.scraper.server1.link)
* [Scraper Binaries](https://binaries.scraper.server1.link)
* [Scraper Editor (prototype, deprecated)](https://editor.scraper.server1.link)

# Documentation

The documentation can be found at the [Scraper Wiki](https://wiki.scraper.server1.link).

# Quickstart - Docker

Scraper is deployed to [Dockerhub](https://hub.docker.com/repository/docker/albsch/scraper).

To use a Scraper container once, use

    docker run -v "$PWD":/rt/ -v "$PWD":/nodes -v "$PWD":/plugins --rm albsch/scraper:latest help

and place your workflow in the current workflow directory. 
'$PWD' can be changed to another working directory if needed.
If custom nodes or plugins are to be supplied (like [dev-nodes](https://github.com/scraperflow/scraper-nodes/releases)),
place the jar(s) in the current working directory (or change '$PWD'), too.


# Quickstart - APT

Scraper is deployed to a [PPA](https://launchpad.net/~albsch/+archive/ubuntu/scraper). 
To install it the PPA:

    sudo add-apt-repository ppa:albsch/scraper
    sudo apt-get update
    sudo apt-get install scraper
    sudo apt-get install scraper-plugins-*
    sudo apt-get install scraper-nodes-*
    
This will install the complete scraper application with all available plugins and all available nodes. 
Scraper can then be executed with the command `scraper`. 
A valid Java 12 environment is needed.
To check it, type `java -version` in your terminal.
[sdkman](https://sdkman.io) is recommended.


To run scraper, run

    scraper

If you need to set JVM options, set them as the environment variable `JAVA_TOOL_OPTIONS`. 
Example:

    export JAVA_TOOL_OPTIONS="-Xms64m -Xmx128m"
    scraper

The console should notify the user if it picked up the options.


You can also inspect the plugins and nodes and install them manually by

    apt list | grep scraper-nodes
    apt list | grep scraper-plugins

Scraper will use following locations:

* `/usr/bin/scraper`: main executable script
* `/usr/lib/scraper`: location of runnable jars (core, plugins, nodes)
* `/var/log/scraper`: default logging location

# Quickstart - Development

Using

      gradle bundleAll

will

* compile the project 
* package the project core and project with all found nodes and plugins as a runnable jar in the `build/libs` folder
  * If the core jar is used, plugins needs to be provided explicitly on the class path
  
         java -cp "/usr/lib/scraper/core/scraper.jar:/usr/lib/scraper/plugins/*:/usr/lib/scraper/nodes/*" scraper.app.Scraper
