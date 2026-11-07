# BRIEFING — 2026-11-06T12:22:00Z

## Mission
Synthesize an authoritative technical blueprint for Desktop Google OAuth, session persistence, and bidirectional Firestore sync complying strictly with firestore.rules 12-key schema.

## 🔒 My Identity
- Archetype: spec-miner
- Roles: Specification Miner, Architect, Technical Blueprint Author
- Working directory: g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_m2_3\
- Original parent: e0fececc-c4b3-489c-b697-38e3390bcb51
- Milestone: M2 (Desktop Google Sign-In & Firestore Sync)

## 🔒 Key Constraints
- Authentic Google Sign-In on desktop (Cordova/Chromium Windows app).
- Zero mock users: completely eliminate `usr_google_rajesh_patel` and mock fallbacks.
- Strictly adhere to `firestore.rules` 12-key document schema and enum constraints.
- Session persistence in localStorage with silent restoration on restart.
- Read-only on source code during specification phase; output authoritative blueprint and handoff report.
- Design must conform to enterprise fintech design system established in M1.

## Current Parent
- Conversation ID: e0fececc-c4b3-489c-b697-38e3390bcb51
- Updated: 2026-11-06T12:17:31Z

## Task Summary
- **What to build**: Authoritative blueprint (`desktop_auth_sync_blueprint.md`) synthesizing findings from Explorer M2-1 and M2-2.
- **Success criteria**: Complete specification covering desktop OAuth flow, mock elimination, session persistence, schema mapping for Firestore 12-key whitelist, conflict resolution, offline queue, and UI states.
- **Interface contracts**: `firestore.rules`, `ORIGINAL_REQUEST.md`, `PROJECT.md`
- **Code layout**: Cordova desktop app at `E:\CashBookRelease\app\www\index.html`

## Loaded Skills
- **Source**: `g:\My Drive\project\Cash Book\.agents\skills\cashbook-fix-verify\SKILL.md`
  - **Local copy**: N/A (referenced directly)
  - **Core methodology**: Investigation, safe implementation, financial verification, and structured reporting for Cash Book Pro.
- **Source**: `g:\My Drive\project\Cash Book\.agents\skills\senior-developer\SKILL.md`
  - **Local copy**: N/A (referenced directly)
  - **Core methodology**: Senior developer workflow with verification gates and tested code patterns.

## Key Decisions Made
- Successfully synthesized Explorer M2-1 (desktop OAuth & WebView2 host) and Explorer M2-2 (Firestore sync & 12-key whitelist) into unified authoritative blueprint.
- Defined protocol-neutral `signInWithCredential` architecture with `Persistence.LOCAL` and `onAuthStateChanged` session restoration.
- Defined exact two-way transformation layer (`toFirestoreTransactionPayload` & `fromFirestoreTransactionDoc`) enforcing 12-key whitelist and stripping local fields.
- Fully specified `cashbook_sync_queue` with 5-retry exponential backoff limit (`B11.5`).

## Artifact Index
- `desktop_auth_sync_blueprint.md` — Authoritative blueprint for M2 implementation (33,747 bytes).
- `handoff.md` — Complete five-component handoff report.
