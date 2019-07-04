const { app, BrowserWindow } = require("electron");
const path = require("path");
const url = require("url");
const getPort = require("get-port");
const { spawn } = require("child_process");
const { ipcMain } = require("electron");

let win;
let vripperServer;

function createWindow() {
  win = new BrowserWindow({
    width: 1024,
    height: 768,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    }
  });

  win.removeMenu();

  win.loadURL(
    url.format({
      pathname: path.join(__dirname, `vripper-ui/index.html`),
      protocol: "file:",
      slashes: true
    })
  );

  // win.webContents.openDevTools();

  win.on("closed", () => {
    win = null;
  });
}

getPort().then(port => {
  ipcMain.on("get-port", event => {
    setTimeout(() => {
      event.reply("port", port);
    }, 5000);
  });
  vripperServer = spawn("java", [
    "-Dvripper.server.port=" + port,
    "-jar",
    "bin/vripper-server.jar"
  ], {stdio: 'inherit'});
});

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  app.on("second-instance", (event, commandLine, workingDirectory) => {
    if (win) {
      if (win.isMinimized()) win.restore();
      win.focus();
    }
  });

  app.on("ready", createWindow);

  app.on("window-all-closed", () => {
    if (process.platform !== "darwin") {
      vripperServer.kill("SIGTERM");
      app.quit();
    }
  });

  app.on("activate", () => {
    if (win === null) {
      createWindow();
    }
  });
}
