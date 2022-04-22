import { loadFile } from 'nbb';
// leave this in if you want to require applied-science.js-interop in script.cljs:
// import 'nbb/lib/nbb_js_interop.js';

const path = require('path');

// make dep globally available so script.cljs can use it
// this is a workaround for not being able to use dynamic import in nbb within cjs modules
globalThis.path = path;

const { app, BrowserWindow, ipcMain } = require('electron');

const createWindow = () => {
  const win = new BrowserWindow({
    width: 800,
    height: 600,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js')
    }
  });

  win.loadFile('index.html');
};

app.whenReady().then(() => {
  const window = createWindow();
});

ipcMain.on('callNbb', (event, arg) => {
  loadFile(path.join(__dirname, 'script.cljs')).then((res) => {
    res = res.foo();
    event.reply('callNbbResult', res);
  }).catch((err) => {
    console.log(err);
  });
});

