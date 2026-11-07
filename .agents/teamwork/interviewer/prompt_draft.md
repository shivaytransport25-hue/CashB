Overhaul the Cash Book software visual styling and color scheme to replace AI-generated aesthetics with a human-crafted professional accounting/fintech design system, and thoroughly audit, fix, and verify Google Sign-In across both Windows desktop and mobile platforms.

Integrity mode: development

## Requirements

### R1. Human-Crafted Professional Accounting Design System
Replace all generic, oversaturated AI-like gradients, bright neon accents, and cartoonish borders with a human-crafted enterprise fintech / accounting visual design system. The interface across both Windows Desktop (including WebView2 `www/index.html` & CashBookPro) and Mobile (Android / Flutter apps) must feature balanced neutral tones, subtle dividers, high-contrast readable typography, and clean tabular ledger presentations.

### R2. Google Sign-In End-to-End Fix and Session Persistence
Audit and fix the Google Sign-In authentication flow across both Windows desktop (handling WebView2 / local file schemes) and mobile applications. Ensure authentication succeeds reliably without hanging, unhandled errors, or blocked popups, persists user sessions across application restarts, and maintains seamless bidirectional synchronization with Firestore and backend records.

## Acceptance Criteria

### Visual Styling & Aesthetics
- [ ] No oversaturated multi-color gradients, neon glows, or AI-generated styling artifacts remain in the active UI.
- [ ] Color palette uses professional accounting neutrals with deliberate, restrained accent colors for credit/debit and key actions.
- [ ] Typography and numeric figures in transaction tables and ledger views display with high contrast and legible tabular alignment.
- [ ] Design updates are applied and visually consistent across both the Windows desktop software and mobile applications.

### Google Sign-In & Authentication
- [ ] Users can trigger and successfully complete "Sign in with Google" without hanging, getting blocked by popup restrictions, or throwing unhandled errors.
- [ ] Authenticated sessions persist across application restarts on both desktop and mobile platforms.
- [ ] User profile data and associated financial records sync bidirectionally with Firestore upon successful sign-in.
- [ ] Verification passes using the existing Google OAuth and Firebase configurations already present in the codebase.
