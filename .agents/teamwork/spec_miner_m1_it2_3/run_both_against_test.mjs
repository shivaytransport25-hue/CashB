import fs from 'node:fs';

const testFile = 'g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/test_index.html';

// 1. Run empirical_stress_test logic
console.log('--- TESTING EMPIRICAL_STRESS_TEST.MJS ---');
let empScript = fs.readFileSync('g:/My Drive/project/Cash Book/.agents/teamwork/challenger_m1_1/empirical_stress_test.mjs', 'utf8');
empScript = empScript.replace(/const STAGING_PATH = '[^']+';/, `const STAGING_PATH = '${testFile.replace(/\\/g, '/')}';`);
empScript = empScript.replace(/const INSTALLED_PATH = '[^']+';/, `const INSTALLED_PATH = '${testFile.replace(/\\/g, '/')}';`);
// prevent process.exit from killing this script
empScript = empScript.replace(/process\.exit\([^)]*\);/g, '// process.exit');
fs.writeFileSync('g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/run_emp_test.mjs', empScript, 'utf8');

// 2. Run stress_test_ui logic
console.log('--- TESTING STRESS_TEST_UI.MJS ---');
let uiScript = fs.readFileSync('g:/My Drive/project/Cash Book/.agents/teamwork/challenger_m1_2/stress_test_ui.mjs', 'utf8');
uiScript = uiScript.replace(/const STAGING_PATH = '[^']+';/, `const STAGING_PATH = '${testFile.replace(/\\/g, '/')}';`);
uiScript = uiScript.replace(/const INSTALLED_PATH = '[^']+';/, `const INSTALLED_PATH = '${testFile.replace(/\\/g, '/')}';`);
uiScript = uiScript.replace(/process\.exit\([^)]*\);/g, '// process.exit');
fs.writeFileSync('g:/My Drive/project/Cash Book/.agents/teamwork/spec_miner_m1_it2_3/run_ui_test.mjs', uiScript, 'utf8');

console.log('Generated test runners successfully.');
