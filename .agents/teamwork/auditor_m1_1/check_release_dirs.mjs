import fs from 'node:fs';
import path from 'node:path';

function getFilesRecursively(dir, fileList = []) {
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        getFilesRecursively(fullPath, fileList);
      } else {
        fileList.push(fullPath);
      }
    }
  } catch (err) {}
  return fileList;
}

const dirs = [
  'E:/CashBookRelease',
  'C:/Users/WIN/AppData/Local/Programs/CashBookPro'
];

const cutoff = new Date('2026-11-06T16:10:00+05:30').getTime();

for (const d of dirs) {
  console.log(`Checking ${d}...`);
  const files = getFilesRecursively(d);
  const modified = [];
  for (const f of files) {
    try {
      const stats = fs.statSync(f);
      if (stats.mtimeMs > cutoff) {
        modified.push({ file: f, mtime: stats.mtime.toISOString() });
      }
    } catch (e) {}
  }
  console.log(`  Modified files in ${d}: ${modified.length}`);
  modified.forEach(m => console.log(`    ${m.mtime}: ${m.file}`));
}
