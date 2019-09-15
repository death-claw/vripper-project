console.log("renderer intialized");
const customTitlebar = require("custom-electron-titlebar");
const electron = require("electron").remote;
const contextMenu = require("electron-context-menu");

contextMenu({});
const menu = new electron.Menu();
const titlebar = new customTitlebar.Titlebar({
  backgroundColor: customTitlebar.Color.fromHex("#000000"),
  titleHorizontalAlignment: 'left',
  icon: 'favicon.ico',
  shadow: true,
  menu: menu
});
titlebar.updateTitle("Viper Ripper");