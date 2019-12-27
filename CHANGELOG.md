# Changelog

## [2.9.0] - 2019-12-27
### Changed
- Fix build issue

## [2.9.0] - 2019-12-27
### Changed
- Decrease timeout for fast fail
### Added
- Allow many hosts to run at the same time
- Add title to gallery
- Add global concurrent downloads
- Add counter badge in link collector

## [2.8.1] - 2019-12-24
### Changed
- Fix issue with path creation

## [2.8.0] - 2019-12-24
### Added
- Photo gallery feature

## [2.7.1] - 2019-12-21
### Added
- Add version to title bar

## [2.7.0] - 2019-12-21
### Changed
- Add missing icon
### Added
- Add support for pixxxels

## [2.6.0] - 2019-12-21
### Changed
- Disable download button when nothing selected in Link collector grab menu
- Better quality app icon
- Implement context menu on download tab
- Add stop remove and start button to context menu
- Link will not be removed automatically from link collector tab
- Some UI fixes

## [2.5.1] - 2019-12-09
### Changed
- Disable download button when nothing selected in Link collector grab menu
### Added
- Add hosts column in Link collector grab menu

## [2.5.0] - 2019-12-08
### Changed
- Change loading screen
- UI Fixes
- Fix bug with previews
### Added
- Add link collector feature
- Add support for multiple links at a time

## [2.4.1] - 2019-11-26
### Changed
- Increase attempts for establishing ws connection between front and back end
- Change default download folder to Home folder (installer), App folder (portable)

## [2.4.0] - 2019-11-23
### Changed
- Pimpandhost Host support

## [2.3.2] - 2019-11-23
### Changed
- Fix a bug with the scan component

## [2.3.1] - 2019-11-22
### Changed
- Fix connexion reset issue
- Fix Post Status when autostart option is selected
- Add stop all button

## [2.3.0] - 2019-11-21
### Changed
- UI enhancements and overhaul
- Stability fixes with HTTP connections
- MacOS official support
- Use local sockjs and material icons font

## [2.2.5] - 2019-11-09
### Changed
- Exclude thumbs download for imagezilla and pixhost
- Change retry on fail logic (may help avoid having download errors)

## [2.2.4] - 2019-10-05
### Added
- AppImage target for linux

## [2.2.3] - 2019-10-01
### Changed
- Strip libjvm.so for debian

## [2.2.2] - 2019-10-01
### Changed
- Update product name

## [2.2.1] - 2019-10-01
### Changed
- Update dependency list for deb package

## [2.2.0] - 2019-10-01
### Added
- Embedd JRE within the app
### Changed
- Fix 'Start' on pending items
- Upgrade to Java 11

## [2.1.0] - 2019-09-30
### Added
- Add a create a subfolder per thread option

## [2.0.4] - 2019-09-27
### Changed
- Fix some issues with ImxHost

## [2.0.3] - 2019-09-23
### Changed
- Update deb dependencies

## [2.0.2] - 2019-09-23
### Changed
- Add portable mode

## [2.0.1] - 2019-09-22
### Changed
- Change app name

## [2.0.0] - 2019-09-22
### Added
- Add sub folder option
- Add clear finished option
### Changed
- UI enhancements
- Bug fixes
- Change data and log paths

## [1.6.5] - 2019-09-18
### Changed
- electron update

## [1.6.4] - 2019-09-18
### Changed
- Change menu item name

## [1.6.3] - 2019-09-18
### Changed
- Better multithread management
- Fix partial state UI

## [1.6.2] - 2019-09-17
### Changed
- Rework UI colors
- Fix scan input bug

## [1.6.1] - 2019-09-16
### Changed
- Rework UI
- Fix clipboard monitoring

## [1.6.0] - 2019-09-15
### Changed
- Rework the UI
- Add a dark theme
- Fix the force ordering bug
- Other bug fixes

## [1.5.2] - 2019-09-01
### Added
- imx.to host fix

## [1.5.1] - 2019-08-24
### Added
- Bug fixes

## [1.5.0] - 2019-08-24
### Added
- Display logged in user
- Add post details when clicking on main list items (Status, Post URL, Host)
- Add open download location on main list items menu
- Add support for multi-post threads (Show a menu to select posts to download)
- Add link to image from details list

## [1.4.10] - 2019-08-15
### Changed
- Fix a rename issue on windows

## [1.4.9] - 2019-08-13
### Changed
- Update Built-By to be generic

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