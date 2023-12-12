# VRipper!

This is my spin for a cross-platform gallery ripper for [vipergirls.to](https://vipergirls.to).


![1Jw0oq8h_o](https://github.com/UncleRoger33/vripper-project/assets/66418211/80b44389-7620-4e05-8696-4b62fa626b1b)


## Requirements
Direct access to vipergirls.to (or one of its [alternative domains](https://vipergirls.to/threads/5887340)), consider using a VPN otherwise.


## Supported Image Hosts
The following hosts are supported:

* acidimg.cc  
* imagetwist.com  
* imagezilla.com  
* imgspice.com  
* imagebam.com  
* imgbox.com  
* imx.to  
* pimpandhost.com  
* pixhost.to  
* pixxxels.cc  
* turboimagehost.com  
* postimg.cc  
* imagevenue.com  
* pixroute.to  
* vipr.im  


## Installation
Download the latest version from the Release page and execute the installer depending on your Operating System

Application data (application logs, settings and persisted data) is stored in

    HOME_FOLDER/vripper (Windows)
    HOME_FOLDER/.config/vripper (Linux and macOS)

## Instructions for Jar
You need Java 17+, you can download from https://adoptium.net/

Download the latest jar file from the Release page, open a command prompt and run the jar file using the following command

For the GUI app

    javaw -jar vripper-gui.jar

For the WEB app

    java -jar vripper-web.jar

Application data (application logs, settings and persisted data) is stored in the location where you launched the jar for both GUI and WEB


## How to build

You need JDK 17 and a recent version of maven 3.6.1+

To build run the following:

    mvn clean install

Build artifact is located under

    vripper-project\vripper-gui\target\vripper-gui-{{version}}.jar

Copy the artifact into any other folder and run:

    java -jar vripper-gui-{{version}}.jar
