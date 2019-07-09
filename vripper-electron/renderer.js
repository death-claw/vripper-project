console.log("renderer intialized");
const customTitlebar = require("custom-electron-titlebar");
const electron = require("electron").remote;
const contextMenu = require("electron-context-menu");

contextMenu({});
const menu = new electron.Menu();
const titlebar = new customTitlebar.Titlebar({
  backgroundColor: customTitlebar.Color.fromHex("#3f51b5"),
  menu: menu
});
titlebar.updateTitle("Viper Ripper");