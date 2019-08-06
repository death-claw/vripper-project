const { app, BrowserWindow } = require("electron");
const path = require("path");
const url = require("url");
const getPort = require("get-port");
const { spawn } = require("child_process");
const { ipcMain } = require("electron");
const commandExists = require("command-exists").sync;
const { dialog } = require("electron");
const axios = require('axios');
const appDir = process.env.APPDIR;

let win;
let vripperServer;
let serverPort;

const maxTerminationAttemps = 5;
let terminationAttemps = 0;
let terminationInteval;
let terminated = false;

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
    height: 800,
    minWidth: 640,
    minHeight: 480,
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
  serverPort = port;
  ipcMain.on("get-port", event => {
    event.reply("port", port);
  });
  vripperServer = spawn("java", [
    "-Dvripper.server.port=" + port,
    "-jar",
    appDir !== undefined ? path.join(appDir, "bin/vripper-server.jar") :
    path.join(app.getPath('exe'), "../bin/vripper-server.jar")
  ], {
    stdio: 'ignore'
  });
  vripperServer.on('exit', (code, signal) => {
    console.log(`vripper server terminated, code = ${code}, signal = ${signal}`);
    terminated = true;
    app.quit();
  });
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
      axios.post('http://localhost:' + serverPort + '/actuator/shutdown', {}, {
        headers: { 'content-type': 'application/json' },
      }).then((response) => {
        terminationInteval=  setInterval(() => {
          terminationAttemps++;
          if(terminated) {
            console.log('viper server terminated');
            clearInterval(terminationInteval);
            app.quit();
          } else if(terminationAttemps > maxTerminationAttemps) {
            console.log('viper server is not terminated');
            console.log('Proceed to kill');
            vripperServer.kill('SIGKILL');
            clearInterval(terminationInteval);
            app.quit();
          }
        }, 1000);
      })
      .catch((error) => {
        // Terminate immediately
        vripperServer.kill('SIGKILL');
        terminated = true;
        app.quit();
      });
    }
  });

  app.on("activate", () => {
    if (win === null) {
      createWindow();
    }
  });
}
