const { ipcRenderer } = require('electron');

ipcRenderer.on('callNbbResult', (event, arg) => {
  console.log(arg);
  alert(arg);
});

function callNbb() {
  ipcRenderer.send('callNbb');
}

window.addEventListener('DOMContentLoaded', (event) => {
  document.getElementById("callNbb").addEventListener("click", callNbb);
});
