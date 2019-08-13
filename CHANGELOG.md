# Changelog

## [1.4.8] - 2019-08-13
### Changed
- Add option to force image ordering

## [1.4.7] - 2019-08-13
### Changed
- Fixed a bug in image filename extensions 

## [1.4.6] - 2019-08-06
### Changed
- Fixed a bug when creating image file names
- Remove unnecessary logs
- Fix data.json init 

## [1.4.5] - 2019-08-03
### Changed
- Add min width and height for electron app
### Added
- Add download speed to status bar

## [1.4.4] - 2019-08-01
### Changed
- Fix names for ImgboxHost
- Enhance UI
- Minor fixes
### Added
- Add start and stop button

## [1.4.3] - 2019-07-14
### Changed
- Update some messages
- Fix scrolling issue

## [1.4.2] - 2019-07-14
### Changed
- Fix layout issue in web mode

## [1.4.1] - 2019-07-14
### Changed
- Change title's font
### Added
- Add borders

## [1.4.0] - 2019-07-14
### Changed
- Better error handling on startup
- Better support for linux AppImage
### Added
- Clear all completed
- Add clipboard support
- Add option to remove all posts
- Add confirmation on remove

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