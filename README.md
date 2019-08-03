# Viper Ripper!

This is my spin for a gallery ripper app for [vipergirls](https://vipergirls.to) website.
The purpose of this project is to build a robust and clean application to conveniently download photo galleries using modern web technologies, Java + Spring boot for the back end and angular + electron for the front end.

## How to build
You need a recent version of maven 3.6.1+.

The application support 2 build modes:
 - Desktop app (*Partially cross platform): Self contained app, built with electron, depends on Java runtimes.
 - Server app (Fully cross platform): Depends on Java runtimes, you need a web browser to access the app UI.

Most people will be interested only on the desktop app, however if you have a nas or a server, the server app is there for you.

To build the server app:

    mvn clean install
To build the desktop app:

    mvn clean install -Pelectron

Maven will automatically handle front end compilation. However, for development, you will need to install a recent version of nodejs on your system.

*Partially cross platform:
Technically cross platform, but the Desktop app relies on electron builder to make appropriate binaries for each platform. mac binaries can only be generated on a mac, and i don't own one. So i can only provide binaries for windows and linux.
The Server app however depends only on Java, so it will work just fine on a mac, you only need a browser to access it.
