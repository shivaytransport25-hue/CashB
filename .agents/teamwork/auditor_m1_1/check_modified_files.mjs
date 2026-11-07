import fs from 'node:fs';
import path from 'node:path';

function getFilesRecursively(dir, fileList = []) {
  try {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        if (entry.name !== 'node_modules' && entry.name !== '.git' && entry.name !== 'build' && entry.name !== '.dart_tool') {
          getFilesRecursively(fullPath, fileList);
        }
      } else {
        fileList.push(fullPath);
      }
    }
  } catch (err) {
    // ignore inaccessible dirs
  }
  return fileList;
}

const rootDir = 'G:/My Drive/project/Cash Book';
const allFiles = getFilesRecursively(rootDir);

// Filter files modified after 16:10:00 (Worker M1-1 dispatch was 16:14)
const cutoff = new Date('2026-11-06T16:10:00+05:30').getTime();

console.log(`Scanning ${allFiles.length} files in Cash Book for modifications after 16:10...`);
const modified = [];
for (const file of allFiles) {
  try {
    const stats = fs.statSync(file);
    if (stats.mtimeMs > cutoff) {
      modified.push({ file, mtime: stats.mtime.toISOString() });
    }
  } catch (e) {}
}

console.log('Modified files found:');
modified.forEach(m => console.log(`  ${m.mtime}: ${m.file}`));
