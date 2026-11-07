import fs from 'node:fs';

const targetFile = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';
let content = fs.readFileSync(targetFile, 'utf8');

// Update showToast to handle mock or uninitialized container
const oldShowToast = `    function showToast(message, type = 'info', durationMs = 4500) {
      let container = document.getElementById('toast-container');
      if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
      }`;

const newShowToast = `    function showToast(message, type = 'info', durationMs = 4500) {
      let container = document.getElementById('toast-container');
      if (!container || typeof container.appendChild !== 'function') {
        container = document.createElement('div');
        container.id = 'toast-container';
        if (document.body && typeof document.body.appendChild === 'function') {
          document.body.appendChild(container);
        }
      }`;

if (!content.includes(oldShowToast)) {
  console.log('oldShowToast not found directly, checking...');
} else {
  content = content.replace(oldShowToast, newShowToast);
  fs.writeFileSync(targetFile, content, 'utf8');
  console.log('Successfully replaced showToast in test_index.html');
}
