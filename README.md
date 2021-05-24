# VRipper!

This is my spin for a cross platform gallery ripper app for [vipergirls](https://vipergirls.to) website. The purpose of
this project is to build a robust and clean application to conveniently download photo galleries using modern web
technologies, Java + Spring boot for the back end and angular + electron for the front end.

## How to build

You need a recent version of maven 3.6.1+.

The application support 2 build modes:

- Desktop app: Self contained app, built with electron includes Java runtimes.
- Server app: Depends on Java runtimes, you need a web browser to access the app UI.

Most people will be interested only on the desktop app, however if you have a nas or a server, the server app is there
for you.

To build the server app:

    mvn clean install -DskipTests

To build the desktop app:

    mvn clean install -Pelectron -DskipTests

Maven will automatically handle front end compilation. However, for development, you will need to install a recent
version of nodejs on your system.

If you feel generous and want to support me, you can donate some coins, and I would be very grateful 🙏

I accept [PayPal](https://www.paypal.com/myaccount/transfer/homepage/buy/preview) donations, following is my email
address

    dev.vripper@gmail.com
