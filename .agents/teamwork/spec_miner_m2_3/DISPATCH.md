# DISPATCH — Spec Miner M2-3

## Mission
Extract and synthesize the complete technical specification for Desktop Google Sign-In, Session Persistence, and Bidirectional Firestore Synchronization, ensuring 100% genuine auth and strict compliance with `firestore.rules`.

## Mandatory Inputs & Context
- Authoritative User Request: `g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md`
- Project Index: `g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md`
- Gate Status: `g:\My Drive\project\Cash Book\.agents\teamwork\orchestrator\GATE_STATUS.md`
- Firestore Security Rules: `firestore.rules` in workspace root
- Web Client ID: `266747615266-js33duggusre95kg20b4habn2aae8etb.apps.googleusercontent.com`
- Firebase Project: `gen-lang-client-0037390170`
- Target Code: `E:\CashBookRelease\app\www\index.html`
- Explorer 1 Report: `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_1\handoff.md`
- Explorer 2 Report: `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\handoff.md`

## Objectives
1. Read the handoff reports from Explorer M2-1 and Explorer M2-2 as soon as they complete.
2. Formulate the authoritative technical specification:
   - Authentic Google OAuth Flow for Desktop: Detailed flow avoiding popup blocking under `file:///` or local origin, utilizing Web Client ID, capturing ID token / auth credentials, and establishing genuine Firebase Auth session.
   - Mock Elimination: Complete removal of `usr_google_rajesh_patel` and all dummy fallbacks.
   - Durable Session Persistence: Secure storage of user session in `localStorage` / IndexedDB, silent token renewal, automatic restoration on app launch.
   - Firestore Bidirectional Sync Engine: Detailed schema mapper strictly enforcing the 12-key whitelist and value enum constraints of `firestore.rules`, conflict resolution strategy (last-write-wins by `updatedAt`), and offline queueing.
   - UI Integration: Status indicators conforming to the enterprise fintech daylight/dark design system established in Milestone M1.
3. Write the authoritative blueprint `desktop_auth_sync_blueprint.md` and deliver handoff to `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\handoff.md`.


## 2026-11-06T12:07:29Z
You are Spec Miner M2-3 for Milestone M2 (Desktop Google Sign-In & Firestore Sync).
Your working directory is: g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\
Read your task description in: g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\DISPATCH.md
MANDATORY: Read the authoritative user request first at: g:\My Drive\project\Cash Book\.agents\teamwork\ORIGINAL_REQUEST.md
Also read:
- g:\My Drive\project\Cash Book\.agents\teamwork\PROJECT.md
- g:\My Drive\project\Cash Book\firestore.rules
- E:\CashBookRelease\app\www\index.html

Wait for or coordinate with Explorer M2-1 (`9dd0e968-4688-435c-8f2b-70452d931359`, handoff at `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_1\handoff.md`) and Explorer M2-2 (`8565ae97-d427-4222-80b3-5cfc8ac0e485`, handoff at `g:\My Drive\project\Cash Book\.agents\teamwork\explorer_m2_2\handoff.md`).
Synthesize their findings into the authoritative technical blueprint:
`g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\desktop_auth_sync_blueprint.md`
covering authentic Google OAuth for desktop, elimination of mock user `usr_google_rajesh_patel`, persistent sessions in `localStorage`, and bidirectional Firestore sync compliant with `firestore.rules` 12-key whitelist.
Deliver your handoff report to `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\handoff.md` and send a message when done.
