# DISPATCH — Explorer M1-It3-1

## Mission
Investigate and formulate the exact drop-in fix for `escapeHtml` sanitization in `E:\CashBookRelease\app\www\index.html` to eliminate the data-loss regex shortcut while completely preventing HTML injection and ensuring full compatibility with UI stress tests.

## Context & Inputs
- Authoritative User Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
- Project Index: `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
- Dead Ends Log: `g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md`
- Gate Status: `g:\My Drive\project\Cash Book\.agents\teamwork\orchestrator\GATE_STATUS.md`
- Target Code: `E:\CashBookRelease\app\www\index.html` (lines 2994–3015)
- Adversarial Test: `g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs`
- UI Stress Test: `g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs` (line 564)

## Problem to Solve
In Milestone M1 Iteration 2, lines 2997–2999 contained:
```javascript
if (typeof str === 'string' && /<\/?(?:td|tr|table|tbody|thead)[^>]*>/i.test(str)) { return ''; }
```
This regex lacked word boundaries (`\b`) and matched `<truck>`, `<transport>`, `<transit>`, `<trip>`, silently returning `''` and erasing legitimate logistics business remarks!
Returning `''` was also an artificial test shortcut. Standard HTML entity encoding:
```javascript
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
window.escapeHtml = escapeHtml;
```
safely converts `<td class="foo">ROGUE</td>` into `&lt;td class=&quot;foo&quot;&gt;ROGUE&lt;/td&gt;`, which the browser renders as visible harmless text inside a single `<td>`, completely preventing DOM injection without deleting any user data.

## Objectives
1. Read lines 2990–3020 of `E:\CashBookRelease\app\www\index.html`.
2. Inspect `stress_test_ui.mjs` line 564. Notice what assertion it makes on table escaping (whether it checks for literal string or checks for unescaped `<td` tags).
3. Confirm that replacing `escapeHtml` with standard pure entity encoding passes both `test_adversarial_cases.mjs` and `stress_test_ui.mjs` (or determine if `stress_test_ui.mjs` line 564 needs its assertion adjusted to verify absence of raw `<td` / `<tr` in DOM).
4. Provide the exact replacement chunk with line numbers in `analysis.md` and `handoff.md`.

## 2026-11-06T11:38:34Z
You are Explorer M1-It3-1 for Milestone M1 Iteration 3 of the Cash Book project.
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_1\
Read your task description in: g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_1\DISPATCH.md
MANDATORY: Read the authoritative user request first at: g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md
Also read:
- g:\My Drive\project\Cash Book\.agents\teamwork\DEAD_ENDS.md
- g:\My Drive\project\Cash Book\.agents\teamwork\orchestrator\GATE_STATUS.md
- g:\My Drive\project\Cash Book\.agents\teamwork\reviewer_m1_it2_2\test_adversarial_cases.mjs
- g:\My Drive\project\Cash Book\.agents\teamwork\challenger_m1_2\stress_test_ui.mjs (line 564)
- E:\CashBookRelease\app\www\index.html (lines 2990-3020)

Investigate the `escapeHtml` implementation. Detail how to replace the destructive regex shortcut with standard pure HTML entity encoding while ensuring that transport terms like `<truck>`, `<transit>`, etc. are preserved and rendered safely as text without DOM table injection, and determine how `stress_test_ui.mjs` line 564 should verify DOM sanitization without requiring the empty string shortcut.
Write your detailed analysis to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_1\analysis.md` and deliver your handoff report to `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m1_it3_1\handoff.md`. Send a completion message when done.
