# Dispatch for Spec Miner: Fintech Design System & Auth Spec Survey
Task: Extract exact specifications for human-crafted accounting/fintech design system (from user image reference and domain best practices) and unified Google Sign-In / session persistence specifications.
Path to Original Request: g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md
Reference Image: C:\Users\WIN\.gemini\antigravity\brain\1407bb07-e95c-496b-ab38-d1549f649037\.user_uploaded\media_1793958737050_b0f8f00b.jpg

## 2026-11-06T10:07:58Z
You are Spec Miner 3 (Fintech Design & Auth Spec Miner).
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\
You MUST read:
1. `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
2. `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\DISPATCH.md`
3. The uploaded visual reference image: `C:\Users\WIN\.gemini\antigravity\brain\1407bb07-e95c-496b-ab38-d1549f649037\.user_uploaded\media_1793958737050_b0f8f00b.jpg` (use view_file to inspect)
4. Domain skill: `g:\My Drive\project\Cash Book\.agents\skills\cashbook-fix-verify\SKILL.md`

Your objective:
Extract exact, authoritative specification requirements for:
1. Enterprise Fintech / Accounting Visual Design System:
   - Specific neutral color palettes (backgrounds, surfaces, borders, text primary/secondary/muted).
   - Deliberate, restrained semantic colors for Accounting: Credit (income/inflow) vs Debit (expense/outflow), status tags.
   - Typography standards: Tabular figures (`tnum` / monospaced numbers for alignments), contrast ratios (WCAG AAA/AA).
   - Layout & Ledger components: Clean tables, subtle hairline dividers, removal of cartoonish shadows, blur effects, or AI multi-color gradients.
   - Cross-platform token mapping: CSS variables for Desktop Web, Android XML/Compose, Flutter ThemeData.
2. Unified Google Sign-In & Auth Architecture Spec:
   - Cross-platform auth flow requirements (WebView2 desktop protocol vs Android/Flutter native Google Sign In).
   - Session persistence specifications (token refresh, auto-login, secure storage across restarts).
   - Firestore bidirectional sync requirements upon login.
- Deliverable:
  - Write full specifications to `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md` and handoff report to `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\handoff.md`.
  - Maintain `progress.md` in your directory.
  - When done, send a message to orchestrator with summary and path.
