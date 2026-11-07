# Desktop Visual Styling & Anti-Patterns Fix Strategy (Milestone 1)

- **Explorer**: Explorer M1-1 (Desktop Visual Styling & Anti-Patterns)
- **Target Files**:
  1. Primary Staging Source: `E:\CashBookRelease\app\www\index.html` (5,573 lines)
  2. Installed Desktop Target: `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html` (5,118 lines)
- **Specification Authority**: `g:\My Drive\project\Cash Book\.agents\teamwork\spec_miner_survey_3\spec_requirements.md`
- **Visual Reference Master**: `C:\Users\WIN\.gemini\antigravity\brain\1407bb07-e95c-496b-ab38-d1549f649037\.user_uploaded\media_1793958737050_b0f8f00b.jpg`
- **Date**: 2026-11-06

---

## 1. Executive Summary

This investigation formulates an exact, actionable fix strategy for the Worker to overhaul the Windows Desktop WebView2 application (`index.html`). The interface previously contained AI-generated aesthetic anti-patterns, including:
1. Cartoonish pulsing blur halos (`@keyframes pulseGlow`, `filter: blur(16px)`) around startup graphics.
2. Sci-fi neon glows (`box-shadow: 0 0 8px var(--win-green)`) on cloud sync indicators.
3. Oversaturated, garish gradients on buttons (`.sidebar-new-voucher-btn`), modal headers, and nav items.
4. Diffuse deep drop shadows (`box-shadow: 0 14px 32px ...` up to `0 25px 60px ...`).
5. Over-rounded border radii (16px to 26px) inconsistent with utilitarian enterprise accounting tools.
6. Missing tabular numeral alignment (`font-variant-numeric: tabular-nums`), causing ledger numbers to jitter.
7. Low-contrast and washed-out text in Light Mode (`.amount-balance` hardcoded `#f1f5f9` against daylight backgrounds).

This document details every offending line, selector, and inline style across both the staging and installed repositories, and provides direct, drop-in replacement CSS code blocks conforming to the Enterprise Fintech Design System tokens.

---

## 2. Anti-Pattern Inventory & Precise Mapping

### 2.1 Summary Comparison Table

| Anti-Pattern Category | Staging Line(s) (`E:\CashBookRelease`) | Installed Line(s) (`C:\Users\WIN\...`) | Severity | Spec Requirement |
|---|---|---|---|---|
| `@keyframes pulseGlow` | Lines 20–23 | Lines 20–23 | Critical | Delete entirely |
| Startup Aura `filter: blur(16px)` | Line 1275 | Line 1227 | Critical | Delete entire glowing div |
| Backdrop Blur Halos (`blur(3px..12px)`) | Lines 950, 2293, 2497, 2561 | Lines 902, 2208 | High | Replace with clean solid overlay |
| Neon Glow on `.sync-dot` | Line 380 | Line 332 | High | `box-shadow: none;` solid green |
| Saturated Button Gradients | Lines 536, 547, 552 | Lines 488, 499, 504 | High | Solid Amber `#D97706` |
| Action Ribbon Gradients | Lines 120, 418 | Lines 72, 370 | Medium | Flat slate/daylight fills |
| Nav Link Active Gradient | Line 627 | Line 579 | Medium | Solid surface highlight |
| Modal Header Gradients | Lines 2341, 2403, 2501 | Lines 2256, 2318 | High | Solid surface with 1px border |
| Toast Alert Gradients | Lines 58, 62, 66, 70 | (Added in v2.0.0 staging) | Medium | Solid surface + solid border |
| Diffuse Drop Shadows | Lines 49, 328, 689, 736, 789, 960, 1137, 1276, 2002, 2294, 2340, 2402, 2498, 2562 | Lines 83, 280, 641, 688, 741, 912, 1089, 1228, 1917, 2209, 2255, 2317 | High | Restrained enterprise shadows (`0 1px 3px` or `0 8px 24px`) |
| Cartoonish Radii (16px–26px) | Lines 1275, 1276, 2294, 2298, 2498 | Lines 1227, 1228, 2209, 2213 | High | Standardize to 6px (cards) & 8px (modals) |
| Missing Tabular Numerals | Lines 888–890, 1535–1537 | Lines 840–842 | High | Add `font-variant-numeric: tabular-nums` & monospace |
| Invisible Light Balance Text | Line 890 (`#f1f5f9` hardcoded) | Line 842 | High | Use `var(--win-text)` |

---

## 3. Detailed Selector & Code Replacements

### Section A: Foundation CSS Tokens (`:root` & `body[data-theme="light"]`)

#### Target Location:
- `E:\CashBookRelease\app\www\index.html`: Lines 76–116
- `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`: Lines 28–68

#### Problem:
- Color palette uses AI-generated saturated yellow-gold (`#f59e0b`), washed-out pastels, and lacks explicit accounting tokens (credit container, debit container, transfer container).
- Missing semantic CSS variables for hairline borders, table row dividers, and standardized subtle shadows.

#### Drop-In Replacement for Foundation Variables:

```css
    :root {
      /* Neutral Foundation Tokens (Dark Slate Ledger) */
      --win-bg: #0B111B;              /* Deep Navy Black Canvas */
      --win-surface: #111A28;         /* Dark Slate Surface Card */
      --win-surface-2: #182438;       /* Slate Surface 2 (inputs, subtle backgrounds) */
      --win-surface-3: #21314D;       /* Slate Surface 3 (hover / active) */
      --win-border: #1E293B;          /* 1px Hairline Slate Border */
      --win-border-light: #334155;    /* Strong Slate Border (perimeters, outlines) */
      --win-text: #F8FAFC;            /* Primary High-Contrast Text (Slate 50) */
      --win-text-muted: #94A3B8;      /* Secondary/Muted Text (Slate 400) */
      
      /* Semantic Accounting Accents (Dark Mode) */
      --win-accent: #D97706;          /* Wealth Amber Solid Action */
      --win-accent-hover: #B45309;    /* Wealth Amber Darker Hover */
      --win-green: #22C55E;           /* Credit / Inflow / Jama Text */
      --win-green-bg: rgba(6, 78, 59, 0.25); /* Credit Container Fill */
      --win-green-border: #059669;    /* Credit Hairline Border */
      --win-red: #F87171;             /* Debit / Outflow / Udhar Text */
      --win-red-bg: rgba(127, 29, 29, 0.25); /* Debit Container Fill */
      --win-red-border: #DC2626;      /* Debit Hairline Border */
      --win-blue: #60A5FA;            /* Transfer / Bank Accent Text */
      --win-blue-bg: rgba(30, 58, 95, 0.25); /* Transfer Container Fill */
      --win-blue-border: #2563EB;     /* Transfer Hairline Border */
      --win-purple: #8B5CF6;
      
      /* Typography */
      --win-font: 'Plus Jakarta Sans', 'Inter', 'Segoe UI', -apple-system, sans-serif;
      --win-mono: 'JetBrains Mono', 'Consolas', 'Courier New', monospace;
      
      /* Standardized Restrained Enterprise Shadows */
      --win-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.2);
      --win-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.3);
      --win-shadow-modal: 0 8px 24px rgba(0, 0, 0, 0.45);
    }

    /* Professional Desktop Light Mode (Office Daylight Accounting Style) */
    body[data-theme="light"] {
      --win-bg: #F8FAFC;              /* Slate 50 Daylight Canvas */
      --win-surface: #FFFFFF;         /* Pure White Surface Card */
      --win-surface-2: #F1F5F9;       /* Slate 100 Subtle Fill */
      --win-surface-3: #E2E8F0;       /* Slate 200 Raised Fill / Hover */
      --win-border: #E2E8F0;          /* Slate 200 Hairline Border */
      --win-border-light: #CBD5E1;    /* Slate 300 Strong Border */
      --win-text: #0F172A;            /* Slate 900 Primary Text */
      --win-text-muted: #64748B;      /* Slate 500 Secondary/Muted Text */
      
      /* Semantic Accounting Accents (Light Mode) */
      --win-accent: #D97706;          /* Solid Amber Action */
      --win-accent-hover: #B45309;    /* Amber 700 Hover */
      --win-green: #047857;           /* Emerald 700 Credit Text */
      --win-green-bg: #ECFDF5;        /* Mint 50 Credit Container */
      --win-green-border: #059669;
      --win-red: #B91C1C;             /* Red 700 Debit Text */
      --win-red-bg: #FEF2F2;          /* Rose 50 Debit Container */
      --win-red-border: #DC2626;
      --win-blue: #1D4ED8;            /* Blue 700 Transfer Text */
      --win-blue-bg: #EFF6FF;         /* Blue 50 Transfer Container */
      --win-blue-border: #2563EB;
      
      /* Light Shadows */
      --win-shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.04);
      --win-shadow-card: 0 1px 3px rgba(0, 0, 0, 0.06);
      --win-shadow-modal: 0 8px 24px rgba(0, 0, 0, 0.12);
    }
```

---

### Section B: Removal of `@keyframes pulseGlow` and Blur Elements

#### 1. Delete `@keyframes pulseGlow` in CSS
- **Target Location**: `E:\CashBookRelease\app\www\index.html`: Lines 20–23 (`C:\...`: Lines 20–23)
- **Current Code**:
  ```css
  @keyframes pulseGlow {
    0% { transform: scale(0.95); opacity: 0.5; }
    100% { transform: scale(1.08); opacity: 0.9; }
  }
  ```
- **Action**: Delete completely.

#### 2. Remove Blurry Glowing Aura Div in Startup Screen
- **Target Location**: `E:\CashBookRelease\app\www\index.html`: Lines 1274–1277 (`C:\...`: Lines 1226–1229)
- **Current Code**:
  ```html
  <div style="position: relative; width: 90px; height: 90px; display: flex; align-items: center; justify-content: center;">
    <div style="position: absolute; inset: -10px; border-radius: 26px; background: linear-gradient(135deg, rgba(245, 158, 11, 0.4), rgba(30, 58, 138, 0.5)); filter: blur(16px); animation: pulseGlow 1.8s infinite alternate;"></div>
    <img src="app_logo.png" onerror="this.src='logo_concept3.png'" style="position: relative; width: 80px; height: 80px; border-radius: 20px; border: 2px solid #f59e0b; box-shadow: 0 10px 25px rgba(0,0,0,0.6); object-fit: cover;">
  </div>
  ```
- **Replacement Drop-In**:
  ```html
  <div style="position: relative; width: 80px; height: 80px; display: flex; align-items: center; justify-content: center;">
    <img src="app_logo.png" onerror="this.src='logo_concept3.png'" style="position: relative; width: 80px; height: 80px; border-radius: 8px; border: 1px solid var(--win-border-light, #334155); box-shadow: var(--win-shadow-card, 0 1px 3px rgba(0,0,0,0.3)); object-fit: cover;">
  </div>
  ```
- **Impact**: Eradicates the AI glowing halo, reduces border radius from 20px/26px to 8px, and eliminates `filter: blur(16px)` completely.

#### 3. Eliminate Backdrop Blurs on Modal Overlays
- **Line 950**: In `.modal-overlay`, change `backdrop-filter: blur(3px);` to `backdrop-filter: none;`
- **Line 2293**: In `#modal-login-gate`, remove `backdrop-filter: blur(12px);`
- **Line 2497**: In `#modal-error-diagnostic`, remove `backdrop-filter: blur(10px);`
- **Line 2561**: In `#modal-diagnostic-history`, remove `backdrop-filter: blur(8px);`

---

### Section C: Neon Glow on `.sync-dot`

#### Target Location:
- `E:\CashBookRelease\app\www\index.html`: Lines 365–381
- `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`: Lines 317–333

#### Current Code:
```css
    .topbar-sync-status {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      color: var(--win-green);
      background: var(--win-green-bg);
      padding: 3px 9px;
      border-radius: 12px;
      border: 1px solid rgba(16, 185, 129, 0.25);
      cursor: pointer;
    }
    .sync-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--win-green);
      box-shadow: 0 0 8px var(--win-green);
    }
```

#### Replacement Drop-In:
```css
    .topbar-sync-status {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 11px;
      font-weight: 600;
      color: var(--win-green);
      background: var(--win-green-bg);
      padding: 3px 9px;
      border-radius: 4px;
      border: 1px solid var(--win-green-border, rgba(16, 185, 129, 0.3));
      cursor: pointer;
    }
    .sync-dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      background: var(--win-green);
      box-shadow: none;
      display: inline-block;
      flex-shrink: 0;
    }
```

---

### Section D: Saturated Gradients on Buttons, Ribbons, and Headers

#### 1. `.sidebar-new-voucher-btn`
- **Target Location**: `E:\CashBookRelease\app\www\index.html`: Lines 529–555 (`C:\...`: Lines 481–507)
- **Current Code**:
  ```css
  .sidebar-new-voucher-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 9px 12px;
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    color: #0f172a;
    font-weight: 700;
    font-size: 13px;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
    transition: all 0.15s ease;
  }
  .sidebar-new-voucher-btn:hover {
    background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(245, 158, 11, 0.35);
  }
  body[data-theme="light"] .sidebar-new-voucher-btn {
    background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(217, 119, 6, 0.25);
  }
  ```
- **Replacement Drop-In**:
  ```css
  .sidebar-new-voucher-btn {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 9px 12px;
    background: var(--win-accent);
    color: #ffffff;
    font-weight: 700;
    font-size: 13px;
    border: 1px solid var(--win-accent-hover, #b45309);
    border-radius: 6px;
    cursor: pointer;
    box-shadow: var(--win-shadow-sm, 0 1px 2px rgba(0, 0, 0, 0.1));
    transition: background-color 0.15s ease, border-color 0.15s ease;
  }
  .sidebar-new-voucher-btn:hover {
    background: var(--win-accent-hover);
    border-color: #92400e;
    transform: none;
    box-shadow: var(--win-shadow-card, 0 1px 3px rgba(0, 0, 0, 0.15));
  }
  body[data-theme="light"] .sidebar-new-voucher-btn {
    background: #D97706;
    color: #ffffff;
    border: 1px solid #B45309;
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }
  body[data-theme="light"] .sidebar-new-voucher-btn:hover {
    background: #B45309;
    border-color: #92400e;
  }
  ```

#### 2. Action Ribbon Gradients
- **Target Location**:
  - `E:\...:120`: `body[data-theme="light"] .action-ribbon { background: linear-gradient(180deg, #ffffff 0%, #f1f5f9 100%); ... }`
  - `E:\...:418`: `.action-ribbon { background: linear-gradient(180deg, #131c2b 0%, #0e1624 100%); ... }`
- **Replacement Drop-In**:
  - Line 120: `body[data-theme="light"] .action-ribbon { background: #F1F5F9; border-bottom: 1px solid #CBD5E1; }`
  - Line 418: `.action-ribbon { background: #111A28; border-bottom: 1px solid var(--win-border); ... }`

#### 3. Nav Link Active Gradient
- **Target Location**: `E:\...:627` (`C:\...:579`)
- **Current Code**:
  ```css
  .nav-link.active {
    background: linear-gradient(90deg, rgba(245, 158, 11, 0.2) 0%, rgba(245, 158, 11, 0.05) 100%);
    color: #fbbf24;
    border-left: 3px solid var(--win-accent);
    font-weight: 600;
  }
  ```
- **Replacement Drop-In**:
  ```css
  .nav-link.active {
    background: var(--win-surface-3);
    color: var(--win-accent);
    border-left: 3px solid var(--win-accent);
    font-weight: 600;
  }
  ```

#### 4. Toast Notification Gradients
- **Target Location**: `E:\...:56–71`
- **Current Code**: 135deg linear gradients with low-contrast tint.
- **Replacement Drop-In**:
  ```css
  .toast-msg.toast-error {
    border-left: 4px solid var(--win-red);
    background: var(--win-surface);
  }
  .toast-msg.toast-warning {
    border-left: 4px solid var(--win-accent);
    background: var(--win-surface);
  }
  .toast-msg.toast-success {
    border-left: 4px solid var(--win-green);
    background: var(--win-surface);
  }
  .toast-msg.toast-info {
    border-left: 4px solid var(--win-blue);
    background: var(--win-surface);
  }
  ```

#### 5. Modal Header Gradients
- **Line 2341 (`#google-user-modal`)**:
  - *Current*: `style="background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); color: #ffffff !important; padding: 16px 20px; ..."`
  - *Replace with*: `style="background: var(--win-surface-2); color: var(--win-text); padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--win-border);"`
- **Line 2403 (`#mobile-qr-modal`)**:
  - *Current*: `style="background: linear-gradient(135deg, #1e3a5f 0%, #0f172a 100%); color: #ffffff !important; padding: 16px 22px; ..."`
  - *Replace with*: `style="background: var(--win-surface-2); color: var(--win-text); padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--win-border);"`
- **Line 2501 (`#modal-error-diagnostic`)**:
  - *Current*: `style="background: linear-gradient(135deg, #7f1d1d 0%, #450a0a 100%); color: #ffffff !important; padding: 16px 20px; ..."`
  - *Replace with*: `style="background: var(--win-surface-2); color: var(--win-text); padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid var(--win-red);"`

---

### Section E: Standardizing Diffuse Shadows & Cartoonish Radii

#### 1. Diffuse Shadows on Cards & Panels
- **`.kpi-card` (Line 689)**:
  - *Current*: `box-shadow: 0 4px 12px rgba(0,0,0,0.25);`
  - *Replace with*: `box-shadow: var(--win-shadow-card, 0 1px 3px rgba(0,0,0,0.12));`
- **`.chart-panel` (Line 736)**:
  - *Current*: `box-shadow: 0 4px 12px rgba(0,0,0,0.25);`
  - *Replace with*: `box-shadow: var(--win-shadow-card, 0 1px 3px rgba(0,0,0,0.12));`
- **`.table-panel` (Line 789)**:
  - *Current*: `box-shadow: 0 4px 12px rgba(0,0,0,0.25);`
  - *Replace with*: `box-shadow: var(--win-shadow-card, 0 1px 3px rgba(0,0,0,0.12));`
- **`.brand-logo-img` (Lines 328–329)**:
  - *Current*: `box-shadow: 0 2px 8px rgba(245, 158, 11, 0.35); border: 1px solid rgba(245, 158, 11, 0.5);`
  - *Replace with*: `border: 1px solid var(--win-border-light); box-shadow: none;`
- **`.toast-msg` (Line 49)**:
  - *Current*: `box-shadow: 0 14px 32px rgba(0,0,0,0.5);`
  - *Replace with*: `box-shadow: var(--win-shadow-modal, 0 4px 12px rgba(0,0,0,0.25));`
- **`.modal-dialog` (Line 960)**:
  - *Current*: `box-shadow: 0 16px 40px rgba(0,0,0,0.6);`
  - *Replace with*: `box-shadow: var(--win-shadow-modal, 0 8px 24px rgba(0,0,0,0.35));`
- **`body[data-theme="light"] .modal-dialog` (Line 131)**:
  - *Current*: `box-shadow: 0 16px 40px rgba(0, 0, 0, 0.12);`
  - *Replace with*: `box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);`

#### 2. Cartoonish Border Radii (16px–26px) Standardization
- **Line 2294 (`.login-card`)**:
  - *Current*: `border-radius: 16px; padding: 32px 28px; box-shadow: 0 25px 60px rgba(0,0,0,0.6);`
  - *Replace with*: `border-radius: 8px; padding: 28px 24px; box-shadow: var(--win-shadow-modal, 0 8px 24px rgba(0,0,0,0.35));`
- **Line 2298 (`.login-card` logo)**:
  - *Current*: `border-radius: 18px; border: 2px solid #f59e0b; box-shadow: 0 6px 18px rgba(245,158,11,0.3);`
  - *Replace with*: `border-radius: 8px; border: 1px solid var(--win-border-light); box-shadow: none;`
- **Line 2498 (`#modal-error-diagnostic .modal-dialog`)**:
  - *Current*: `border-radius: 14px; ... box-shadow: 0 25px 60px rgba(239, 68, 68, 0.25);`
  - *Replace with*: `border-radius: 8px; ... box-shadow: var(--win-shadow-modal, 0 8px 24px rgba(0,0,0,0.35));`
- **Lines 2002, 2340, 2402, 2562 (Modal Dialogs inline)**:
  - Change all inline `border-radius: 12px; box-shadow: 0 24px 48px rgba(0,0,0,0.5);` to `border-radius: 8px; box-shadow: var(--win-shadow-modal, 0 8px 24px rgba(0,0,0,0.35));`
- **Line 1109 (`.desc-tag`)**: Change `border-radius: 12px;` to `border-radius: 4px;`
- **Line 1558 (`#filter-count-badge`)**: Change `border-radius: 10px;` to `border-radius: 4px;`

---

### Section F: High-Precision Tabular Numbers & Ledger Typography

#### Target Location:
- `E:\CashBookRelease\app\www\index.html`: Lines 880–905
- `C:\Users\WIN\AppData\Local\Programs\CashBookPro\www\index.html`: Lines 835–860

#### Current Code:
```css
    .amount-in { color: var(--win-green); font-family: var(--win-mono); font-weight: 700; text-align: right; }
    .amount-out { color: var(--win-red); font-family: var(--win-mono); font-weight: 700; text-align: right; }
    .amount-balance { color: #f1f5f9; font-family: var(--win-mono); font-weight: 700; text-align: right; }
```

#### Defects:
1. No `font-variant-numeric: tabular-nums` or `font-feature-settings: "tnum" 1, "zero" 1;`.
2. `.amount-balance` has hardcoded `#f1f5f9` (invisible in daylight mode).
3. Table cell padding and right-alignment are inconsistent across dynamically rendered cells.

#### Replacement Drop-In:
```css
    /* High-Precision Tabular Financial Figures */
    .amount-in,
    .amount-out,
    .amount-balance,
    .kpi-value,
    .mono-amount,
    .badge-voucher,
    .data-table td.amount-in,
    .data-table td.amount-out,
    .data-table td.amount-balance {
      font-family: var(--win-mono);
      font-variant-numeric: tabular-nums;
      font-feature-settings: "tnum" 1, "zero" 1;
      text-align: right;
    }

    .amount-in { 
      color: var(--win-green); 
      font-weight: 700; 
    }
    .amount-out { 
      color: var(--win-red); 
      font-weight: 700; 
    }
    .amount-balance { 
      color: var(--win-text); 
      font-weight: 700; 
    }

    /* Strict Right-Alignment for Table Amount Columns */
    .data-table th:nth-child(7),
    .data-table th:nth-child(8),
    .data-table th:nth-child(9),
    .data-table td:nth-child(7),
    .data-table td:nth-child(8),
    .data-table td:nth-child(9) {
      text-align: right;
    }
```

---

## 4. Verification Checkpoints for Implementation Worker

When the Worker executes this plan, verify against these exact commands:

1. **Pulse Glow Verification**:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "pulseGlow"
   ```
   *Expected output*: 0 hits.

2. **Filter Blur Verification**:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "filter:\s*blur"
   ```
   *Expected output*: 0 hits.

3. **Neon Dot Glow Verification**:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "box-shadow:.*var\(--win-green\)"
   ```
   *Expected output*: 0 hits.

4. **Saturated Gradient on Voucher Button Verification**:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "\.sidebar-new-voucher-btn" -Context 0, 5
   ```
   *Expected output*: Uses `var(--win-accent)` with solid `#D97706`, no `linear-gradient`.

5. **Tabular Numerals Verification**:
   ```powershell
   Select-String -Path "E:\CashBookRelease\app\www\index.html" -Pattern "font-variant-numeric:\s*tabular-nums"
   ```
   *Expected output*: At least 1 hit applying to all financial figure classes.

---

## 5. Scope Boundary Note for Downstream Milestones

- **Milestone 1 Scope**: Strictly visual styling, CSS variable modernization, removal of anti-pattern halos/gradients/shadows, and tabular typography.
- **Milestone 2 Scope**: Desktop Google Sign-In loopback/virtual host flow and true bidirectional Firestore sync (`syncTransactionToFirestore()` and `firestore.rules` 12-key whitelist). The Worker in Milestone 1 must NOT alter the JavaScript auth or sync methods.
