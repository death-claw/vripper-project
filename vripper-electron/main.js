require('v8-compile-cache');
const { app, BrowserWindow } = require("electron");
const path = require("path");
const url = require("url");
const getPort = require("get-port");
const { spawn } = require("child_process");
const { ipcMain } = require("electron");
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

function createWindow() {

    if (process.platform === 'win32') {
        app.setAppUserModelId("tn.mnlr.vripper");
    }

    let icon;
    if(process.platform === "win32") {
        icon = __dirname + '/icon.ico';
    } else {
        icon = __dirname + '/icon.png';
    }
    win = new BrowserWindow({
        width: 1024,
        height: 800,
        minWidth: 800,
        minHeight: 600,
        webPreferences: {
            nodeIntegration: true,
            enableRemoteModule: true
        },
        icon: icon
    });

    win.removeMenu();
    win.setMenu(null);
    win.setMenuBarVisibility(false);

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

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
    app.quit();
} else {
    getPort().then(port => {
        serverPort = port;
        ipcMain.on("get-port", event => {
            event.reply("port", port);
        });
        let javaBinPath;
        if(appDir !== undefined) {
            javaBinPath = path.join(appDir, "java-runtime/bin/java");
        } else {
            if(process.platform === 'darwin') {
                javaBinPath = path.join(app.getPath('exe'), "../../java-runtime/bin/java");
            } else {
                javaBinPath = path.join(app.getPath('exe'), "../java-runtime/bin/java");
            }
        }
        let jarPath;
        if(appDir !== undefined) {
            jarPath = path.join(appDir, "bin/vripper-server.jar");
        } else {
            if(process.platform === 'darwin') {
                jarPath = path.join(app.getPath('exe'), "../../bin/vripper-server.jar");
            } else {
                jarPath = path.join(app.getPath('exe'), "../bin/vripper-server.jar");
            }
        }
        vripperServer = spawn(javaBinPath, [
            "-Xms256m",
            "-Dvripper.server.port=" + port,
            "-jar",
            jarPath
        ], {
            stdio: 'ignore'
        });
        vripperServer.on('exit', (code, signal) => {
            console.log(`vripper server terminated, code = ${code}, signal = ${signal}`);
            terminated = true;
            app.quit();
        });
    });

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
                    console.log(error);
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
