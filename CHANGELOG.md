# Changelog

## [1.3.0-rc0] - 2019-07-04
### Changed
- Change default setting for autostart false -> true
- Change default setting for max concurrent downloads 1 -> 4
### Added
- Desktop app built with electron
- Add the option to remove a post

## [1.2.0] - 2019-06-23
### Added
- Add a shutdown button to gracefully shutdown the app
- Open and redirect the browser to the app address on startup
- Add the option to run the app in headless mode (-Djava.awt.headless=true)

## [1.1.1] - 2019-06-22
### Changed
- Fix a stability issue with progress updates when the app is runninh for a long time

## [1.0.0] - 2019-06-22
### Added
- Add real time communication for progress updates via websocket (SockJS)
- Add settings for download path, max concurrent download, enable/disable integration with vipergirls
- Add integration with vipergitls.to (possibility to auto leave thanks after grab post)
- Add support for acidimg.cc, imagezilla.net, imgbox.com, imx.to, pixhost.to, turboimagehost.com