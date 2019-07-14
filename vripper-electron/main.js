const { app, BrowserWindow } = require("electron");
const path = require("path");
const url = require("url");
const getPort = require("get-port");
const { spawn } = require("child_process");
const { ipcMain } = require("electron");
const commandExists = require("command-exists").sync;
const { dialog } = require("electron");
const appDir = process.env.APPDIR;

console.log('App path', path.join(app.getPath('exe'), "../bin/vripper-server.jar"));
console.log('App image path', appDir);

let win;
let vripperServer;

process.on("uncaughtException", err => {
  dialog.showErrorBox(err.message, err.stack);
  process.exit(1);
});

if (!commandExists("java")) {
  dialog.showErrorBox(
    "Java command is missing",
    "Java cannot be found on your PATH, make sure to install java before running Viper Ripper"
  );
  process.exit(1);
}

function createWindow() {
  let icon;
  if(process.platform === "win32") {
    icon = path.join(__dirname, `icon.ico`);
  } else {
    icon = path.join(__dirname, `icon.icns`);
  }
  win = new BrowserWindow({
    width: 1024,
    height: 768,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    },
    icon: icon
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
    event.reply("port", port);
  });
  vripperServer = spawn("java", [
    "-Dvripper.server.port=" + port,
    "-jar",
    appDir !== undefined ? path.join(appDir, "bin/vripper-server.jar") :
    path.join(app.getPath('exe'), "../bin/vripper-server.jar")
  ], {stdio: 'ignore'});
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
      if(vripperServer != null) {
        vripperServer.kill("SIGTERM");
      }
      app.quit();
    }
  });

  app.on("activate", () => {
    if (win === null) {
      createWindow();
    }
  });
}
