# CardDemo Copybook Data Dictionary

**Project:** AWS Mainframe Modernization — CardDemo  
**Methodology:** Equivalence-First Migration  
**Generated:** 2026-06-23  
**Source:** `app/cpy/` — all 30 copybooks  
**Context:** FD sections from `CBACT04C.cbl` and `CBTRN02C.cbl` used for cross-reference

---

## Overview

This dictionary covers every copybook in `app/cpy/`. Copybooks are organized into five functional groups:

| Group                    | Prefix                   | Purpose                                                                      |
| :----------------------- | :----------------------- | :--------------------------------------------------------------------------- |
| Core VSAM File Records   | `CV`                     | Physical VSAM KSDS record layouts (accounts, cards, customers, transactions) |
| Export / Import          | `CV`                     | Multi-record export file layout with REDEFINES-based type dispatch           |
| Online / CICS Structures | `CO`, `CVCRD`            | CICS communication areas, menu tables, screen work areas                     |
| Common Shared Utilities  | `CS`, `CSDAT`, `CUSTREC` | Working storage fragments, messages, security records, date utilities        |
| Non-Data Copybooks       | `CS*`                    | Procedure Division code snippets and validation lookup tables                |

### Migration Gotchas — Read Before Translating

1. **Overpunch sign encoding**: All `PIC S9(n) DISPLAY` fields store the sign as an EBCDIC zone overpunch in the last digit byte. A normalizer must decode this before any arithmetic comparison.
2. **Implied decimal**: `V` in a PIC clause is a logical (implied) decimal point — there is no physical separator byte. `PIC S9(10)V99` has 12 bytes of digits with the decimal point logically between bytes 10 and 11.
3. **COMP-3 nibble packing**: Packed decimal fields store two BCD digits per byte, with the last nibble being the sign (C = positive, D = negative, F = unsigned). Never treat COMP-3 bytes as characters.
4. **COMP / BINARY byte widths** (IBM mainframe, big-endian): PIC 9(1–4) → 2 bytes; PIC 9(5–9) → 4 bytes; PIC 9(10–18) → 8 bytes. Signed variants follow the same rule.
5. **REDEFINES do not allocate additional storage**: A REDEFINES item overlays the same bytes as its target. Offsets for a REDEFINES entry are identical to the field it redefines.
6. **Typo in source**: `EXPIRAION` (missing 'I') appears consistently in `CVACT01Y`, `CVACT02Y`, and `CVEXPORT`. Do not correct this during translation — field-name matching in the harness requires exact spelling.
7. **CUSTREC vs CVCUS01Y**: Both define `CUSTOMER-RECORD`. They cannot be COPYed into the same compilation unit. The only difference is the date field name (`CUST-DOB-YYYYMMDD` vs `CUST-DOB-YYYY-MM-DD`). Programs use one or the other.
8. **UNUSED1Y is dead code**: The copybook is never COPYed by any active program. It mirrors `CSUSR01Y` structurally but is not equivalent.

---

## Section Index

- [CVACT01Y](#cvact01y) — Account Record (300 bytes)
- [CVACT02Y](#cvact02y) — Card Record (150 bytes)
- [CVACT03Y](#cvact03y) — Card Cross-Reference Record (50 bytes)
- [CVCUS01Y](#cvcus01y) — Customer Record (500 bytes)
- [CVTRA01Y](#cvtra01y) — Transaction Category Balance Record (50 bytes)
- [CVTRA02Y](#cvtra02y) — Disclosure Group / Interest Rate Record (50 bytes)
- [CVTRA03Y](#cvtra03y) — Transaction Type Record (60 bytes)
- [CVTRA04Y](#cvtra04y) — Transaction Category Record (60 bytes)
- [CVTRA05Y](#cvtra05y) — Transaction Record (350 bytes)
- [CVTRA06Y](#cvtra06y) — Daily Transaction Record (350 bytes)
- [CVTRA07Y](#cvtra07y) — Transaction Report Print Structures
- [COSTM01](#costm01) — Statement Transaction Record (350 bytes)
- [CVEXPORT](#cvexport) — Multi-Type Export Record (500 bytes)
- [COCOM01Y](#cocom01y) — CICS Communication Area (160 bytes)
- [COADM02Y](#coadm02y) — Admin Menu Options Table
- [COMEN02Y](#comen02y) — Main Menu Options Table
- [CVCRD01Y](#cvcrd01y) — Card / CICS Work Areas (213 bytes)
- [CODATECN](#codatecn) — Date Conversion Record (80 bytes)
- [COTTL01Y](#cottl01y) — Screen Title Constants (120 bytes)
- [CSDAT01Y](#csdat01y) — Date / Time Working Storage (58 bytes)
- [CSMSG01Y](#csmsg01y) — Common Screen Messages (100 bytes)
- [CSMSG02Y](#csmsg02y) — Abend Error Data (134 bytes)
- [CSUSR01Y](#csusr01y) — Security User Record (80 bytes)
- [CSUTLDWY](#csutldwy) — Date Edit Working Storage (fragment)
- [CUSTREC](#custrec) — Customer Record Variant (500 bytes)
- [CSLKPCDY](#cslkpcdy) — Validation Lookup Tables (code-only)
- [CSSETATY](#cssetaty) — CICS AID Key Setter (Procedure Division snippet)
- [CSSTRPFY](#csstrpfy) — Screen Field Highlighter (Procedure Division snippet)
- [CSUTLDPY](#csutldpy) — Date Edit Procedures (Procedure Division snippet)
- [UNUSED1Y](#unused1y) — Unused Data (dead code, 80 bytes)

---

## CVACT01Y

**File:** `app/cpy/CVACT01Y.cpy`  
**Record:** `ACCOUNT-RECORD`  
**Record Length:** 300 bytes  
**VSAM Type:** KSDS — primary key is `ACCT-ID`  
**DD Name:** `ACCTFILE`  
**Used By:** `CBACT04C.cbl`, `CBTRN02C.cbl`, `COACTUPC.cbl`, `COACTVWC.cbl`

This is the master account record. One record exists per credit card account. Financial fields (`CURR-BAL`, `CREDIT-LIMIT`, etc.) are signed display numerics stored with EBCDIC overpunch — they must be decoded before any arithmetic. The cycle credit/debit fields accumulate postings during the current billing cycle and are reset at statement cut.

> **Typo note:** `ACCT-EXPIRAION-DATE` is a deliberate transcription of the source — do not rename during migration.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name             | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                                      |
| :--------------------- | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :------------------------------------------------------------------------------------ |
| ACCOUNT-RECORD         | 01      | 0        | 300      | —         | —       | —                     | Root of the account master record                                                     |
| ACCT-ID                | 05      | 0        | 11       | 9(11)     | DISPLAY | none                  | Unique account identifier; primary VSAM key                                           |
| ACCT-ACTIVE-STATUS     | 05      | 11       | 1        | X(01)     | DISPLAY | trailing-space-strip  | Account status flag; 'Y' = active, 'N' = inactive                                     |
| ACCT-CURR-BAL          | 05      | 12       | 12       | S9(10)V99 | DISPLAY | overpunch-sign-decode | Current outstanding balance; 10 integer + 2 decimal digits; negative = credit balance |
| ACCT-CREDIT-LIMIT      | 05      | 24       | 12       | S9(10)V99 | DISPLAY | overpunch-sign-decode | Maximum credit line approved for this account                                         |
| ACCT-CASH-CREDIT-LIMIT | 05      | 36       | 12       | S9(10)V99 | DISPLAY | overpunch-sign-decode | Maximum cash advance sub-limit within the overall credit limit                        |
| ACCT-OPEN-DATE         | 05      | 48       | 10       | X(10)     | DISPLAY | date-parse            | Date account was opened; format YYYY-MM-DD                                            |
| ACCT-EXPIRAION-DATE    | 05      | 58       | 10       | X(10)     | DISPLAY | date-parse            | Date the account (not the card) expires; format YYYY-MM-DD; note spelling: EXPIRAION  |
| ACCT-REISSUE-DATE      | 05      | 68       | 10       | X(10)     | DISPLAY | date-parse            | Date a replacement card was last issued to this account; format YYYY-MM-DD            |
| ACCT-CURR-CYC-CREDIT   | 05      | 78       | 12       | S9(10)V99 | DISPLAY | overpunch-sign-decode | Total credits (payments, returns) posted in the current billing cycle                 |
| ACCT-CURR-CYC-DEBIT    | 05      | 90       | 12       | S9(10)V99 | DISPLAY | overpunch-sign-decode | Total debits (purchases, fees) posted in the current billing cycle                    |
| ACCT-ADDR-ZIP          | 05      | 102      | 10       | X(10)     | DISPLAY | trailing-space-strip  | ZIP or postal code associated with the billing address on file                        |
| ACCT-GROUP-ID          | 05      | 112      | 10       | X(10)     | DISPLAY | trailing-space-strip  | Disclosure group identifier; links account to the interest rate schedule in CVTRA02Y. **Migration note:** This value is read from the account record already in working storage — it is NOT retrieved from XREF-FILE. `CBACT04C` uses it as the first key segment when reading DISCGRP-FILE directly. File status 23 (record not found) triggers a fallback to the literal `'DEFAULT'` group; this is intentional business logic, not a defect. |
| FILLER                 | 05      | 122      | 178      | X(178)    | DISPLAY | none                  | Reserved / padding to 300-byte record length                                          |

```json
{
  "copybook": "CVACT01Y",
  "record": "ACCOUNT-RECORD",
  "total_length": 300,
  "fields": [
    {
      "name": "ACCT-ID",
      "level": "05",
      "offset": 0,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Unique account identifier; primary VSAM key for ACCTFILE"
    },
    {
      "name": "ACCT-ACTIVE-STATUS",
      "level": "05",
      "offset": 11,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Account status flag: 'Y' = active, 'N' = closed/inactive"
    },
    {
      "name": "ACCT-CURR-BAL",
      "level": "05",
      "offset": 12,
      "length": 12,
      "pic": "S9(10)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Current outstanding balance on the account; negative value means credit balance (customer overpaid)"
    },
    {
      "name": "ACCT-CREDIT-LIMIT",
      "level": "05",
      "offset": 24,
      "length": 12,
      "pic": "S9(10)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Maximum total credit line approved for this account"
    },
    {
      "name": "ACCT-CASH-CREDIT-LIMIT",
      "level": "05",
      "offset": 36,
      "length": 12,
      "pic": "S9(10)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Cash advance sub-limit; must not exceed ACCT-CREDIT-LIMIT"
    },
    {
      "name": "ACCT-OPEN-DATE",
      "level": "05",
      "offset": 48,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Date account was opened; format YYYY-MM-DD with hyphens"
    },
    {
      "name": "ACCT-EXPIRAION-DATE",
      "level": "05",
      "offset": 58,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Account expiration date; format YYYY-MM-DD; field name contains deliberate typo (EXPIRAION not EXPIRATION) — do not correct"
    },
    {
      "name": "ACCT-REISSUE-DATE",
      "level": "05",
      "offset": 68,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Date replacement card was last issued; format YYYY-MM-DD"
    },
    {
      "name": "ACCT-CURR-CYC-CREDIT",
      "level": "05",
      "offset": 78,
      "length": 12,
      "pic": "S9(10)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Running total of credits (payments, refunds) in the current billing cycle; reset at statement cut"
    },
    {
      "name": "ACCT-CURR-CYC-DEBIT",
      "level": "05",
      "offset": 90,
      "length": 12,
      "pic": "S9(10)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Running total of debits (purchases, fees, interest) in the current billing cycle; reset at statement cut"
    },
    {
      "name": "ACCT-ADDR-ZIP",
      "level": "05",
      "offset": 102,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Billing address ZIP or postal code (up to 10 chars to accommodate extended ZIP+4 or Canadian postal codes)"
    },
    {
      "name": "ACCT-GROUP-ID",
      "level": "05",
      "offset": 112,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Disclosure group code linking this account to interest rate tiers in CVTRA02Y (DIS-GROUP-RECORD); value 'DEFAULT' used as fallback when no specific group matches. Migration note: read from working storage (account record already loaded) — NOT from XREF-FILE. CBACT04C uses this as the first DISCGRP-FILE key segment; file status 23 (not found) falls back to 'DEFAULT' — intentional business logic, not a defect."
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 122,
      "length": 178,
      "pic": "X(178)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding; brings record to 300-byte fixed length"
    }
  ]
}
```

---

## CVACT02Y

**File:** `app/cpy/CVACT02Y.cpy`  
**Record:** `CARD-RECORD`  
**Record Length:** 150 bytes  
**VSAM Type:** KSDS — primary key is `CARD-NUM`  
**DD Name:** `CARDFILE`  
**Used By:** `COCRDLIC.cbl`, `COCRDSLC.cbl`, `COCRDUPC.cbl`

One record per physical credit card. A single account may have multiple card records (e.g., primary + authorized users). Linked to the account via `CARD-ACCT-ID`. The card number is stored as alphanumeric (PIC X) to preserve leading zeros and handle non-numeric card formats.

> **Typo note:** `CARD-EXPIRAION-DATE` — same deliberate misspelling as in CVACT01Y.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name          | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                             |
| :------------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :--------------------------------------------------------------------------- |
| CARD-RECORD         | 01      | 0        | 150      | —     | —       | —                    | Root of the card master record                                               |
| CARD-NUM            | 05      | 0        | 16       | X(16) | DISPLAY | trailing-space-strip | 16-digit card number (PAN); stored as alphanumeric to preserve leading zeros |
| CARD-ACCT-ID        | 05      | 16       | 11       | 9(11) | DISPLAY | none                 | Account ID this card belongs to; foreign key to CVACT01Y.ACCT-ID             |
| CARD-CVV-CD         | 05      | 27       | 3        | 9(03) | DISPLAY | none                 | Card Verification Value (CVV/CVC) — 3-digit security code                    |
| CARD-EMBOSSED-NAME  | 05      | 30       | 50       | X(50) | DISPLAY | trailing-space-strip | Cardholder name as it appears embossed on the card                           |
| CARD-EXPIRAION-DATE | 05      | 80       | 10       | X(10) | DISPLAY | date-parse           | Card expiry date; format YYYY-MM-DD; note spelling: EXPIRAION                |
| CARD-ACTIVE-STATUS  | 05      | 90       | 1        | X(01) | DISPLAY | trailing-space-strip | Card status flag: 'Y' = active, 'N' = inactive/cancelled                     |
| FILLER              | 05      | 91       | 59       | X(59) | DISPLAY | none                 | Reserved padding to 150-byte record length                                   |

```json
{
  "copybook": "CVACT02Y",
  "record": "CARD-RECORD",
  "total_length": 150,
  "fields": [
    {
      "name": "CARD-NUM",
      "level": "05",
      "offset": 0,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "16-character Primary Account Number (PAN); stored as alphanumeric; primary VSAM key for CARDFILE"
    },
    {
      "name": "CARD-ACCT-ID",
      "level": "05",
      "offset": 16,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Account identifier linking this card to its parent account (foreign key to CVACT01Y.ACCT-ID)"
    },
    {
      "name": "CARD-CVV-CD",
      "level": "05",
      "offset": 27,
      "length": 3,
      "pic": "9(03)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "3-digit Card Verification Value security code printed on the card"
    },
    {
      "name": "CARD-EMBOSSED-NAME",
      "level": "05",
      "offset": 30,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Cardholder name as physically embossed on the card; may differ from legal name on CVCUS01Y"
    },
    {
      "name": "CARD-EXPIRAION-DATE",
      "level": "05",
      "offset": 80,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Card expiration date in YYYY-MM-DD format; field name contains deliberate typo EXPIRAION"
    },
    {
      "name": "CARD-ACTIVE-STATUS",
      "level": "05",
      "offset": 90,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Card active flag: 'Y' = active and usable, 'N' = cancelled/blocked"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 91,
      "length": 59,
      "pic": "X(59)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 150-byte fixed record length"
    }
  ]
}
```

---

## CVACT03Y

**File:** `app/cpy/CVACT03Y.cpy`  
**Record:** `CARD-XREF-RECORD`  
**Record Length:** 50 bytes  
**VSAM Type:** KSDS — primary key `XREF-CARD-NUM`; alternate key `XREF-ACCT-ID` (per FD in CBACT04C)  
**DD Name:** `XREFFILE`  
**Used By:** `CBACT04C.cbl`, `CBTRN02C.cbl`

Cross-reference file mapping card numbers to their owning customer and account. Used by batch programs to resolve a card number to an account ID for transaction posting. This three-way join (card → customer → account) allows navigation from any of the three entities.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name       | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                      |
| :--------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :-------------------------------------------------------------------- |
| CARD-XREF-RECORD | 01      | 0        | 50       | —     | —       | —                    | Root of the card cross-reference record                               |
| XREF-CARD-NUM    | 05      | 0        | 16       | X(16) | DISPLAY | trailing-space-strip | Card number (PAN); primary VSAM key; foreign key to CVACT02Y.CARD-NUM |
| XREF-CUST-ID     | 05      | 16       | 9        | 9(09) | DISPLAY | none                 | Customer ID of the cardholder; foreign key to CVCUS01Y.CUST-ID        |
| XREF-ACCT-ID     | 05      | 25       | 11       | 9(11) | DISPLAY | none                 | Account ID; alternate VSAM key; foreign key to CVACT01Y.ACCT-ID       |
| FILLER           | 05      | 36       | 14       | X(14) | DISPLAY | none                 | Reserved padding to 50-byte record length                             |

```json
{
  "copybook": "CVACT03Y",
  "record": "CARD-XREF-RECORD",
  "total_length": 50,
  "fields": [
    {
      "name": "XREF-CARD-NUM",
      "level": "05",
      "offset": 0,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Card number (PAN); primary VSAM key for XREFFILE; foreign key to CARD-RECORD"
    },
    {
      "name": "XREF-CUST-ID",
      "level": "05",
      "offset": 16,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Customer ID of the cardholder; links card to the customer master (CVCUS01Y)"
    },
    {
      "name": "XREF-ACCT-ID",
      "level": "05",
      "offset": 25,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Account ID; alternate VSAM key enabling lookup by account to find all cards"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 36,
      "length": 14,
      "pic": "X(14)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 50-byte fixed record length"
    }
  ]
}
```

---

## CVCUS01Y

**File:** `app/cpy/CVCUS01Y.cpy`  
**Record:** `CUSTOMER-RECORD`  
**Record Length:** 500 bytes  
**VSAM Type:** KSDS — primary key `CUST-ID`  
**DD Name:** `CUSTFILE`  
**Used By:** `CBCUS01C.cbl`, `COACTVWC.cbl`

Master customer record. Stores personal identity information (name, address, SSN, DOB), contact information, and a credit assessment score. The SSN is stored as plain numeric display — the Veritas harness must treat it as PII and never log it. The FICO score (3 digits) is unsigned display.

> **Variant:** `CUSTREC.cpy` defines an identical structure under the same record name with a slightly different date field name. See the [CUSTREC](#custrec) section.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name               | Level   | Offset   | Length   | PIC    | USAGE   | Normalization Rule   | Business Meaning                                                                   |
| :----------------------- | :------ | -------: | -------: | :----- | :------ | :------------------- | :--------------------------------------------------------------------------------- |
| CUSTOMER-RECORD          | 01      | 0        | 500      | —      | —       | —                    | Root of the customer master record                                                 |
| CUST-ID                  | 05      | 0        | 9        | 9(09)  | DISPLAY | none                 | Unique customer identifier; primary VSAM key                                       |
| CUST-FIRST-NAME          | 05      | 9        | 25       | X(25)  | DISPLAY | trailing-space-strip | Customer given name; space-padded to 25 bytes                                      |
| CUST-MIDDLE-NAME         | 05      | 34       | 25       | X(25)  | DISPLAY | trailing-space-strip | Customer middle name or initial; spaces if absent                                  |
| CUST-LAST-NAME           | 05      | 59       | 25       | X(25)  | DISPLAY | trailing-space-strip | Customer family name; space-padded to 25 bytes                                     |
| CUST-ADDR-LINE-1         | 05      | 84       | 50       | X(50)  | DISPLAY | trailing-space-strip | Mailing address line 1 (street number and name)                                    |
| CUST-ADDR-LINE-2         | 05      | 134      | 50       | X(50)  | DISPLAY | trailing-space-strip | Mailing address line 2 (apartment, suite, etc.)                                    |
| CUST-ADDR-LINE-3         | 05      | 184      | 50       | X(50)  | DISPLAY | trailing-space-strip | Mailing address line 3 (city, state, for international)                            |
| CUST-ADDR-STATE-CD       | 05      | 234      | 2        | X(02)  | DISPLAY | trailing-space-strip | US state or territory code (2-char USPS abbreviation); validated against CSLKPCDY  |
| CUST-ADDR-COUNTRY-CD     | 05      | 236      | 3        | X(03)  | DISPLAY | trailing-space-strip | ISO 3166-1 alpha-3 country code                                                    |
| CUST-ADDR-ZIP            | 05      | 239      | 10       | X(10)  | DISPLAY | trailing-space-strip | ZIP or postal code; up to 10 chars for ZIP+4 format; validated against CSLKPCDY    |
| CUST-PHONE-NUM-1         | 05      | 249      | 15       | X(15)  | DISPLAY | trailing-space-strip | Primary phone number including area code; area code validated against CSLKPCDY     |
| CUST-PHONE-NUM-2         | 05      | 264      | 15       | X(15)  | DISPLAY | trailing-space-strip | Secondary / alternate phone number                                                 |
| CUST-SSN                 | 05      | 279      | 9        | 9(09)  | DISPLAY | none                 | Social Security Number (9 digits, no dashes); PII — never log or expose in reports |
| CUST-GOVT-ISSUED-ID      | 05      | 288      | 20       | X(20)  | DISPLAY | trailing-space-strip | Government-issued ID other than SSN (e.g., passport, driver's license)             |
| CUST-DOB-YYYY-MM-DD      | 05      | 308      | 10       | X(10)  | DISPLAY | date-parse           | Customer date of birth; format YYYY-MM-DD with hyphens; must be in the past        |
| CUST-EFT-ACCOUNT-ID      | 05      | 318      | 10       | X(10)  | DISPLAY | trailing-space-strip | Electronic Funds Transfer bank account ID for direct payment                       |
| CUST-PRI-CARD-HOLDER-IND | 05      | 328      | 1        | X(01)  | DISPLAY | trailing-space-strip | Primary cardholder indicator: 'Y' = this customer is the primary holder            |
| CUST-FICO-CREDIT-SCORE   | 05      | 329      | 3        | 9(03)  | DISPLAY | none                 | FICO credit score (300–850); used for credit limit decisions                       |
| FILLER                   | 05      | 332      | 168      | X(168) | DISPLAY | none                 | Reserved padding to 500-byte record length                                         |

```json
{
  "copybook": "CVCUS01Y",
  "record": "CUSTOMER-RECORD",
  "total_length": 500,
  "fields": [
    {
      "name": "CUST-ID",
      "level": "05",
      "offset": 0,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Unique customer identifier; primary VSAM key for CUSTFILE"
    },
    {
      "name": "CUST-FIRST-NAME",
      "level": "05",
      "offset": 9,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer given (first) name; space-padded to fixed 25 bytes"
    },
    {
      "name": "CUST-MIDDLE-NAME",
      "level": "05",
      "offset": 34,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer middle name or initial; all spaces if not provided"
    },
    {
      "name": "CUST-LAST-NAME",
      "level": "05",
      "offset": 59,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer family (last) name; space-padded to fixed 25 bytes"
    },
    {
      "name": "CUST-ADDR-LINE-1",
      "level": "05",
      "offset": 84,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Primary mailing address: street number and street name"
    },
    {
      "name": "CUST-ADDR-LINE-2",
      "level": "05",
      "offset": 134,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Secondary address line: apartment, suite, unit number; spaces if not applicable"
    },
    {
      "name": "CUST-ADDR-LINE-3",
      "level": "05",
      "offset": 184,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Tertiary address line: used for city/state in international addresses; spaces if not applicable"
    },
    {
      "name": "CUST-ADDR-STATE-CD",
      "level": "05",
      "offset": 234,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Two-letter US state or territory abbreviation (e.g., 'TX', 'CA'); validated against CSLKPCDY valid-state list"
    },
    {
      "name": "CUST-ADDR-COUNTRY-CD",
      "level": "05",
      "offset": 236,
      "length": 3,
      "pic": "X(03)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "ISO 3166-1 alpha-3 country code (e.g., 'USA', 'CAN')"
    },
    {
      "name": "CUST-ADDR-ZIP",
      "level": "05",
      "offset": 239,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "ZIP or postal code; first 5 digits validated against state code using CSLKPCDY state-zip combo table"
    },
    {
      "name": "CUST-PHONE-NUM-1",
      "level": "05",
      "offset": 249,
      "length": 15,
      "pic": "X(15)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Primary contact phone number; first 3 digits (area code) validated against CSLKPCDY NANP list; format unspecified (may include dashes)"
    },
    {
      "name": "CUST-PHONE-NUM-2",
      "level": "05",
      "offset": 264,
      "length": 15,
      "pic": "X(15)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Alternate contact phone number; same validation rules as CUST-PHONE-NUM-1"
    },
    {
      "name": "CUST-SSN",
      "level": "05",
      "offset": 279,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Social Security Number stored as 9 consecutive digits without dashes; PII — mask in logs and test data; used for identity verification"
    },
    {
      "name": "CUST-GOVT-ISSUED-ID",
      "level": "05",
      "offset": 288,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Government ID number other than SSN (passport, driver license, etc.); type not distinguished by a separate field"
    },
    {
      "name": "CUST-DOB-YYYY-MM-DD",
      "level": "05",
      "offset": 308,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Date of birth in YYYY-MM-DD format; business rule: must be in the past (validated by CSUTLDPY EDIT-DATE-OF-BIRTH)"
    },
    {
      "name": "CUST-EFT-ACCOUNT-ID",
      "level": "05",
      "offset": 318,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Bank account identifier for Electronic Funds Transfer payments; format is institution-specific"
    },
    {
      "name": "CUST-PRI-CARD-HOLDER-IND",
      "level": "05",
      "offset": 328,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Flag indicating this customer is the primary (not authorized) cardholder: 'Y' = primary, 'N' = authorized user"
    },
    {
      "name": "CUST-FICO-CREDIT-SCORE",
      "level": "05",
      "offset": 329,
      "length": 3,
      "pic": "9(03)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "FICO credit score from 300 (worst) to 850 (best); used in credit limit decisioning"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 332,
      "length": 168,
      "pic": "X(168)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding bringing record to 500-byte fixed length"
    }
  ]
}
```

---

## CVTRA01Y

**File:** `app/cpy/CVTRA01Y.cpy`  
**Record:** `TRAN-CAT-BAL-RECORD`  
**Record Length:** 50 bytes  
**VSAM Type:** KSDS — composite primary key (`TRANCAT-ACCT-ID` + `TRANCAT-TYPE-CD` + `TRANCAT-CD`)  
**DD Name:** `TCATBALF`  
**Used By:** `CBACT04C.cbl`, `CBTRN02C.cbl`

Accumulates transaction dollar balances per account, per transaction type, and per category. Used by `CBTRN02C` to build per-category totals during the daily transaction posting run, and by `CBACT04C` to apply interest rates (which are differentiated by category). One record per unique (account, type, category) combination.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name          | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                                             |
| :------------------ | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :------------------------------------------------------------------------------------------- |
| TRAN-CAT-BAL-RECORD | 01      | 0        | 50       | —         | —       | —                     | Root record for transaction category balance                                                 |
| TRAN-CAT-KEY        | 05      | 0        | 17       | —         | —       | —                     | Composite VSAM primary key (3 subfields)                                                     |
| TRANCAT-ACCT-ID     | 10      | 0        | 11       | 9(11)     | DISPLAY | none                  | Account ID; first component of composite key                                                 |
| TRANCAT-TYPE-CD     | 10      | 11       | 2        | X(02)     | DISPLAY | trailing-space-strip  | Transaction type code; e.g., 'PR' = purchase, 'CR' = credit; second key component            |
| TRANCAT-CD          | 10      | 13       | 4        | 9(04)     | DISPLAY | none                  | Transaction category code (4-digit); third key component; maps to CVTRA04Y                   |
| TRAN-CAT-BAL        | 05      | 17       | 11       | S9(09)V99 | DISPLAY | overpunch-sign-decode | Accumulated balance for this account/type/category combination; 9 integer + 2 decimal digits |
| FILLER              | 05      | 28       | 22       | X(22)     | DISPLAY | none                  | Reserved padding to 50-byte record length                                                    |

```json
{
  "copybook": "CVTRA01Y",
  "record": "TRAN-CAT-BAL-RECORD",
  "total_length": 50,
  "fields": [
    {
      "name": "TRAN-CAT-KEY",
      "level": "05",
      "offset": 0,
      "length": 17,
      "pic": null,
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Composite VSAM primary key comprising account ID, transaction type, and category code"
    },
    {
      "name": "TRANCAT-ACCT-ID",
      "level": "10",
      "offset": 0,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Account identifier; first segment of composite key; foreign key to CVACT01Y.ACCT-ID"
    },
    {
      "name": "TRANCAT-TYPE-CD",
      "level": "10",
      "offset": 11,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction type code (e.g., 'PR'=purchase, 'CR'=credit); second segment of composite key; foreign key to CVTRA03Y.TRAN-TYPE"
    },
    {
      "name": "TRANCAT-CD",
      "level": "10",
      "offset": 13,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "4-digit transaction category code; third segment of composite key; foreign key to CVTRA04Y.TRAN-CAT-CD"
    },
    {
      "name": "TRAN-CAT-BAL",
      "level": "05",
      "offset": 17,
      "length": 11,
      "pic": "S9(09)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Running balance in dollars for this account/type/category bucket; used by interest calculation to apply category-specific rates"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 28,
      "length": 22,
      "pic": "X(22)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 50-byte fixed record length"
    }
  ]
}
```

---

## CVTRA02Y

**File:** `app/cpy/CVTRA02Y.cpy`  
**Record:** `DIS-GROUP-RECORD`  
**Record Length:** 50 bytes  
**VSAM Type:** KSDS — composite primary key (`DIS-ACCT-GROUP-ID` + `DIS-TRAN-TYPE-CD` + `DIS-TRAN-CAT-CD`)  
**DD Name:** `DISCGRP`  
**Used By:** `CBACT04C.cbl`

Disclosure group / interest rate table. Each record stores an annual interest rate for a specific combination of account group, transaction type, and transaction category. The interest calculation program (`CBACT04C`) looks up the applicable rate here using the account's `ACCT-GROUP-ID` as the first key segment. If no specific rate exists for the account group, the program falls back to the key 'DEFAULT'.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name        | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                                      |
| :---------------- | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :------------------------------------------------------------------------------------ |
| DIS-GROUP-RECORD  | 01      | 0        | 50       | —         | —       | —                     | Root record for interest rate disclosure group                                        |
| DIS-GROUP-KEY     | 05      | 0        | 16       | —         | —       | —                     | Composite VSAM primary key (3 subfields)                                              |
| DIS-ACCT-GROUP-ID | 10      | 0        | 10       | X(10)     | DISPLAY | trailing-space-strip  | Account group identifier; matches CVACT01Y.ACCT-GROUP-ID; 'DEFAULT' = catch-all rate  |
| DIS-TRAN-TYPE-CD  | 10      | 10       | 2        | X(02)     | DISPLAY | trailing-space-strip  | Transaction type; foreign key to CVTRA03Y.TRAN-TYPE                                   |
| DIS-TRAN-CAT-CD   | 10      | 12       | 4        | 9(04)     | DISPLAY | none                  | Transaction category; foreign key to CVTRA04Y.TRAN-CAT-CD                             |
| DIS-INT-RATE      | 05      | 16       | 6        | S9(04)V99 | DISPLAY | overpunch-sign-decode | Annual interest rate percentage; 4 integer + 2 decimal digits (e.g., 002150 = 21.50%) |
| FILLER            | 05      | 22       | 28       | X(28)     | DISPLAY | none                  | Reserved padding to 50-byte record length                                             |

```json
{
  "copybook": "CVTRA02Y",
  "record": "DIS-GROUP-RECORD",
  "total_length": 50,
  "fields": [
    {
      "name": "DIS-GROUP-KEY",
      "level": "05",
      "offset": 0,
      "length": 16,
      "pic": null,
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Composite primary key: group ID + transaction type + category"
    },
    {
      "name": "DIS-ACCT-GROUP-ID",
      "level": "10",
      "offset": 0,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Account group identifier matching CVACT01Y.ACCT-GROUP-ID; value 'DEFAULT' is used as a fallback rate when no specific group match exists"
    },
    {
      "name": "DIS-TRAN-TYPE-CD",
      "level": "10",
      "offset": 10,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction type code partitioning this interest rate; foreign key to CVTRA03Y.TRAN-TYPE"
    },
    {
      "name": "DIS-TRAN-CAT-CD",
      "level": "10",
      "offset": 12,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Transaction category code partitioning this interest rate; foreign key to CVTRA04Y.TRAN-CAT-CD"
    },
    {
      "name": "DIS-INT-RATE",
      "level": "05",
      "offset": 16,
      "length": 6,
      "pic": "S9(04)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Annual interest rate as a percentage with 2 decimal places; e.g., raw value 002150 represents 21.50% APR; used by CBACT04C for interest calculation; use BigDecimal — never double"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 22,
      "length": 28,
      "pic": "X(28)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 50-byte fixed record length"
    }
  ]
}
```

---

## CVTRA03Y

**File:** `app/cpy/CVTRA03Y.cpy`  
**Record:** `TRAN-TYPE-RECORD`  
**Record Length:** 60 bytes  
**VSAM Type:** KSDS — primary key `TRAN-TYPE`  
**DD Name:** (referenced by CBTRN02C / CBACT04C via category lookup)  
**Used By:** `CBTRN02C.cbl`, reporting programs

Reference table mapping two-character transaction type codes to their descriptive labels. Used in report generation (CVTRA07Y structures) to expand codes to readable descriptions.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name       | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                         |
| :--------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :----------------------------------------------------------------------- |
| TRAN-TYPE-RECORD | 01      | 0        | 60       | —     | —       | —                    | Root record for transaction type reference data                          |
| TRAN-TYPE        | 05      | 0        | 2        | X(02) | DISPLAY | trailing-space-strip | Two-character transaction type code; primary VSAM key (e.g., 'PR', 'CR') |
| TRAN-TYPE-DESC   | 05      | 2        | 50       | X(50) | DISPLAY | trailing-space-strip | Human-readable description of the transaction type (up to 50 chars)      |
| FILLER           | 05      | 52       | 8        | X(08) | DISPLAY | none                 | Reserved padding to 60-byte record length                                |

```json
{
  "copybook": "CVTRA03Y",
  "record": "TRAN-TYPE-RECORD",
  "total_length": 60,
  "fields": [
    {
      "name": "TRAN-TYPE",
      "level": "05",
      "offset": 0,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Two-character transaction type code; primary VSAM key (e.g., 'PR'=Purchase, 'CR'=Credit, 'FE'=Fee)"
    },
    {
      "name": "TRAN-TYPE-DESC",
      "level": "05",
      "offset": 2,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Descriptive label for the transaction type; used in report column TRAN-REPORT-TYPE-DESC (15 chars displayed, 50 stored)"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 52,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 60-byte fixed record length"
    }
  ]
}
```

---

## CVTRA04Y

**File:** `app/cpy/CVTRA04Y.cpy`  
**Record:** `TRAN-CAT-RECORD`  
**Record Length:** 60 bytes  
**VSAM Type:** KSDS — composite primary key (`TRAN-TYPE-CD` + `TRAN-CAT-CD`)  
**Used By:** `CBTRN02C.cbl`, reporting programs

Reference table mapping transaction type + category combinations to descriptions. Each category belongs to a parent type (e.g., category 5010 under type 'PR' might be 'Grocery'). The composite key ensures category codes are scoped within their type.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name         | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                                     |
| :----------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :----------------------------------------------------------------------------------- |
| TRAN-CAT-RECORD    | 01      | 0        | 60       | —     | —       | —                    | Root record for transaction category reference data                                  |
| TRAN-CAT-KEY       | 05      | 0        | 6        | —     | —       | —                    | Composite VSAM primary key (type + category)                                         |
| TRAN-TYPE-CD       | 10      | 0        | 2        | X(02) | DISPLAY | trailing-space-strip | Transaction type code; first key segment; foreign key to CVTRA03Y.TRAN-TYPE          |
| TRAN-CAT-CD        | 10      | 2        | 4        | 9(04) | DISPLAY | none                 | 4-digit category code; second key segment; unique within a type                      |
| TRAN-CAT-TYPE-DESC | 05      | 6        | 50       | X(50) | DISPLAY | trailing-space-strip | Human-readable description of this category (e.g., 'Grocery Stores', 'Cash Advance') |
| FILLER             | 05      | 56       | 4        | X(04) | DISPLAY | none                 | Reserved padding to 60-byte record length                                            |

```json
{
  "copybook": "CVTRA04Y",
  "record": "TRAN-CAT-RECORD",
  "total_length": 60,
  "fields": [
    {
      "name": "TRAN-CAT-KEY",
      "level": "05",
      "offset": 0,
      "length": 6,
      "pic": null,
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Composite primary key: transaction type code + 4-digit category code"
    },
    {
      "name": "TRAN-TYPE-CD",
      "level": "10",
      "offset": 0,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Parent transaction type code; first key segment; category codes are unique within a type, not globally"
    },
    {
      "name": "TRAN-CAT-CD",
      "level": "10",
      "offset": 2,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "4-digit category code unique within its parent type; links to CVTRA01Y.TRANCAT-CD and CVTRA02Y.DIS-TRAN-CAT-CD"
    },
    {
      "name": "TRAN-CAT-TYPE-DESC",
      "level": "05",
      "offset": 6,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Human-readable category description used in report column TRAN-REPORT-CAT-DESC (29 chars displayed, 50 stored)"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 56,
      "length": 4,
      "pic": "X(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 60-byte fixed record length"
    }
  ]
}
```

---

## CVTRA05Y

**File:** `app/cpy/CVTRA05Y.cpy`  
**Record:** `TRAN-RECORD`  
**Record Length:** 350 bytes  
**VSAM Type:** KSDS — primary key `TRAN-ID`  
**DD Name:** `TRANSACT` (written by CBACT04C), `TRANFILE` (read by CBTRN02C)  
**Used By:** `CBACT04C.cbl`, `CBTRN02C.cbl`, `COTRN01C.cbl`, `COTRN02C.cbl`

The master transaction record. Written during online transaction entry and read/updated during batch posting. The timestamp fields (`TRAN-ORIG-TS`, `TRAN-PROC-TS`) store ISO-format timestamps with microsecond precision. The amount field is a signed display numeric requiring overpunch decode. The card number stored here is the PAN (no masking).

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name         | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                                               |
| :----------------- | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :--------------------------------------------------------------------------------------------- |
| TRAN-RECORD        | 01      | 0        | 350      | —         | —       | —                     | Root of the transaction master record                                                          |
| TRAN-ID            | 05      | 0        | 16       | X(16)     | DISPLAY | trailing-space-strip  | Unique transaction identifier; primary VSAM key                                                |
| TRAN-TYPE-CD       | 05      | 16       | 2        | X(02)     | DISPLAY | trailing-space-strip  | Transaction type code; foreign key to CVTRA03Y.TRAN-TYPE                                       |
| TRAN-CAT-CD        | 05      | 18       | 4        | 9(04)     | DISPLAY | none                  | Transaction category code; foreign key to CVTRA04Y.TRAN-CAT-CD (scoped within TRAN-TYPE-CD)    |
| TRAN-SOURCE        | 05      | 22       | 10       | X(10)     | DISPLAY | trailing-space-strip  | Origination channel (e.g., 'ONLINE', 'BATCH', 'POS')                                           |
| TRAN-DESC          | 05      | 32       | 100      | X(100)    | DISPLAY | trailing-space-strip  | Free-text transaction description                                                              |
| TRAN-AMT           | 05      | 132      | 11       | S9(09)V99 | DISPLAY | overpunch-sign-decode | Transaction amount in dollars; 9 integer + 2 decimal digits; negative = credit/refund          |
| TRAN-MERCHANT-ID   | 05      | 143      | 9        | 9(09)     | DISPLAY | none                  | Merchant identifier (9-digit numeric)                                                          |
| TRAN-MERCHANT-NAME | 05      | 152      | 50       | X(50)     | DISPLAY | trailing-space-strip  | Merchant legal or DBA name                                                                     |
| TRAN-MERCHANT-CITY | 05      | 202      | 50       | X(50)     | DISPLAY | trailing-space-strip  | City where the merchant is located                                                             |
| TRAN-MERCHANT-ZIP  | 05      | 252      | 10       | X(10)     | DISPLAY | trailing-space-strip  | Merchant ZIP or postal code                                                                    |
| TRAN-CARD-NUM      | 05      | 262      | 16       | X(16)     | DISPLAY | trailing-space-strip  | Card number (PAN) used for this transaction; PII                                               |
| TRAN-ORIG-TS       | 05      | 278      | 26       | X(26)     | DISPLAY | date-parse            | Original transaction timestamp; format YYYY-MM-DD HH:MM:SS.nnnnnn (ISO 8601 with microseconds) |
| TRAN-PROC-TS       | 05      | 304      | 26       | X(26)     | DISPLAY | date-parse            | Batch processing timestamp; same format as TRAN-ORIG-TS; set during posting run                |
| FILLER             | 05      | 330      | 20       | X(20)     | DISPLAY | none                  | Reserved padding to 350-byte record length                                                     |

```json
{
  "copybook": "CVTRA05Y",
  "record": "TRAN-RECORD",
  "total_length": 350,
  "fields": [
    {
      "name": "TRAN-ID",
      "level": "05",
      "offset": 0,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Unique transaction ID; primary VSAM key for TRANSACT/TRANFILE"
    },
    {
      "name": "TRAN-TYPE-CD",
      "level": "05",
      "offset": 16,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Two-character transaction type code; foreign key to CVTRA03Y"
    },
    {
      "name": "TRAN-CAT-CD",
      "level": "05",
      "offset": 18,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "4-digit category code scoped within TRAN-TYPE-CD; used with type code to look up interest rate and description"
    },
    {
      "name": "TRAN-SOURCE",
      "level": "05",
      "offset": 22,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Origination channel or system that created this transaction (e.g., 'ONLINE', 'BATCH')"
    },
    {
      "name": "TRAN-DESC",
      "level": "05",
      "offset": 32,
      "length": 100,
      "pic": "X(100)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Free-text description of the transaction; may contain merchant-provided narrative"
    },
    {
      "name": "TRAN-AMT",
      "level": "05",
      "offset": 132,
      "length": 11,
      "pic": "S9(09)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Transaction dollar amount; 9 integer + 2 decimal digits; negative = credit or refund; use BigDecimal for all arithmetic"
    },
    {
      "name": "TRAN-MERCHANT-ID",
      "level": "05",
      "offset": 143,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Numeric merchant identifier; 9-digit unique merchant code"
    },
    {
      "name": "TRAN-MERCHANT-NAME",
      "level": "05",
      "offset": 152,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant name as it appears on the cardholder's statement"
    },
    {
      "name": "TRAN-MERCHANT-CITY",
      "level": "05",
      "offset": 202,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "City of the merchant location; used in transaction detail report"
    },
    {
      "name": "TRAN-MERCHANT-ZIP",
      "level": "05",
      "offset": 252,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant ZIP or postal code"
    },
    {
      "name": "TRAN-CARD-NUM",
      "level": "05",
      "offset": 262,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Full PAN (card number) used for this transaction; PII — must be masked in logs and test data"
    },
    {
      "name": "TRAN-ORIG-TS",
      "level": "05",
      "offset": 278,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Timestamp when the transaction originally occurred; format: YYYY-MM-DD HH:MM:SS.nnnnnn (26 chars including microseconds)"
    },
    {
      "name": "TRAN-PROC-TS",
      "level": "05",
      "offset": 304,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Timestamp when the transaction was processed by batch; same format as TRAN-ORIG-TS; set by CBTRN02C during posting run"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 330,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 350-byte fixed record length"
    }
  ]
}
```

---

## CVTRA06Y

**File:** `app/cpy/CVTRA06Y.cpy`  
**Record:** `DALYTRAN-RECORD`  
**Record Length:** 350 bytes  
**File Type:** Sequential flat file  
**DD Name:** `DALYTRAN`  
**Used By:** `CBTRN02C.cbl` (reads), `CBTRN01C.cbl` (writes)

Daily transaction input file. Structurally identical to `CVTRA05Y.TRAN-RECORD` but uses the `DALYTRAN-` field prefix. This file is produced during the online day and consumed by the nightly batch posting run (`CBTRN02C`). Records rejected during posting are written to `DALYREJS`.

All field lengths, offsets, PIC clauses, and normalization rules are identical to CVTRA05Y — only the field-name prefix differs. Refer to [CVTRA05Y](#cvtra05y) for detailed field descriptions.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name             | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                  |
| :--------------------- | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :---------------------------------------------------------------- |
| DALYTRAN-RECORD        | 01      | 0        | 350      | —         | —       | —                     | Root of the daily transaction input record                        |
| DALYTRAN-ID            | 05      | 0        | 16       | X(16)     | DISPLAY | trailing-space-strip  | Unique transaction identifier (same semantics as TRAN-ID)         |
| DALYTRAN-TYPE-CD       | 05      | 16       | 2        | X(02)     | DISPLAY | trailing-space-strip  | Transaction type code                                             |
| DALYTRAN-CAT-CD        | 05      | 18       | 4        | 9(04)     | DISPLAY | none                  | Transaction category code                                         |
| DALYTRAN-SOURCE        | 05      | 22       | 10       | X(10)     | DISPLAY | trailing-space-strip  | Origination channel                                               |
| DALYTRAN-DESC          | 05      | 32       | 100      | X(100)    | DISPLAY | trailing-space-strip  | Transaction description                                           |
| DALYTRAN-AMT           | 05      | 132      | 11       | S9(09)V99 | DISPLAY | overpunch-sign-decode | Transaction amount; negative = credit/refund                      |
| DALYTRAN-MERCHANT-ID   | 05      | 143      | 9        | 9(09)     | DISPLAY | none                  | Merchant identifier                                               |
| DALYTRAN-MERCHANT-NAME | 05      | 152      | 50       | X(50)     | DISPLAY | trailing-space-strip  | Merchant name                                                     |
| DALYTRAN-MERCHANT-CITY | 05      | 202      | 50       | X(50)     | DISPLAY | trailing-space-strip  | Merchant city                                                     |
| DALYTRAN-MERCHANT-ZIP  | 05      | 252      | 10       | X(10)     | DISPLAY | trailing-space-strip  | Merchant ZIP code                                                 |
| DALYTRAN-CARD-NUM      | 05      | 262      | 16       | X(16)     | DISPLAY | trailing-space-strip  | Card number (PAN) used; PII                                       |
| DALYTRAN-ORIG-TS       | 05      | 278      | 26       | X(26)     | DISPLAY | date-parse            | Original transaction timestamp; format YYYY-MM-DD HH:MM:SS.nnnnnn |
| DALYTRAN-PROC-TS       | 05      | 304      | 26       | X(26)     | DISPLAY | date-parse            | Batch processing timestamp; same format                           |
| FILLER                 | 05      | 330      | 20       | X(20)     | DISPLAY | none                  | Reserved padding to 350-byte record length                        |

```json
{
  "copybook": "CVTRA06Y",
  "record": "DALYTRAN-RECORD",
  "total_length": 350,
  "fields": [
    {
      "name": "DALYTRAN-ID",
      "level": "05",
      "offset": 0,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Unique transaction identifier for this daily transaction; same semantics as TRAN-ID in CVTRA05Y"
    },
    {
      "name": "DALYTRAN-TYPE-CD",
      "level": "05",
      "offset": 16,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction type code; foreign key to CVTRA03Y.TRAN-TYPE"
    },
    {
      "name": "DALYTRAN-CAT-CD",
      "level": "05",
      "offset": 18,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "4-digit category code scoped within DALYTRAN-TYPE-CD"
    },
    {
      "name": "DALYTRAN-SOURCE",
      "level": "05",
      "offset": 22,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Origination system or channel for this transaction"
    },
    {
      "name": "DALYTRAN-DESC",
      "level": "05",
      "offset": 32,
      "length": 100,
      "pic": "X(100)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Free-text transaction description"
    },
    {
      "name": "DALYTRAN-AMT",
      "level": "05",
      "offset": 132,
      "length": 11,
      "pic": "S9(09)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Transaction amount; 9 integer + 2 decimal digits; negative = refund/credit; use BigDecimal"
    },
    {
      "name": "DALYTRAN-MERCHANT-ID",
      "level": "05",
      "offset": 143,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Numeric merchant identifier"
    },
    {
      "name": "DALYTRAN-MERCHANT-NAME",
      "level": "05",
      "offset": 152,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant name for statement and reporting"
    },
    {
      "name": "DALYTRAN-MERCHANT-CITY",
      "level": "05",
      "offset": 202,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant city"
    },
    {
      "name": "DALYTRAN-MERCHANT-ZIP",
      "level": "05",
      "offset": 252,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant ZIP or postal code"
    },
    {
      "name": "DALYTRAN-CARD-NUM",
      "level": "05",
      "offset": 262,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Card PAN used for this transaction; used to look up account via XREFFILE; PII"
    },
    {
      "name": "DALYTRAN-ORIG-TS",
      "level": "05",
      "offset": 278,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Timestamp the transaction was initiated; format YYYY-MM-DD HH:MM:SS.nnnnnn"
    },
    {
      "name": "DALYTRAN-PROC-TS",
      "level": "05",
      "offset": 304,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Timestamp the transaction was processed by batch; format YYYY-MM-DD HH:MM:SS.nnnnnn"
    },
    {
      "name": "FILLER",
      "level": "05",
      "offset": 330,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 350-byte fixed record length"
    }
  ]
}
```

---

## CVTRA07Y

**File:** `app/cpy/CVTRA07Y.cpy`  
**Used By:** `CBSTM03A.CBL`, `CBSTM03B.CBL`, `CORPT00C.cbl`

This copybook defines **print report layouts** — not file records. It contains seven `01`-level structures representing lines of the Daily Transaction Report. Fields with `PIC -ZZZ,ZZZ,ZZZ.ZZ` or `PIC +ZZZ,ZZZ,ZZZ.ZZ` are edited numeric edit pictures (15 bytes each): already formatted with sign, zero-suppression, and decimal for printing.

> **Migration note:** These are output-only structures. The Java equivalent should use `String.format` or `DecimalFormat` to produce equivalent formatted output. The edit picture `+ZZZ,ZZZ,ZZZ.ZZ` produces `+999,999,999.99` for positives and `-999,999,999.99` for negatives with leading zero suppression.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

### REPORT-NAME-HEADER (115 bytes)

| Field Name       | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                             |
| :--------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :----------------------------------------------------------- |
| REPT-SHORT-NAME  | 05      | 0        | 38       | X(38) | DISPLAY | none                 | Short report name constant: 'DALYREPT' left-justified        |
| REPT-LONG-NAME   | 05      | 38       | 41       | X(41) | DISPLAY | none                 | Full report title: 'Daily Transaction Report' left-justified |
| REPT-DATE-HEADER | 05      | 79       | 12       | X(12) | DISPLAY | none                 | Label 'Date Range: '                                         |
| REPT-START-DATE  | 05      | 91       | 10       | X(10) | DISPLAY | date-parse           | Report start date populated at runtime; format YYYY-MM-DD    |
| FILLER           | 05      | 101      | 4        | X(04) | DISPLAY | none                 | Literal ' to ' separator                                     |
| REPT-END-DATE    | 05      | 105      | 10       | X(10) | DISPLAY | date-parse           | Report end date populated at runtime; format YYYY-MM-DD      |

### TRANSACTION-DETAIL-REPORT (114 bytes)

| Field Name             | Level   | Offset   | Length   | PIC             | USAGE   | Normalization Rule   | Business Meaning                                                         |
| :--------------------- | :------ | -------: | -------: | :-------------- | :------ | :------------------- | :----------------------------------------------------------------------- |
| TRAN-REPORT-TRANS-ID   | 05      | 0        | 16       | X(16)           | DISPLAY | trailing-space-strip | Transaction ID column                                                    |
| FILLER                 | 05      | 16       | 1        | X(01)           | DISPLAY | none                 | Column separator                                                         |
| TRAN-REPORT-ACCOUNT-ID | 05      | 17       | 11       | X(11)           | DISPLAY | trailing-space-strip | Account ID column                                                        |
| FILLER                 | 05      | 28       | 1        | X(01)           | DISPLAY | none                 | Column separator                                                         |
| TRAN-REPORT-TYPE-CD    | 05      | 29       | 2        | X(02)           | DISPLAY | trailing-space-strip | Transaction type code column                                             |
| FILLER                 | 05      | 31       | 1        | X(01)           | DISPLAY | none                 | Dash separator                                                           |
| TRAN-REPORT-TYPE-DESC  | 05      | 32       | 15       | X(15)           | DISPLAY | trailing-space-strip | Transaction type description (first 15 chars of TRAN-TYPE-DESC)          |
| FILLER                 | 05      | 47       | 1        | X(01)           | DISPLAY | none                 | Column separator                                                         |
| TRAN-REPORT-CAT-CD     | 05      | 48       | 4        | 9(04)           | DISPLAY | none                 | Category code column                                                     |
| FILLER                 | 05      | 52       | 1        | X(01)           | DISPLAY | none                 | Dash separator                                                           |
| TRAN-REPORT-CAT-DESC   | 05      | 53       | 29       | X(29)           | DISPLAY | trailing-space-strip | Category description (first 29 chars of TRAN-CAT-TYPE-DESC)              |
| FILLER                 | 05      | 82       | 1        | X(01)           | DISPLAY | none                 | Column separator                                                         |
| TRAN-REPORT-SOURCE     | 05      | 83       | 10       | X(10)           | DISPLAY | trailing-space-strip | Transaction source channel column                                        |
| FILLER                 | 05      | 93       | 4        | X(04)           | DISPLAY | none                 | Column spacing                                                           |
| TRAN-REPORT-AMT        | 05      | 97       | 15       | -ZZZ,ZZZ,ZZZ.ZZ | DISPLAY | none                 | Formatted transaction amount; negative sign floats left; zero-suppressed |
| FILLER                 | 05      | 112      | 2        | X(02)           | DISPLAY | none                 | Trailing column spacing                                                  |

```json
{
  "copybook": "CVTRA07Y",
  "record": "REPORT-NAME-HEADER",
  "total_length": 115,
  "note": "Print-line structure — not a file record. Six additional 01-level print structures in this copybook; see markdown table above.",
  "fields": [
    {
      "name": "REPT-SHORT-NAME",
      "level": "05",
      "offset": 0,
      "length": 38,
      "pic": "X(38)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Short report identifier constant 'DALYREPT'"
    },
    {
      "name": "REPT-LONG-NAME",
      "level": "05",
      "offset": 38,
      "length": 41,
      "pic": "X(41)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Full report title 'Daily Transaction Report'"
    },
    {
      "name": "REPT-DATE-HEADER",
      "level": "05",
      "offset": 79,
      "length": 12,
      "pic": "X(12)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Label 'Date Range: ' — constant"
    },
    {
      "name": "REPT-START-DATE",
      "level": "05",
      "offset": 91,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Report start date in YYYY-MM-DD format; populated at runtime"
    },
    {
      "name": "REPT-END-DATE",
      "level": "05",
      "offset": 105,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Report end date in YYYY-MM-DD format; populated at runtime"
    }
  ]
}
```

---

## CVEXPORT

**File:** `app/cpy/CVEXPORT.cpy`  
**Record:** `EXPORT-RECORD`  
**Record Length:** 500 bytes  
**File Type:** Sequential export file  
**Used By:** `CBEXPORT.cbl` (writes), `CBIMPORT.cbl` (reads)

Multi-type export record using `REDEFINES` for type dispatch. The first byte (`EXPORT-REC-TYPE`) indicates which of five payload types occupies the `EXPORT-RECORD-DATA` area. This is a branch migration export format — it transports all core entities in a single sequential file.

⚠️ **WARNING — REDEFINES type dispatch**: The 460-byte `EXPORT-RECORD-DATA` field is redefined five times. The Java parser **must** read `EXPORT-REC-TYPE` first and then dispatch to the correct substructure. Do not attempt to parse `EXPORT-RECORD-DATA` without first checking the type byte.

⚠️ **WARNING — Mixed USAGE within REDEFINES variants**: Different variants use different USAGE clauses (COMP, COMP-3, DISPLAY) for fields that are DISPLAY in the base copybooks. The export file is **not byte-for-byte compatible** with the base VSAM records.

⚠️ **WARNING — OCCURS inside REDEFINES** (`EXPORT-CUSTOMER-DATA`): `EXP-CUST-ADDR-LINES OCCURS 3 TIMES` and `EXP-CUST-PHONE-NUMS OCCURS 2 TIMES` are nested inside a REDEFINES. The Java parser must handle both the OCCURS iteration and the REDEFINES dispatch simultaneously.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

### Envelope Fields (all record types)

| Field Name              | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule   | Business Meaning                                                                               |
| :---------------------- | :------ | -------: | -------: | :-------- | :------ | :------------------- | :--------------------------------------------------------------------------------------------- |
| EXPORT-RECORD           | 01      | 0        | 500      | —         | —       | —                    | Root of the multi-type export record                                                           |
| EXPORT-REC-TYPE         | 05      | 0        | 1        | X(1)      | DISPLAY | trailing-space-strip | Record type discriminator: 'C'=Customer, 'A'=Account, 'T'=Transaction, 'X'=Card-Xref, 'K'=Card |
| EXPORT-TIMESTAMP        | 05      | 1        | 26       | X(26)     | DISPLAY | date-parse           | Export timestamp; format YYYY-MM-DD HH:MM:SS.nnnnnn                                            |
| EXPORT-DATE (REDEFINES) | 10      | 1        | 10       | X(10)     | DISPLAY | date-parse           | Date component of timestamp (YYYY-MM-DD)                                                       |
| EXPORT-DATE-TIME-SEP    | 10      | 11       | 1        | X(1)      | DISPLAY | none                 | Separator 'T' or space between date and time                                                   |
| EXPORT-TIME             | 10      | 12       | 15       | X(15)     | DISPLAY | none                 | Time component of timestamp (HH:MM:SS.nnnnnn)                                                  |
| EXPORT-SEQUENCE-NUM     | 05      | 27       | 4        | 9(9) COMP | COMP    | none                 | Sequential record counter; big-endian binary 4-byte unsigned integer                           |
| EXPORT-BRANCH-ID        | 05      | 31       | 4        | X(4)      | DISPLAY | trailing-space-strip | Source branch or system identifier                                                             |
| EXPORT-REGION-CODE      | 05      | 35       | 5        | X(5)      | DISPLAY | trailing-space-strip | Geographic region code for routing                                                             |
| EXPORT-RECORD-DATA      | 05      | 40       | 460      | X(460)    | DISPLAY | none                 | Payload area; interpreted by EXPORT-REC-TYPE discriminator                                     |

### Customer Payload (EXPORT-REC-TYPE = 'C')

⚠️ **WARNING — OCCURS inside REDEFINES**: `EXP-CUST-ADDR-LINES OCCURS 3 TIMES` at offset 43 (relative to payload start = 83 absolute). Each occurrence is 50 bytes. `EXP-CUST-PHONE-NUMS OCCURS 2 TIMES` at absolute offset 243. Each occurrence is 15 bytes.

| Field Name                   | Level   | Abs Offset   | Length   | PIC          | USAGE   | Normalization Rule   | Business Meaning                                                                      |
| :--------------------------- | :------ | -----------: | -------: | :----------- | :------ | :------------------- | :------------------------------------------------------------------------------------ |
| EXP-CUST-ID                  | 10      | 40           | 4        | 9(09) COMP   | COMP    | none                 | Customer ID as big-endian 4-byte binary (DISPLAY in CVCUS01Y — note USAGE difference) |
| EXP-CUST-FIRST-NAME          | 10      | 44           | 25       | X(25)        | DISPLAY | trailing-space-strip | Customer first name                                                                   |
| EXP-CUST-MIDDLE-NAME         | 10      | 69           | 25       | X(25)        | DISPLAY | trailing-space-strip | Customer middle name                                                                  |
| EXP-CUST-LAST-NAME           | 10      | 94           | 25       | X(25)        | DISPLAY | trailing-space-strip | Customer last name                                                                    |
| EXP-CUST-ADDR-LINE (×3)      | 15      | 119–268      | 50 each  | X(50)        | DISPLAY | trailing-space-strip | Address lines 1–3; OCCURS 3 TIMES starting at offset 119                              |
| EXP-CUST-ADDR-STATE-CD       | 10      | 269          | 2        | X(02)        | DISPLAY | trailing-space-strip | State code                                                                            |
| EXP-CUST-ADDR-COUNTRY-CD     | 10      | 271          | 3        | X(03)        | DISPLAY | trailing-space-strip | Country code                                                                          |
| EXP-CUST-ADDR-ZIP            | 10      | 274          | 10       | X(10)        | DISPLAY | trailing-space-strip | ZIP code                                                                              |
| EXP-CUST-PHONE-NUM (×2)      | 15      | 284–313      | 15 each  | X(15)        | DISPLAY | trailing-space-strip | Phone numbers 1–2; OCCURS 2 TIMES starting at offset 284                              |
| EXP-CUST-SSN                 | 10      | 314          | 9        | 9(09)        | DISPLAY | none                 | SSN (9 digits, PII)                                                                   |
| EXP-CUST-GOVT-ISSUED-ID      | 10      | 323          | 20       | X(20)        | DISPLAY | trailing-space-strip | Government ID                                                                         |
| EXP-CUST-DOB-YYYY-MM-DD      | 10      | 343          | 10       | X(10)        | DISPLAY | date-parse           | Date of birth; YYYY-MM-DD                                                             |
| EXP-CUST-EFT-ACCOUNT-ID      | 10      | 353          | 10       | X(10)        | DISPLAY | trailing-space-strip | EFT account ID                                                                        |
| EXP-CUST-PRI-CARD-HOLDER-IND | 10      | 363          | 1        | X(01)        | DISPLAY | trailing-space-strip | Primary cardholder flag                                                               |
| EXP-CUST-FICO-CREDIT-SCORE   | 10      | 364          | 2        | 9(03) COMP-3 | COMP-3  | COMP-3/BigDecimal    | FICO score as packed decimal (DISPLAY in CVCUS01Y)                                    |
| FILLER                       | 10      | 366          | 134      | X(134)       | DISPLAY | none                 | Padding within customer payload                                                       |

### Account Payload (EXPORT-REC-TYPE = 'A')

| Field Name                 | Level   | Abs Offset   | Length   | PIC              | USAGE   | Normalization Rule    | Business Meaning                                                               |
| :------------------------- | :------ | -----------: | -------: | :--------------- | :------ | :-------------------- | :----------------------------------------------------------------------------- |
| EXP-ACCT-ID                | 10      | 40           | 11       | 9(11)            | DISPLAY | none                  | Account ID (DISPLAY as in CVACT01Y)                                            |
| EXP-ACCT-ACTIVE-STATUS     | 10      | 51           | 1        | X(01)            | DISPLAY | trailing-space-strip  | Account active flag                                                            |
| EXP-ACCT-CURR-BAL          | 10      | 52           | 7        | S9(10)V99 COMP-3 | COMP-3  | COMP-3/BigDecimal     | Current balance as packed decimal (DISPLAY in CVACT01Y)                        |
| EXP-ACCT-CREDIT-LIMIT      | 10      | 59           | 12       | S9(10)V99        | DISPLAY | overpunch-sign-decode | Credit limit (DISPLAY, same as CVACT01Y)                                       |
| EXP-ACCT-CASH-CREDIT-LIMIT | 10      | 71           | 7        | S9(10)V99 COMP-3 | COMP-3  | COMP-3/BigDecimal     | Cash credit limit as packed decimal                                            |
| EXP-ACCT-OPEN-DATE         | 10      | 78           | 10       | X(10)            | DISPLAY | date-parse            | Account open date; YYYY-MM-DD                                                  |
| EXP-ACCT-EXPIRAION-DATE    | 10      | 88           | 10       | X(10)            | DISPLAY | date-parse            | Account expiration date; YYYY-MM-DD; note EXPIRAION typo                       |
| EXP-ACCT-REISSUE-DATE      | 10      | 98           | 10       | X(10)            | DISPLAY | date-parse            | Card reissue date; YYYY-MM-DD                                                  |
| EXP-ACCT-CURR-CYC-CREDIT   | 10      | 108          | 12       | S9(10)V99        | DISPLAY | overpunch-sign-decode | Cycle credits (DISPLAY)                                                        |
| EXP-ACCT-CURR-CYC-DEBIT    | 10      | 120          | 8        | S9(10)V99 COMP   | COMP    | none                  | Cycle debits as 8-byte binary (DISPLAY in CVACT01Y — significant USAGE change) |
| EXP-ACCT-ADDR-ZIP          | 10      | 128          | 10       | X(10)            | DISPLAY | trailing-space-strip  | Billing ZIP code                                                               |
| EXP-ACCT-GROUP-ID          | 10      | 138          | 10       | X(10)            | DISPLAY | trailing-space-strip  | Disclosure group ID                                                            |
| FILLER                     | 10      | 148          | 352      | X(352)           | DISPLAY | none                  | Padding within account payload                                                 |

### Transaction Payload (EXPORT-REC-TYPE = 'T')

| Field Name             | Level   | Abs Offset   | Length   | PIC              | USAGE   | Normalization Rule   | Business Meaning                               |
| :--------------------- | :------ | -----------: | -------: | :--------------- | :------ | :------------------- | :--------------------------------------------- |
| EXP-TRAN-ID            | 10      | 40           | 16       | X(16)            | DISPLAY | trailing-space-strip | Transaction ID                                 |
| EXP-TRAN-TYPE-CD       | 10      | 56           | 2        | X(02)            | DISPLAY | trailing-space-strip | Transaction type code                          |
| EXP-TRAN-CAT-CD        | 10      | 58           | 4        | 9(04)            | DISPLAY | none                 | Category code                                  |
| EXP-TRAN-SOURCE        | 10      | 62           | 10       | X(10)            | DISPLAY | trailing-space-strip | Source channel                                 |
| EXP-TRAN-DESC          | 10      | 72           | 100      | X(100)           | DISPLAY | trailing-space-strip | Description                                    |
| EXP-TRAN-AMT           | 10      | 172          | 6        | S9(09)V99 COMP-3 | COMP-3  | COMP-3/BigDecimal    | Amount as packed decimal (DISPLAY in CVTRA05Y) |
| EXP-TRAN-MERCHANT-ID   | 10      | 178          | 4        | 9(09) COMP       | COMP    | none                 | Merchant ID as 4-byte binary                   |
| EXP-TRAN-MERCHANT-NAME | 10      | 182          | 50       | X(50)            | DISPLAY | trailing-space-strip | Merchant name                                  |
| EXP-TRAN-MERCHANT-CITY | 10      | 232          | 50       | X(50)            | DISPLAY | trailing-space-strip | Merchant city                                  |
| EXP-TRAN-MERCHANT-ZIP  | 10      | 282          | 10       | X(10)            | DISPLAY | trailing-space-strip | Merchant ZIP                                   |
| EXP-TRAN-CARD-NUM      | 10      | 292          | 16       | X(16)            | DISPLAY | trailing-space-strip | Card number (PAN); PII                         |
| EXP-TRAN-ORIG-TS       | 10      | 308          | 26       | X(26)            | DISPLAY | date-parse           | Original timestamp; YYYY-MM-DD HH:MM:SS.nnnnnn |
| EXP-TRAN-PROC-TS       | 10      | 334          | 26       | X(26)            | DISPLAY | date-parse           | Processing timestamp                           |
| FILLER                 | 10      | 360          | 140      | X(140)           | DISPLAY | none                 | Padding within transaction payload             |

### Card Cross-Reference Payload (EXPORT-REC-TYPE = 'X')

| Field Name        | Level   | Abs Offset   | Length   | PIC        | USAGE   | Normalization Rule   | Business Meaning                                         |
| :---------------- | :------ | -----------: | -------: | :--------- | :------ | :------------------- | :------------------------------------------------------- |
| EXP-XREF-CARD-NUM | 10      | 40           | 16       | X(16)      | DISPLAY | trailing-space-strip | Card PAN                                                 |
| EXP-XREF-CUST-ID  | 10      | 56           | 9        | 9(09)      | DISPLAY | none                 | Customer ID (DISPLAY)                                    |
| EXP-XREF-ACCT-ID  | 10      | 65           | 8        | 9(11) COMP | COMP    | none                 | Account ID as 8-byte binary (11 digits → COMP = 8 bytes) |
| FILLER            | 10      | 73           | 427      | X(427)     | DISPLAY | none                 | Padding within xref payload                              |

### Card Payload (EXPORT-REC-TYPE = 'K')

| Field Name              | Level   | Abs Offset   | Length   | PIC        | USAGE   | Normalization Rule   | Business Meaning                             |
| :---------------------- | :------ | -----------: | -------: | :--------- | :------ | :------------------- | :------------------------------------------- |
| EXP-CARD-NUM            | 10      | 40           | 16       | X(16)      | DISPLAY | trailing-space-strip | Card PAN                                     |
| EXP-CARD-ACCT-ID        | 10      | 56           | 8        | 9(11) COMP | COMP    | none                 | Account ID as 8-byte binary                  |
| EXP-CARD-CVV-CD         | 10      | 64           | 2        | 9(03) COMP | COMP    | none                 | CVV as 2-byte binary                         |
| EXP-CARD-EMBOSSED-NAME  | 10      | 66           | 50       | X(50)      | DISPLAY | trailing-space-strip | Embossed name                                |
| EXP-CARD-EXPIRAION-DATE | 10      | 116          | 10       | X(10)      | DISPLAY | date-parse           | Card expiry date; YYYY-MM-DD; EXPIRAION typo |
| EXP-CARD-ACTIVE-STATUS  | 10      | 126          | 1        | X(01)      | DISPLAY | trailing-space-strip | Card active flag                             |
| FILLER                  | 10      | 127          | 373      | X(373)     | DISPLAY | none                 | Padding within card payload                  |

```json
{
  "copybook": "CVEXPORT",
  "record": "EXPORT-RECORD",
  "total_length": 500,
  "note": "Multi-type record with REDEFINES dispatch on EXPORT-REC-TYPE. Five payload variants share bytes 40-499. See full tables above for payload-level offsets.",
  "fields": [
    {
      "name": "EXPORT-REC-TYPE",
      "level": "05",
      "offset": 0,
      "length": 1,
      "pic": "X(1)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Type discriminator: 'C'=Customer, 'A'=Account, 'T'=Transaction, 'X'=Card-Xref, 'K'=Card"
    },
    {
      "name": "EXPORT-TIMESTAMP",
      "level": "05",
      "offset": 1,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Record export timestamp in ISO format YYYY-MM-DD HH:MM:SS.nnnnnn"
    },
    {
      "name": "EXPORT-DATE",
      "level": "10",
      "offset": 1,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "redefines": "EXPORT-TIMESTAMP",
      "description": "Date portion of EXPORT-TIMESTAMP (YYYY-MM-DD)"
    },
    {
      "name": "EXPORT-DATE-TIME-SEP",
      "level": "10",
      "offset": 11,
      "length": 1,
      "pic": "X(1)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Single-character separator between date and time components"
    },
    {
      "name": "EXPORT-TIME",
      "level": "10",
      "offset": 12,
      "length": 15,
      "pic": "X(15)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Time portion of EXPORT-TIMESTAMP (HH:MM:SS.nnnnnn)"
    },
    {
      "name": "EXPORT-SEQUENCE-NUM",
      "level": "05",
      "offset": 27,
      "length": 4,
      "pic": "9(9) COMP",
      "usage": "COMP",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Sequential record counter as 4-byte big-endian unsigned binary; max value 999,999,999"
    },
    {
      "name": "EXPORT-BRANCH-ID",
      "level": "05",
      "offset": 31,
      "length": 4,
      "pic": "X(4)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Identifier of the source branch or system exporting this record"
    },
    {
      "name": "EXPORT-REGION-CODE",
      "level": "05",
      "offset": 35,
      "length": 5,
      "pic": "X(5)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Geographic region code for routing decisions during import"
    },
    {
      "name": "EXPORT-RECORD-DATA",
      "level": "05",
      "offset": 40,
      "length": 460,
      "pic": "X(460)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "460-byte payload area; redefined by five named variant structures; parse according to EXPORT-REC-TYPE"
    }
  ]
}
```

---

## COCOM01Y

**File:** `app/cpy/COCOM01Y.cpy`  
**Record:** `CARDDEMO-COMMAREA`  
**Length:** 160 bytes  
**Type:** CICS COMMAREA (passed between programs via EXEC CICS LINK/XCTL)  
**Used By:** All online CICS programs

The CICS communication area passed between every online screen program. It carries the navigation context (which transaction/program called whom), the authenticated user's identity and type, and the currently selected customer/account/card identifiers. Programs use this to implement stateful navigation without server-side session storage.

`CDEMO-USER-TYPE` controls access: value 'A' grants administrator menus (COADM02Y options), value 'U' restricts to regular user menus (COMEN02Y options). `CDEMO-PGM-CONTEXT` distinguishes first entry (value 0) from re-entry after validation failure (value 1).

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name          | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                        |
| :------------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :---------------------------------------------------------------------- |
| CARDDEMO-COMMAREA   | 01      | 0        | 160      | —     | —       | —                    | Root of the CICS communication area                                     |
| CDEMO-GENERAL-INFO  | 05      | 0        | 34       | —     | —       | —                    | Navigation and identity group                                           |
| CDEMO-FROM-TRANID   | 10      | 0        | 4        | X(04) | DISPLAY | trailing-space-strip | 4-char CICS transaction ID of the calling program                       |
| CDEMO-FROM-PROGRAM  | 10      | 4        | 8        | X(08) | DISPLAY | trailing-space-strip | 8-char program name of the calling program                              |
| CDEMO-TO-TRANID     | 10      | 12       | 4        | X(04) | DISPLAY | trailing-space-strip | 4-char CICS transaction ID of the target program                        |
| CDEMO-TO-PROGRAM    | 10      | 16       | 8        | X(08) | DISPLAY | trailing-space-strip | 8-char program name of the target program                               |
| CDEMO-USER-ID       | 10      | 24       | 8        | X(08) | DISPLAY | trailing-space-strip | Authenticated user ID (matches CSUSR01Y.SEC-USR-ID)                     |
| CDEMO-USER-TYPE     | 10      | 32       | 1        | X(01) | DISPLAY | trailing-space-strip | User type: 'A'=Admin (access to COADM02Y menu), 'U'=Regular user        |
| CDEMO-PGM-CONTEXT   | 10      | 33       | 1        | 9(01) | DISPLAY | none                 | Program entry context: 0=first entry, 1=re-entry after screen error     |
| CDEMO-CUSTOMER-INFO | 05      | 34       | 84       | —     | —       | —                    | Currently selected customer                                             |
| CDEMO-CUST-ID       | 10      | 34       | 9        | 9(09) | DISPLAY | none                 | ID of the customer currently being viewed/edited                        |
| CDEMO-CUST-FNAME    | 10      | 43       | 25       | X(25) | DISPLAY | trailing-space-strip | Customer first name for display purposes                                |
| CDEMO-CUST-MNAME    | 10      | 68       | 25       | X(25) | DISPLAY | trailing-space-strip | Customer middle name for display purposes                               |
| CDEMO-CUST-LNAME    | 10      | 93       | 25       | X(25) | DISPLAY | trailing-space-strip | Customer last name for display purposes                                 |
| CDEMO-ACCOUNT-INFO  | 05      | 118      | 12       | —     | —       | —                    | Currently selected account                                              |
| CDEMO-ACCT-ID       | 10      | 118      | 11       | 9(11) | DISPLAY | none                 | ID of the account currently being viewed/edited                         |
| CDEMO-ACCT-STATUS   | 10      | 129      | 1        | X(01) | DISPLAY | trailing-space-strip | Account active status for display (mirrors CVACT01Y.ACCT-ACTIVE-STATUS) |
| CDEMO-CARD-INFO     | 05      | 130      | 16       | —     | —       | —                    | Currently selected card                                                 |
| CDEMO-CARD-NUM      | 10      | 130      | 16       | 9(16) | DISPLAY | none                 | Card number currently being viewed/edited (numeric display; 16 digits)  |
| CDEMO-MORE-INFO     | 05      | 146      | 14       | —     | —       | —                    | Last screen navigation reference                                        |
| CDEMO-LAST-MAP      | 10      | 146      | 7        | X(7)  | DISPLAY | trailing-space-strip | BMS map name of the last screen displayed                               |
| CDEMO-LAST-MAPSET   | 10      | 153      | 7        | X(7)  | DISPLAY | trailing-space-strip | BMS mapset name of the last screen displayed                            |

```json
{
  "copybook": "COCOM01Y",
  "record": "CARDDEMO-COMMAREA",
  "total_length": 160,
  "fields": [
    {
      "name": "CDEMO-FROM-TRANID",
      "level": "10",
      "offset": 0,
      "length": 4,
      "pic": "X(04)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "CICS transaction ID of the calling (source) program (e.g., 'CC00', 'CM00')"
    },
    {
      "name": "CDEMO-FROM-PROGRAM",
      "level": "10",
      "offset": 4,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Program name of the caller (e.g., 'COSGN00C', 'COMEN01C')"
    },
    {
      "name": "CDEMO-TO-TRANID",
      "level": "10",
      "offset": 12,
      "length": 4,
      "pic": "X(04)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "CICS transaction ID of the target (destination) program"
    },
    {
      "name": "CDEMO-TO-PROGRAM",
      "level": "10",
      "offset": 16,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Program name of the destination program"
    },
    {
      "name": "CDEMO-USER-ID",
      "level": "10",
      "offset": 24,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Authenticated user's login ID; matches SEC-USR-ID in CSUSR01Y security file"
    },
    {
      "name": "CDEMO-USER-TYPE",
      "level": "10",
      "offset": 32,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "User authorization level: 'A'=Admin (can access admin menu), 'U'=Regular user; controls which menu options are presented"
    },
    {
      "name": "CDEMO-PGM-CONTEXT",
      "level": "10",
      "offset": 33,
      "length": 1,
      "pic": "9(01)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Program entry state: 0=initial entry (render blank form), 1=re-entry (validate previously entered data)"
    },
    {
      "name": "CDEMO-CUST-ID",
      "level": "10",
      "offset": 34,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Customer ID of the customer currently being worked on; 0 if no customer selected"
    },
    {
      "name": "CDEMO-CUST-FNAME",
      "level": "10",
      "offset": 43,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer first name carried in commarea for display without re-reading the customer file"
    },
    {
      "name": "CDEMO-CUST-MNAME",
      "level": "10",
      "offset": 68,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer middle name for display"
    },
    {
      "name": "CDEMO-CUST-LNAME",
      "level": "10",
      "offset": 93,
      "length": 25,
      "pic": "X(25)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer last name for display"
    },
    {
      "name": "CDEMO-ACCT-ID",
      "level": "10",
      "offset": 118,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Account ID of the account currently being worked on; 0 if no account selected"
    },
    {
      "name": "CDEMO-ACCT-STATUS",
      "level": "10",
      "offset": 129,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Account status for quick display without re-reading the account file"
    },
    {
      "name": "CDEMO-CARD-NUM",
      "level": "10",
      "offset": 130,
      "length": 16,
      "pic": "9(16)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Card number currently being viewed; stored as numeric display (PIC 9, not PIC X as in CARD-NUM in CVACT02Y)"
    },
    {
      "name": "CDEMO-LAST-MAP",
      "level": "10",
      "offset": 146,
      "length": 7,
      "pic": "X(7)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "BMS map name of the last screen shown; used for back-navigation"
    },
    {
      "name": "CDEMO-LAST-MAPSET",
      "level": "10",
      "offset": 153,
      "length": 7,
      "pic": "X(7)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "BMS mapset name of the last screen shown; used for back-navigation"
    }
  ]
}
```

---

## COADM02Y

**File:** `app/cpy/COADM02Y.cpy`  
**Record:** `CARDDEMO-ADMIN-MENU-OPTIONS`  
**Type:** Constant table (Working Storage)  
**Used By:** `COADM01C.cbl` (admin menu program)

Defines the administrator menu. Hard-coded constant table of 6 active options (numbered 1–6), accessible only to users with `CDEMO-USER-TYPE = 'A'`. Each option maps a menu number to a display label and a program name to invoke.

⚠️ **WARNING — OCCURS vs. data mismatch**: `CDEMO-ADMIN-OPTIONS REDEFINES CDEMO-ADMIN-OPTIONS-DATA` declares `OCCURS 9 TIMES` but only 6 entries of data are defined in the FILLER area (6 × 45 = 270 bytes). Entries 7–9 read beyond defined memory. Programs guard against this using `CDEMO-ADMIN-OPT-COUNT` (value = 6). Never iterate beyond `OPT-COUNT`.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name                      | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                              |
| :------------------------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :---------------------------------------------------------------------------- |
| CARDDEMO-ADMIN-MENU-OPTIONS     | 01      | 0        | 272      | —     | —       | —                    | Admin menu constant table                                                     |
| CDEMO-ADMIN-OPT-COUNT           | 05      | 0        | 2        | 9(02) | DISPLAY | none                 | Number of valid admin menu options (value = 6); iterate only up to this count |
| CDEMO-ADMIN-OPTIONS-DATA        | 05      | 2        | 270      | —     | —       | —                    | Raw FILLER data for 6 menu entries (6 × 45 bytes)                             |
| CDEMO-ADMIN-OPTIONS (REDEFINES) | 05      | 2        | —        | —     | —       | —                    | Array view of menu data; OCCURS 9 TIMES (only 6 valid)                        |
| CDEMO-ADMIN-OPT-NUM             | 15      | —        | 2        | 9(02) | DISPLAY | none                 | Menu option number (1–6)                                                      |
| CDEMO-ADMIN-OPT-NAME            | 15      | —        | 35       | X(35) | DISPLAY | trailing-space-strip | Display label for this menu option                                            |
| CDEMO-ADMIN-OPT-PGMNAME         | 15      | —        | 8        | X(08) | DISPLAY | trailing-space-strip | 8-char COBOL program name to invoke for this option                           |

**Menu option values:**

| #   | Label                              | Program   |
| :-: | :--------------------------------- | :-------- |
| 1   | User List (Security)               | COUSR00C  |
| 2   | User Add (Security)                | COUSR01C  |
| 3   | User Update (Security)             | COUSR02C  |
| 4   | User Delete (Security)             | COUSR03C  |
| 5   | Transaction Type List/Update (Db2) | COTRTLIC  |
| 6   | Transaction Type Maintenance (Db2) | COTRTUPC  |

```json
{
  "copybook": "COADM02Y",
  "record": "CARDDEMO-ADMIN-MENU-OPTIONS",
  "total_length": 272,
  "note": "Constant table — OCCURS 9 declared but only 6 entries populated. Iterate only to CDEMO-ADMIN-OPT-COUNT (value=6).",
  "fields": [
    {
      "name": "CDEMO-ADMIN-OPT-COUNT",
      "level": "05",
      "offset": 0,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Count of active admin menu options; constant value 6; programs must not iterate beyond this"
    },
    {
      "name": "CDEMO-ADMIN-OPT-NUM",
      "level": "15",
      "offset": 2,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "occurs": 9,
      "description": "Sequential menu option number (1–6 for active entries); array index within CDEMO-ADMIN-OPTIONS"
    },
    {
      "name": "CDEMO-ADMIN-OPT-NAME",
      "level": "15",
      "offset": 4,
      "length": 35,
      "pic": "X(35)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "occurs": 9,
      "description": "Human-readable label for this menu option as shown on the admin screen"
    },
    {
      "name": "CDEMO-ADMIN-OPT-PGMNAME",
      "level": "15",
      "offset": 39,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "occurs": 9,
      "description": "8-character COBOL program name invoked when the user selects this menu option"
    }
  ]
}
```

---

## COMEN02Y

**File:** `app/cpy/COMEN02Y.cpy`  
**Record:** `CARDDEMO-MAIN-MENU-OPTIONS`  
**Type:** Constant table (Working Storage)  
**Used By:** `COMEN01C.cbl` (main menu program)

Defines the regular user menu (11 active options). Similar structure to COADM02Y but includes a fourth field `CDEMO-MENU-OPT-USRTYPE` indicating the minimum user type required to access each option.

⚠️ **WARNING — OCCURS vs. data mismatch**: Same issue as COADM02Y — `OCCURS 12` is declared but only 11 data entries exist. Guard using `CDEMO-MENU-OPT-COUNT` (value = 11).

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name                     | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                     |
| :----------------------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :--------------------------------------------------- |
| CARDDEMO-MAIN-MENU-OPTIONS     | 01      | 0        | 508      | —     | —       | —                    | Main menu constant table                             |
| CDEMO-MENU-OPT-COUNT           | 05      | 0        | 2        | 9(02) | DISPLAY | none                 | Number of valid menu options (value = 11)            |
| CDEMO-MENU-OPTIONS-DATA        | 05      | 2        | 506      | —     | —       | —                    | Raw FILLER data for 11 menu entries (11 × 46 bytes)  |
| CDEMO-MENU-OPTIONS (REDEFINES) | 05      | 2        | —        | —     | —       | —                    | Array view; OCCURS 12 TIMES (only 11 valid)          |
| CDEMO-MENU-OPT-NUM             | 15      | —        | 2        | 9(02) | DISPLAY | none                 | Menu option number                                   |
| CDEMO-MENU-OPT-NAME            | 15      | —        | 35       | X(35) | DISPLAY | trailing-space-strip | Display label                                        |
| CDEMO-MENU-OPT-PGMNAME         | 15      | —        | 8        | X(08) | DISPLAY | trailing-space-strip | Target program name                                  |
| CDEMO-MENU-OPT-USRTYPE         | 15      | —        | 1        | X(01) | DISPLAY | trailing-space-strip | Required user type: 'U'=regular user; 'A'=admin only |

```json
{
  "copybook": "COMEN02Y",
  "record": "CARDDEMO-MAIN-MENU-OPTIONS",
  "total_length": 508,
  "note": "OCCURS 12 declared but only 11 entries populated. Iterate only to CDEMO-MENU-OPT-COUNT (value=11).",
  "fields": [
    {
      "name": "CDEMO-MENU-OPT-COUNT",
      "level": "05",
      "offset": 0,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Count of active main menu options; constant value 11"
    },
    {
      "name": "CDEMO-MENU-OPT-NUM",
      "level": "15",
      "offset": 2,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "occurs": 12,
      "description": "Sequential menu option number (1–11 for active entries)"
    },
    {
      "name": "CDEMO-MENU-OPT-NAME",
      "level": "15",
      "offset": 4,
      "length": 35,
      "pic": "X(35)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "occurs": 12,
      "description": "Human-readable menu label"
    },
    {
      "name": "CDEMO-MENU-OPT-PGMNAME",
      "level": "15",
      "offset": 39,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "occurs": 12,
      "description": "8-character COBOL program name invoked for this option"
    },
    {
      "name": "CDEMO-MENU-OPT-USRTYPE",
      "level": "15",
      "offset": 47,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "occurs": 12,
      "description": "Minimum user type required for this option: 'U'=regular, 'A'=admin-only"
    }
  ]
}
```

---

## CVCRD01Y

**File:** `app/cpy/CVCRD01Y.cpy`  
**Record:** `CC-WORK-AREAS`  
**Length:** 213 bytes  
**Type:** CICS Working Storage area  
**Used By:** All card-related online programs (`COCRDLIC`, `COCRDSLC`, `COCRDUPC`, etc.)

Working storage communication area for CICS card programs. Carries the last AID key pressed by the user, navigation targets, error messages, and the currently selected identifiers (account, card, customer) in both alphanumeric and numeric forms via REDEFINES.

Several fields are commented out (lines beginning with `*`) and do not exist in the compiled structure. Only active (uncommented) fields are listed below.

⚠️ **WARNING — REDEFINES on ID fields**: `CC-ACCT-ID-N`, `CC-CARD-NUM-N`, and `CC-CUST-ID-N` REDEFINE their respective PIC X fields to provide a numeric view of the same bytes. Programs use the PIC X form for input/display and the PIC 9 form for arithmetic or key construction. Both names access identical storage.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name                | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                                 |
| :------------------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :------------------------------------------------------------------------------- |
| CC-WORK-AREAS             | 01      | 0        | 213      | —     | —       | —                    | Root of card CICS working storage                                                |
| CC-WORK-AREA              | 05      | 0        | 213      | —     | —       | —                    | Working area group                                                               |
| CCARD-AID                 | 10      | 0        | 5        | X(5)  | DISPLAY | trailing-space-strip | Last AID key pressed; one of 'ENTER', 'CLEAR', 'PA1  ', 'PA2  ', 'PFK01'–'PFK12' |
| CCARD-NEXT-PROG           | 10      | 5        | 8        | X(8)  | DISPLAY | trailing-space-strip | Program name to XCTL to next                                                     |
| CCARD-NEXT-MAPSET         | 10      | 13       | 7        | X(7)  | DISPLAY | trailing-space-strip | BMS mapset to display next                                                       |
| CCARD-NEXT-MAP            | 10      | 20       | 7        | X(7)  | DISPLAY | trailing-space-strip | BMS map within mapset to display next                                            |
| CCARD-ERROR-MSG           | 10      | 27       | 75       | X(75) | DISPLAY | trailing-space-strip | Error message to display on next screen send                                     |
| CCARD-RETURN-MSG          | 10      | 102      | 75       | X(75) | DISPLAY | trailing-space-strip | Informational return message from a subordinate program                          |
| CC-ACCT-ID                | 10      | 177      | 11       | X(11) | DISPLAY | trailing-space-strip | Account ID as alphanumeric (spaces = no account selected)                        |
| CC-ACCT-ID-N (REDEFINES)  | 10      | 177      | 11       | 9(11) | DISPLAY | none                 | Numeric view of CC-ACCT-ID for key operations                                    |
| CC-CARD-NUM               | 10      | 188      | 16       | X(16) | DISPLAY | trailing-space-strip | Card number as alphanumeric (spaces = no card selected)                          |
| CC-CARD-NUM-N (REDEFINES) | 10      | 188      | 16       | 9(16) | DISPLAY | none                 | Numeric view of CC-CARD-NUM                                                      |
| CC-CUST-ID                | 10      | 204      | 9        | X(09) | DISPLAY | trailing-space-strip | Customer ID as alphanumeric (spaces = no customer selected)                      |
| CC-CUST-ID-N (REDEFINES)  | 10      | 204      | 9        | 9(9)  | DISPLAY | none                 | Numeric view of CC-CUST-ID                                                       |

```json
{
  "copybook": "CVCRD01Y",
  "record": "CC-WORK-AREAS",
  "total_length": 213,
  "fields": [
    {
      "name": "CCARD-AID",
      "level": "10",
      "offset": 0,
      "length": 5,
      "pic": "X(5)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "AID key code from last CICS RECEIVE MAP; valid values: 'ENTER', 'CLEAR', 'PA1  ', 'PA2  ', 'PFK01'–'PFK12'"
    },
    {
      "name": "CCARD-NEXT-PROG",
      "level": "10",
      "offset": 5,
      "length": 8,
      "pic": "X(8)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "8-char name of the COBOL program to transfer control to (EXEC CICS XCTL)"
    },
    {
      "name": "CCARD-NEXT-MAPSET",
      "level": "10",
      "offset": 13,
      "length": 7,
      "pic": "X(7)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "7-char BMS mapset to use on the next screen display"
    },
    {
      "name": "CCARD-NEXT-MAP",
      "level": "10",
      "offset": 20,
      "length": 7,
      "pic": "X(7)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "7-char BMS map within the mapset for the next screen display"
    },
    {
      "name": "CCARD-ERROR-MSG",
      "level": "10",
      "offset": 27,
      "length": 75,
      "pic": "X(75)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Error message to display in the error message area of the next screen"
    },
    {
      "name": "CCARD-RETURN-MSG",
      "level": "10",
      "offset": 102,
      "length": 75,
      "pic": "X(75)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Informational message returned from a linked sub-program; space = no message (88 CCARD-RETURN-MSG-OFF = LOW-VALUES)"
    },
    {
      "name": "CC-ACCT-ID",
      "level": "10",
      "offset": 177,
      "length": 11,
      "pic": "X(11)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Account ID as alphanumeric for screen display; space-filled when no account is in context; initial value = SPACES"
    },
    {
      "name": "CC-ACCT-ID-N",
      "level": "10",
      "offset": 177,
      "length": 11,
      "pic": "9(11)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "redefines": "CC-ACCT-ID",
      "description": "Numeric alias for CC-ACCT-ID; used for VSAM key operations that require a numeric type"
    },
    {
      "name": "CC-CARD-NUM",
      "level": "10",
      "offset": 188,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Card number (PAN) for screen display; space-filled when no card is in context"
    },
    {
      "name": "CC-CARD-NUM-N",
      "level": "10",
      "offset": 188,
      "length": 16,
      "pic": "9(16)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "redefines": "CC-CARD-NUM",
      "description": "Numeric alias for CC-CARD-NUM; same storage"
    },
    {
      "name": "CC-CUST-ID",
      "level": "10",
      "offset": 204,
      "length": 9,
      "pic": "X(09)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Customer ID for screen display; space-filled when no customer is in context"
    },
    {
      "name": "CC-CUST-ID-N",
      "level": "10",
      "offset": 204,
      "length": 9,
      "pic": "9(9)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "redefines": "CC-CUST-ID",
      "description": "Numeric alias for CC-CUST-ID; same storage"
    }
  ]
}
```

---

## CODATECN

**File:** `app/cpy/CODATECN.cpy`  
**Record:** `CODATECN-REC`  
**Length:** 80 bytes  
**Type:** Subroutine interface record (passed to date-conversion utility)

Interface record for the date-format conversion utility. Programs set `CODATECN-TYPE` to specify the input format (1 = YYYYMMDD, 2 = YYYY-MM-DD), populate `CODATECN-INP-DATE`, call the conversion routine, then read the result from `CODATECN-OUT-REC`. Errors are reported in `CODATECN-ERROR-MSG`.

⚠️ **WARNING — REDEFINES on both input and output date areas**: Both `CODATECN-INP-DATE` and `CODATECN-0UT-DATE` are REDEFINES targets. `CODATECN-1INP` provides compact (YYYYMMDD) view; `CODATECN-2INP` provides separated (YYYY-MM-DD) view. The same pattern applies to output. The active REDEFINES depends on `CODATECN-TYPE` / `CODATECN-OUTTYPE`.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name                | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                                   |
| :------------------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :--------------------------------------------------------------------------------- |
| CODATECN-REC              | 01      | 0        | 80       | —     | —       | —                    | Root of date conversion record                                                     |
| CODATECN-IN-REC           | 05      | 0        | 21       | —     | —       | —                    | Input side of the conversion                                                       |
| CODATECN-TYPE             | 10      | 0        | 1        | X     | DISPLAY | none                 | Input format selector: '1'=YYYYMMDD (no separators), '2'=YYYY-MM-DD (with hyphens) |
| CODATECN-INP-DATE         | 10      | 1        | 20       | X(20) | DISPLAY | date-parse           | Input date string (up to 20 chars; unused portion is spaces)                       |
| CODATECN-1INP (REDEFINES) | 10      | 1        | 20       | —     | —       | —                    | Compact input view: YYYY(4) + MM(2) + DD(2) + FILLER(12)                           |
| CODATECN-2INP (REDEFINES) | 10      | 1        | 20       | —     | —       | —                    | Separated input view: YYYY(4) + sep(1) + MM(2) + sep(1) + YY(2) + FILLER(10)       |
| CODATECN-OUT-REC          | 05      | 21       | 21       | —     | —       | —                    | Output side of the conversion                                                      |
| CODATECN-OUTTYPE          | 10      | 21       | 1        | X     | DISPLAY | none                 | Output format selector: '1'=YYYY-MM-DD, '2'=YYYYMMDD                               |
| CODATECN-0UT-DATE         | 10      | 22       | 20       | X(20) | DISPLAY | date-parse           | Converted date result string                                                       |
| CODATECN-1OUT (REDEFINES) | 10      | 22       | 20       | —     | —       | —                    | Separated output: YYYY(4) + sep(1) + MM(2) + sep(1) + DD(2) + FILLER(10)           |
| CODATECN-2OUT (REDEFINES) | 10      | 22       | 20       | —     | —       | —                    | Compact output: YYYY(4) + MM(2) + DD(2) + FILLER(12)                               |
| CODATECN-ERROR-MSG        | 05      | 42       | 38       | X(38) | DISPLAY | trailing-space-strip | Error message if conversion fails; spaces if successful                            |

```json
{
  "copybook": "CODATECN",
  "record": "CODATECN-REC",
  "total_length": 80,
  "fields": [
    {
      "name": "CODATECN-TYPE",
      "level": "10",
      "offset": 0,
      "length": 1,
      "pic": "X",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Input format: '1'=YYYYMMDD (8 chars, no separators), '2'=YYYY-MM-DD (10 chars, with hyphens)"
    },
    {
      "name": "CODATECN-INP-DATE",
      "level": "10",
      "offset": 1,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Input date string; first 8 chars used for type 1, first 10 chars for type 2; rest is spaces"
    },
    {
      "name": "CODATECN-OUTTYPE",
      "level": "10",
      "offset": 21,
      "length": 1,
      "pic": "X",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Desired output format: '1'=YYYY-MM-DD (separated), '2'=YYYYMMDD (compact)"
    },
    {
      "name": "CODATECN-0UT-DATE",
      "level": "10",
      "offset": 22,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Converted date result; same length conventions as CODATECN-INP-DATE"
    },
    {
      "name": "CODATECN-ERROR-MSG",
      "level": "05",
      "offset": 42,
      "length": 38,
      "pic": "X(38)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Diagnostic message if conversion fails; all spaces on success"
    }
  ]
}
```

---

## COTTL01Y

**File:** `app/cpy/COTTL01Y.cpy`  
**Record:** `CCDA-SCREEN-TITLE`  
**Length:** 120 bytes  
**Type:** Constant Working Storage (screen header constants)  
**Used By:** All CICS online programs for screen headers

Defines the application title constants displayed at the top of every CICS screen. The active title is `'              CardDemo                  '`. An older title `'  Credit Card Demo Application (CCDA)   '` is commented out.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name        | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                     |
| :---------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :------------------------------------------------------------------- |
| CCDA-SCREEN-TITLE | 01      | 0        | 120      | —     | —       | —                    | Root of screen title constants                                       |
| CCDA-TITLE01      | 05      | 0        | 40       | X(40) | DISPLAY | none                 | First title line: 'AWS Mainframe Modernization' centered in 40 chars |
| CCDA-TITLE02      | 05      | 40       | 40       | X(40) | DISPLAY | none                 | Second title line: 'CardDemo' centered in 40 chars                   |
| CCDA-THANK-YOU    | 05      | 80       | 40       | X(40) | DISPLAY | none                 | Sign-off message: 'Thank you for using CCDA application...'          |

```json
{
  "copybook": "COTTL01Y",
  "record": "CCDA-SCREEN-TITLE",
  "total_length": 120,
  "fields": [
    {
      "name": "CCDA-TITLE01",
      "level": "05",
      "offset": 0,
      "length": 40,
      "pic": "X(40)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Application title line 1: 'AWS Mainframe Modernization' centered in 40 characters"
    },
    {
      "name": "CCDA-TITLE02",
      "level": "05",
      "offset": 40,
      "length": 40,
      "pic": "X(40)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Application title line 2: 'CardDemo' centered in 40 characters"
    },
    {
      "name": "CCDA-THANK-YOU",
      "level": "05",
      "offset": 80,
      "length": 40,
      "pic": "X(40)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Sign-off farewell message displayed on exit screens"
    }
  ]
}
```

---

## CSDAT01Y

**File:** `app/cpy/CSDAT01Y.cpy`  
**Record:** `WS-DATE-TIME`  
**Length:** 58 bytes  
**Type:** Working Storage (date/time utility area)  
**Used By:** Batch and online programs requiring current date/time

Standard date-and-time working storage block. Programs populate `WS-CURDATE` and `WS-CURTIME` using `MOVE FUNCTION CURRENT-DATE TO WS-CURDATE-DATA`, then use the sub-fields for formatted output. The `WS-TIMESTAMP` sub-structure produces a 26-character ISO timestamp suitable for `TRAN-ORIG-TS` and `TRAN-PROC-TS`.

⚠️ **WARNING — REDEFINES throughout**: `WS-CURDATE-N REDEFINES WS-CURDATE` and `WS-CURTIME-N REDEFINES WS-CURTIME` provide 8-digit numeric views of the 8-byte date/time fields. These REDEFINES do not occupy additional bytes.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name               | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                   |
| :----------------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :------------------------------------------------- |
| WS-DATE-TIME             | 01      | 0        | 58       | —     | —       | —                    | Root of date/time working storage                  |
| WS-CURDATE-DATA          | 05      | 0        | 16       | —     | —       | —                    | Current date and time in component sub-fields      |
| WS-CURDATE               | 10      | 0        | 8        | —     | —       | —                    | Current date group (YYYYMMDD)                      |
| WS-CURDATE-YEAR          | 15      | 0        | 4        | 9(04) | DISPLAY | none                 | 4-digit current year (e.g., 2026)                  |
| WS-CURDATE-MONTH         | 15      | 4        | 2        | 9(02) | DISPLAY | none                 | 2-digit current month (01–12)                      |
| WS-CURDATE-DAY           | 15      | 6        | 2        | 9(02) | DISPLAY | none                 | 2-digit current day (01–31)                        |
| WS-CURDATE-N (REDEFINES) | 10      | 0        | 8        | 9(08) | DISPLAY | none                 | Numeric YYYYMMDD view of WS-CURDATE for arithmetic |
| WS-CURTIME               | 10      | 8        | 8        | —     | —       | —                    | Current time group (HHMMSScc)                      |
| WS-CURTIME-HOURS         | 15      | 8        | 2        | 9(02) | DISPLAY | none                 | Hour (00–23)                                       |
| WS-CURTIME-MINUTE        | 15      | 10       | 2        | 9(02) | DISPLAY | none                 | Minute (00–59)                                     |
| WS-CURTIME-SECOND        | 15      | 12       | 2        | 9(02) | DISPLAY | none                 | Second (00–59)                                     |
| WS-CURTIME-MILSEC        | 15      | 14       | 2        | 9(02) | DISPLAY | none                 | Hundredths of a second (00–99)                     |
| WS-CURTIME-N (REDEFINES) | 10      | 8        | 8        | 9(08) | DISPLAY | none                 | Numeric HHMMSScc view of WS-CURTIME                |
| WS-CURDATE-MM-DD-YY      | 05      | 16       | 8        | —     | —       | —                    | Date formatted as MM/DD/YY for US display          |
| WS-CURDATE-MM            | 10      | 16       | 2        | 9(02) | DISPLAY | none                 | Month component                                    |
| FILLER                   | 10      | 18       | 1        | X(01) | DISPLAY | none                 | '/' separator                                      |
| WS-CURDATE-DD            | 10      | 19       | 2        | 9(02) | DISPLAY | none                 | Day component                                      |
| FILLER                   | 10      | 21       | 1        | X(01) | DISPLAY | none                 | '/' separator                                      |
| WS-CURDATE-YY            | 10      | 22       | 2        | 9(02) | DISPLAY | none                 | 2-digit year (Y2K caution: displays 00–99 only)    |
| WS-CURTIME-HH-MM-SS      | 05      | 24       | 8        | —     | —       | —                    | Time formatted as HH:MM:SS for display             |
| WS-TIMESTAMP             | 05      | 32       | 26       | —     | —       | —                    | Full ISO timestamp YYYY-MM-DD HH:MM:SS.nnnnnn      |
| WS-TIMESTAMP-DT-YYYY     | 10      | 32       | 4        | 9(04) | DISPLAY | none                 | Year in timestamp                                  |
| WS-TIMESTAMP-DT-MM       | 10      | 37       | 2        | 9(02) | DISPLAY | none                 | Month in timestamp                                 |
| WS-TIMESTAMP-DT-DD       | 10      | 40       | 2        | 9(02) | DISPLAY | none                 | Day in timestamp                                   |
| WS-TIMESTAMP-TM-HH       | 10      | 43       | 2        | 9(02) | DISPLAY | none                 | Hour in timestamp                                  |
| WS-TIMESTAMP-TM-MM       | 10      | 46       | 2        | 9(02) | DISPLAY | none                 | Minute in timestamp                                |
| WS-TIMESTAMP-TM-SS       | 10      | 49       | 2        | 9(02) | DISPLAY | none                 | Second in timestamp                                |
| WS-TIMESTAMP-TM-MS6      | 10      | 52       | 6        | 9(06) | DISPLAY | none                 | Microseconds in timestamp (000000–999999)          |

```json
{
  "copybook": "CSDAT01Y",
  "record": "WS-DATE-TIME",
  "total_length": 58,
  "note": "Working storage fragment — not a file record. REDEFINES do not add bytes. WS-TIMESTAMP at offset 32 produces the 26-char ISO timestamp format used in TRAN-ORIG-TS / TRAN-PROC-TS.",
  "fields": [
    {
      "name": "WS-CURDATE-YEAR",
      "level": "15",
      "offset": 0,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current year (4 digits)"
    },
    {
      "name": "WS-CURDATE-MONTH",
      "level": "15",
      "offset": 4,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current month (01–12)"
    },
    {
      "name": "WS-CURDATE-DAY",
      "level": "15",
      "offset": 6,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current day of month (01–31)"
    },
    {
      "name": "WS-CURDATE-N",
      "level": "10",
      "offset": 0,
      "length": 8,
      "pic": "9(08)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "redefines": "WS-CURDATE",
      "description": "8-digit YYYYMMDD numeric overlay of WS-CURDATE; used for date arithmetic"
    },
    {
      "name": "WS-CURTIME-HOURS",
      "level": "15",
      "offset": 8,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current hour (00–23)"
    },
    {
      "name": "WS-CURTIME-MINUTE",
      "level": "15",
      "offset": 10,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current minute (00–59)"
    },
    {
      "name": "WS-CURTIME-SECOND",
      "level": "15",
      "offset": 12,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Current second (00–59)"
    },
    {
      "name": "WS-CURTIME-MILSEC",
      "level": "15",
      "offset": 14,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Hundredths of second (00–99) from FUNCTION CURRENT-DATE"
    },
    {
      "name": "WS-TIMESTAMP-DT-YYYY",
      "level": "10",
      "offset": 32,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Year portion of ISO timestamp"
    },
    {
      "name": "WS-TIMESTAMP-DT-MM",
      "level": "10",
      "offset": 37,
      "length": 2,
      "pic": "9(02)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Month portion of ISO timestamp (position 37 because preceding FILLER '-' occupies bytes 36, 39, 42)"
    },
    {
      "name": "WS-TIMESTAMP-TM-MS6",
      "level": "10",
      "offset": 52,
      "length": 6,
      "pic": "9(06)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Microseconds portion of timestamp (000000–999999); combined with seconds produces the .nnnnnn suffix"
    }
  ]
}
```

---

## CSMSG01Y

**File:** `app/cpy/CSMSG01Y.cpy`  
**Record:** `CCDA-COMMON-MESSAGES`  
**Length:** 100 bytes  
**Type:** Constant Working Storage  
**Used By:** All CICS online programs

Two common message constants reused across all screens for consistency.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name           | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                              |
| :------------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :-------------------------------------------- |
| CCDA-COMMON-MESSAGES | 01      | 0        | 100      | —     | —       | —                    | Root of common messages                       |
| CCDA-MSG-THANK-YOU   | 05      | 0        | 50       | X(50) | DISPLAY | none                 | 'Thank you for using CardDemo application...' |
| CCDA-MSG-INVALID-KEY | 05      | 50       | 50       | X(50) | DISPLAY | none                 | 'Invalid key pressed. Please see below...'    |

```json
{
  "copybook": "CSMSG01Y",
  "record": "CCDA-COMMON-MESSAGES",
  "total_length": 100,
  "fields": [
    {
      "name": "CCDA-MSG-THANK-YOU",
      "level": "05",
      "offset": 0,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Standard farewell message displayed when user exits the application"
    },
    {
      "name": "CCDA-MSG-INVALID-KEY",
      "level": "05",
      "offset": 50,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Standard message displayed when user presses an unrecognized function key"
    }
  ]
}
```

---

## CSMSG02Y

**File:** `app/cpy/CSMSG02Y.cpy`  
**Record:** `ABEND-DATA`  
**Length:** 134 bytes  
**Type:** Working Storage (abend/error reporting)  
**Used By:** Online programs for abend handling

Error data structure populated when a CICS program encounters a fatal error (abend). Contains a 4-char abend code (e.g., ASRA, AFCA), the name of the program that abended, a short reason, and a longer diagnostic message.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name    | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                              |
| :------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :---------------------------------------------------------------------------- |
| ABEND-DATA    | 01      | 0        | 134      | —     | —       | —                    | Root of abend error data                                                      |
| ABEND-CODE    | 05      | 0        | 4        | X(4)  | DISPLAY | trailing-space-strip | 4-char CICS abend code (e.g., 'ASRA'=addressing violation, 'AFCA'=file error) |
| ABEND-CULPRIT | 05      | 4        | 8        | X(8)  | DISPLAY | trailing-space-strip | Name of the program where the abend occurred (8-char COBOL program name)      |
| ABEND-REASON  | 05      | 12       | 50       | X(50) | DISPLAY | trailing-space-strip | Short plain-English reason for the abend                                      |
| ABEND-MSG     | 05      | 62       | 72       | X(72) | DISPLAY | trailing-space-strip | Detailed diagnostic message for logging or display on error screen            |

```json
{
  "copybook": "CSMSG02Y",
  "record": "ABEND-DATA",
  "total_length": 134,
  "fields": [
    {
      "name": "ABEND-CODE",
      "level": "05",
      "offset": 0,
      "length": 4,
      "pic": "X(4)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "CICS abend code; 4 characters (e.g., 'ASRA' = storage violation, 'AICA' = runaway task)"
    },
    {
      "name": "ABEND-CULPRIT",
      "level": "05",
      "offset": 4,
      "length": 8,
      "pic": "X(8)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Name of the COBOL program (8 chars) where the abend condition was detected"
    },
    {
      "name": "ABEND-REASON",
      "level": "05",
      "offset": 12,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Short descriptive reason for the error condition"
    },
    {
      "name": "ABEND-MSG",
      "level": "05",
      "offset": 62,
      "length": 72,
      "pic": "X(72)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Full diagnostic message up to 72 characters; suitable for display on a terminal or writing to a log"
    }
  ]
}
```

---

## CSUSR01Y

**File:** `app/cpy/CSUSR01Y.cpy`  
**Record:** `SEC-USER-DATA`  
**Length:** 80 bytes  
**VSAM Type:** KSDS — primary key `SEC-USR-ID`  
**DD Name:** `USRSEC` (inferred from context)  
**Used By:** `COSGN00C.cbl` (signon), `COUSR00C`–`COUSR03C` (user management)

Security user record. One record per authorized CardDemo user. Password (`SEC-USR-PWD`) is stored as plaintext (8 chars) — a known security deficiency of the legacy system. Do not expose this field in logs or reports. User type ('A'=admin, 'U'=regular) controls menu access throughout the application.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name     | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                                                 |
| :------------- | :------ | -------: | -------: | :---- | :------ | :------------------- | :------------------------------------------------------------------------------- |
| SEC-USER-DATA  | 01      | 0        | 80       | —     | —       | —                    | Root of the security user record                                                 |
| SEC-USR-ID     | 05      | 0        | 8        | X(08) | DISPLAY | trailing-space-strip | User login ID (up to 8 chars); primary VSAM key                                  |
| SEC-USR-FNAME  | 05      | 8        | 20       | X(20) | DISPLAY | trailing-space-strip | User first name                                                                  |
| SEC-USR-LNAME  | 05      | 28       | 20       | X(20) | DISPLAY | trailing-space-strip | User last name                                                                   |
| SEC-USR-PWD    | 05      | 48       | 8        | X(08) | DISPLAY | trailing-space-strip | Password stored as plaintext (8 chars); PII — known LATENT_BUG: should be hashed |
| SEC-USR-TYPE   | 05      | 56       | 1        | X(01) | DISPLAY | trailing-space-strip | User authorization level: 'A'=Administrator, 'U'=Regular user                    |
| SEC-USR-FILLER | 05      | 57       | 23       | X(23) | DISPLAY | none                 | Reserved padding to 80-byte record length                                        |

```json
{
  "copybook": "CSUSR01Y",
  "record": "SEC-USER-DATA",
  "total_length": 80,
  "fields": [
    {
      "name": "SEC-USR-ID",
      "level": "05",
      "offset": 0,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "User login identifier (up to 8 chars); primary VSAM key for the user security file"
    },
    {
      "name": "SEC-USR-FNAME",
      "level": "05",
      "offset": 8,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "User's first name for display in the UI"
    },
    {
      "name": "SEC-USR-LNAME",
      "level": "05",
      "offset": 28,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "User's last name for display in the UI"
    },
    {
      "name": "SEC-USR-PWD",
      "level": "05",
      "offset": 48,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "User password stored as cleartext — LATENT_BUG: plaintext password storage is a security vulnerability; do not log or expose; modern implementation would use a secure hash"
    },
    {
      "name": "SEC-USR-TYPE",
      "level": "05",
      "offset": 56,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Authorization level: 'A'=Administrator (accesses COADM02Y menu), 'U'=Regular user (accesses COMEN02Y menu)"
    },
    {
      "name": "SEC-USR-FILLER",
      "level": "05",
      "offset": 57,
      "length": 23,
      "pic": "X(23)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 80-byte fixed record length"
    }
  ]
}
```

---

## CSUTLDWY

**File:** `app/cpy/CSUTLDWY.cpy`  
**Type:** Working Storage fragment (date validation state)  
**Nesting Level:** Defined at level `10` — must be embedded inside a higher-level group in the host program  
**Used By:** Any program that COPYs this alongside `CSUTLDPY` for date validation

Working storage variables used by the date validation paragraphs in `CSUTLDPY`. This copybook is **not** a standalone 01-level structure; it begins at level 10 and must be nested inside a working-storage group in the host program.

Key sub-structures:
- `WS-EDIT-DATE-CCYYMMDD` (8 bytes): the date being validated, broken into CCYY / MM / DD
- `WS-EDIT-DATE-BINARY` (4 bytes): integer representation for calendar arithmetic
- `WS-CURRENT-DATE` (12 bytes): today's date for DOB reasonableness checks
- `WS-EDIT-DATE-FLGS` (3 bytes): three 1-byte flags (year/month/day OK or not)
- `WS-DATE-FORMAT` (8 bytes): 'YYYYMMDD' constant for CSUTLDTC call
- `WS-DATE-VALIDATION-RESULT` (80 bytes): result area from LE date services call

⚠️ **WARNING — REDEFINES throughout**: Multiple pairs of X/9 REDEFINES exist within date components (e.g., `WS-EDIT-DATE-CC` / `WS-EDIT-DATE-CC-N`). The X form is used for display/comparison; the 9 form is used for numeric arithmetic.

⚠️ **WARNING — BINARY field**: `WS-EDIT-DATE-BINARY` and `WS-CURRENT-DATE-BINARY` are `PIC S9(9) BINARY` (signed 4-byte integers). These hold the result of `FUNCTION INTEGER-OF-DATE` for date comparison arithmetic. In Java: map to `int`.

> This copybook pairs with `CSUTLDPY` (procedure division). The two must always be COPYed together. The validation routine calls `CSUTLDTC` (an external LE services program) for final date verification.

No JSON block is produced for this copybook because it is a working-storage fragment, not a record layout. The field catalog serves as the harness reference.

---

## CUSTREC

**File:** `app/cpy/CUSTREC.cpy`  
**Record:** `CUSTOMER-RECORD`  
**Record Length:** 500 bytes

This is a **near-duplicate of `CVCUS01Y`**. The structures are identical in layout, field count, PIC clauses, and byte offsets. The only difference is:

| Field in CVCUS01Y     | Field in CUSTREC    | Note                                                 |
| :-------------------- | :------------------ | :--------------------------------------------------- |
| `CUST-DOB-YYYY-MM-DD` | `CUST-DOB-YYYYMMDD` | Name differs; PIC X(10) and offset 308 are identical |

Both copybooks declare the same `CUSTOMER-RECORD` 01-level name. They **cannot be COPYed into the same compilation unit** without causing a duplicate data-name error.

`CUSTREC.cpy` appears to be an older version or an alternative view created by different tooling. Active programs use `CVCUS01Y`. `CUSTREC` was retained as a reference.

> **Veritas harness note**: Both copybooks describe the same physical file layout. The date field at offset 308 has format YYYY-MM-DD regardless of which copybook name the program uses. No normalization difference exists between the two.

Refer to [CVCUS01Y](#cvcus01y) for the complete field table and JSON. All offsets, lengths, PIC clauses, and normalization rules are identical; substitute `CUST-DOB-YYYYMMDD` for `CUST-DOB-YYYY-MM-DD` in the field name only.

---

## COSTM01

**File:** `app/cpy/COSTM01.CPY`  
**Record:** `TRNX-RECORD`  
**Record Length:** 350 bytes  
**Used By:** `CBSTM03A.CBL`, `CBSTM03B.CBL` (statement generation)

Transaction record variant used specifically by the statement generation programs. Structurally equivalent to `CVTRA05Y.TRAN-RECORD` in all byte offsets and PIC clauses, but the key is **restructured**: the composite key consists of `TRNX-CARD-NUM` (16 bytes) + `TRNX-ID` (16 bytes) rather than `TRAN-ID` alone. This enables the statement program to sort and group transactions by card number.

The remaining fields (`TRNX-REST`) mirror `CVTRA05Y.TRAN-RECORD` exactly starting at byte 32.

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name         | Level   | Offset   | Length   | PIC       | USAGE   | Normalization Rule    | Business Meaning                                                          |
| :----------------- | :------ | -------: | -------: | :-------- | :------ | :-------------------- | :------------------------------------------------------------------------ |
| TRNX-RECORD        | 01      | 0        | 350      | —         | —       | —                     | Transaction record for statement generation                               |
| TRNX-KEY           | 05      | 0        | 32       | —         | —       | —                     | Composite sort key for statement grouping                                 |
| TRNX-CARD-NUM      | 10      | 0        | 16       | X(16)     | DISPLAY | trailing-space-strip  | Card number; first key component — groups transactions by card            |
| TRNX-ID            | 10      | 16       | 16       | X(16)     | DISPLAY | trailing-space-strip  | Transaction ID; second key component                                      |
| TRNX-REST          | 05      | 32       | 318      | —         | —       | —                     | Transaction detail fields (same layout as CVTRA05Y starting at offset 32) |
| TRNX-TYPE-CD       | 10      | 32       | 2        | X(02)     | DISPLAY | trailing-space-strip  | Transaction type code                                                     |
| TRNX-CAT-CD        | 10      | 34       | 4        | 9(04)     | DISPLAY | none                  | Category code                                                             |
| TRNX-SOURCE        | 10      | 38       | 10       | X(10)     | DISPLAY | trailing-space-strip  | Origination channel                                                       |
| TRNX-DESC          | 10      | 48       | 100      | X(100)    | DISPLAY | trailing-space-strip  | Transaction description                                                   |
| TRNX-AMT           | 10      | 148      | 11       | S9(09)V99 | DISPLAY | overpunch-sign-decode | Transaction amount; 9+2 digits; negative = credit                         |
| TRNX-MERCHANT-ID   | 10      | 159      | 9        | 9(09)     | DISPLAY | none                  | Merchant identifier                                                       |
| TRNX-MERCHANT-NAME | 10      | 168      | 50       | X(50)     | DISPLAY | trailing-space-strip  | Merchant name                                                             |
| TRNX-MERCHANT-CITY | 10      | 218      | 50       | X(50)     | DISPLAY | trailing-space-strip  | Merchant city                                                             |
| TRNX-MERCHANT-ZIP  | 10      | 268      | 10       | X(10)     | DISPLAY | trailing-space-strip  | Merchant ZIP                                                              |
| TRNX-ORIG-TS       | 10      | 278      | 26       | X(26)     | DISPLAY | date-parse            | Original timestamp; YYYY-MM-DD HH:MM:SS.nnnnnn                            |
| TRNX-PROC-TS       | 10      | 304      | 26       | X(26)     | DISPLAY | date-parse            | Processing timestamp; same format                                         |
| FILLER             | 10      | 330      | 20       | X(20)     | DISPLAY | none                  | Reserved padding                                                          |

```json
{
  "copybook": "COSTM01",
  "record": "TRNX-RECORD",
  "total_length": 350,
  "note": "Statement-view of the transaction record. Key is (CARD-NUM + TRAN-ID) rather than TRAN-ID alone. Rest of record is byte-for-byte identical to CVTRA05Y starting at offset 32.",
  "fields": [
    {
      "name": "TRNX-CARD-NUM",
      "level": "10",
      "offset": 0,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Card number (PAN); first segment of composite key; enables grouping by card for statement generation"
    },
    {
      "name": "TRNX-ID",
      "level": "10",
      "offset": 16,
      "length": 16,
      "pic": "X(16)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction ID; second key segment; foreign key to CVTRA05Y.TRAN-ID"
    },
    {
      "name": "TRNX-TYPE-CD",
      "level": "10",
      "offset": 32,
      "length": 2,
      "pic": "X(02)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction type code; foreign key to CVTRA03Y"
    },
    {
      "name": "TRNX-CAT-CD",
      "level": "10",
      "offset": 34,
      "length": 4,
      "pic": "9(04)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Category code; foreign key to CVTRA04Y"
    },
    {
      "name": "TRNX-SOURCE",
      "level": "10",
      "offset": 38,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Origination channel"
    },
    {
      "name": "TRNX-DESC",
      "level": "10",
      "offset": 48,
      "length": 100,
      "pic": "X(100)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Transaction description"
    },
    {
      "name": "TRNX-AMT",
      "level": "10",
      "offset": 148,
      "length": 11,
      "pic": "S9(09)V99",
      "usage": "DISPLAY",
      "normalization": "overpunch-sign-decode",
      "scale": 2,
      "signed": true,
      "description": "Transaction amount; use BigDecimal; negative = credit/refund"
    },
    {
      "name": "TRNX-MERCHANT-ID",
      "level": "10",
      "offset": 159,
      "length": 9,
      "pic": "9(09)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Merchant identifier"
    },
    {
      "name": "TRNX-MERCHANT-NAME",
      "level": "10",
      "offset": 168,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant name"
    },
    {
      "name": "TRNX-MERCHANT-CITY",
      "level": "10",
      "offset": 218,
      "length": 50,
      "pic": "X(50)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant city"
    },
    {
      "name": "TRNX-MERCHANT-ZIP",
      "level": "10",
      "offset": 268,
      "length": 10,
      "pic": "X(10)",
      "usage": "DISPLAY",
      "normalization": "trailing-space-strip",
      "scale": 0,
      "signed": false,
      "description": "Merchant ZIP code"
    },
    {
      "name": "TRNX-ORIG-TS",
      "level": "10",
      "offset": 278,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Original transaction timestamp; format YYYY-MM-DD HH:MM:SS.nnnnnn"
    },
    {
      "name": "TRNX-PROC-TS",
      "level": "10",
      "offset": 304,
      "length": 26,
      "pic": "X(26)",
      "usage": "DISPLAY",
      "normalization": "date-parse",
      "scale": 0,
      "signed": false,
      "description": "Batch processing timestamp; same format"
    },
    {
      "name": "FILLER",
      "level": "10",
      "offset": 330,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Reserved padding to 350-byte length"
    }
  ]
}
```

---

## CSLKPCDY

**File:** `app/cpy/CSLKPCDY.cpy`  
**Type:** Validation lookup table (Working Storage constants with 88-levels)  
**Used By:** `CVCUS01Y` validation in `COACTUPC.cbl`, `COUSR01C.cbl`

This copybook defines three validation tables as 88-level condition names on minimal working-storage fields:

1. **`WS-US-PHONE-AREA-CODE-TO-EDIT` (PIC XXX)** — Two sets of valid North American area codes:  
   - `VALID-PHONE-AREA-CODE`: ~400 real NANP area codes  
   - `VALID-GENERAL-PURP-CODE`: same list minus the "easily recognizable" codes (555, 800, etc.)  
   - `VALID-EASY-RECOG-AREA-CODE`: toll-free, directory, and easily recognizable codes  

2. **`US-STATE-CODE-TO-EDIT` (PIC X(2))** — `VALID-US-STATE-CODE`: all 50 US states + DC + 5 territories (AS, GU, MP, PR, VI)

3. **`US-STATE-ZIPCODE-TO-EDIT`** — `VALID-US-STATE-ZIP-CD2-COMBO`: state + first 2 digits of ZIP (4-char key like 'TX77') for state/ZIP consistency validation

> **Migration note**: These 88-level constant tables translate naturally to Java `Set<String>` or `EnumSet` constants. In Java, hard-code as `static final Set<String>` initialized from a list literal. Do not implement as a switch statement — the list is too long.

No JSON record structure — this copybook defines no record layout. The three working-storage fields are single-value check fields used only for 88-level condition evaluation.

---

## CSSETATY

**File:** `app/cpy/CSSETATY.cpy`  
**Type:** Procedure Division code snippet  
**Used By:** Inline COPYed into online CICS programs

This copybook contains **Procedure Division code** (not data), specifically an inline screen-field error highlighter. When a field fails validation and the user is re-entering data, this snippet:
1. Checks whether a named field flag is in error or blank
2. If yes, sets the BMS field color attribute to red (DFHRED)
3. If the field is blank specifically, overlays the screen field value with `'*'`

The copybook uses parameterized substitution placeholders (`(TESTVAR1)`, `(SCRNVAR2)`, `(MAPNAME3)`) that are replaced with actual field names before COPYing (via COPY REPLACING). This is a COBOL code template, not a static include.

> **Migration note**: The Java equivalent is a helper method that sets field attribute color and optional placeholder character. Not a data structure — no JSON or table produced.

---

## CSSTRPFY

**File:** `app/cpy/CSSTRPFY.cpy`  
**Type:** Procedure Division code snippet  
**Used By:** CICS online programs for keyboard input translation

This copybook contains Procedure Division code — specifically, an `EVALUATE TRUE` block that maps CICS AID values (EIBAID) to symbolic names in the `CC-WORK-AREAS.CCARD-AID` field (defined in CVCRD01Y). It handles PF1–PF24, ENTER, CLEAR, PA1, PA2, and maps PF13–PF24 back to PF01–PF12 for consistency.

The paragraph is named `YYYY-STORE-PFKEY` and is PERFORMed from online programs after each screen receive.

> **Migration note**: Translate to a Java method `mapAidToKey(String aidValue) -> KeyPress` where `KeyPress` is a Java enum. Not a data structure — no JSON or table produced.

---

## CSUTLDPY

**File:** `app/cpy/CSUTLDPY.cpy`  
**Type:** Procedure Division code snippet (date validation routines)  
**Used By:** Programs that COPY this alongside CSUTLDWY for date editing

Contains reusable date validation paragraphs:
- `EDIT-DATE-CCYYMMDD` — entry point; validates a complete CCYYMMDD date
- `EDIT-YEAR-CCYY` — validates century and year (only 19xx and 20xx considered valid)
- `EDIT-MONTH` — validates month (1–12)
- `EDIT-DAY` — validates day (1–31, basic range)
- `EDIT-DAY-MONTH-YEAR` — cross-validates day against month and leap year
- `EDIT-DATE-LE` — final validation via external call to `CSUTLDTC` (LE date services)
- `EDIT-DATE-OF-BIRTH` — checks that a date is in the past (not future)

**Leap year logic**: If month = 2 and day = 29, divides year by 400 (if year ends in 00) or by 4 (otherwise). If remainder ≠ 0, the date is invalid. This is a simplified implementation that does not handle the century-not-divisible-by-400 case correctly for all inputs — a known **LATENT_BUG** to document.

> **Migration note**: Translate to a Java `DateValidator` class with methods corresponding to each paragraph. Use `java.time.LocalDate.of(year, month, day)` which throws `DateTimeException` on invalid dates — simpler and more correct than the COBOL implementation. Preserve the LATENT_BUG in the discrepancy register; do not silently fix it.

---

## UNUSED1Y

**File:** `app/cpy/UNUSED1Y.cpy`  
**Record:** `UNUSED-DATA`  
**Length:** 80 bytes  
**Status:** DEAD CODE — not COPYed by any active program

This copybook is identical in structure to `CSUSR01Y.SEC-USER-DATA` but uses `UNUSED-` prefixed field names. It is not referenced by any COBOL program. The filename prefix (`UNUSED`) explicitly marks it as dead code.

> **Migration note**: Do not translate. Document as dead code. Include in the discrepancy register with classification `LATENT_BUG` (unnecessary artifact that could confuse future maintainers if left).

⚠️ **JSON block below is source of truth. This table is display only — do not edit independently.**

| Field Name    | Level   | Offset   | Length   | PIC   | USAGE   | Normalization Rule   | Business Meaning                                              |
| :------------ | :------ | -------: | -------: | :---- | :------ | :------------------- | :------------------------------------------------------------ |
| UNUSED-DATA   | 01      | 0        | 80       | —     | —       | —                    | Unused mirror of SEC-USER-DATA; not referenced by any program |
| UNUSED-ID     | 05      | 0        | 8        | X(08) | DISPLAY | none                 | Mirrors SEC-USR-ID — not in use                               |
| UNUSED-FNAME  | 05      | 8        | 20       | X(20) | DISPLAY | none                 | Mirrors SEC-USR-FNAME — not in use                            |
| UNUSED-LNAME  | 05      | 28       | 20       | X(20) | DISPLAY | none                 | Mirrors SEC-USR-LNAME — not in use                            |
| UNUSED-PWD    | 05      | 48       | 8        | X(08) | DISPLAY | none                 | Mirrors SEC-USR-PWD — not in use                              |
| UNUSED-TYPE   | 05      | 56       | 1        | X(01) | DISPLAY | none                 | Mirrors SEC-USR-TYPE — not in use                             |
| UNUSED-FILLER | 05      | 57       | 23       | X(23) | DISPLAY | none                 | Padding — not in use                                          |

```json
{
  "copybook": "UNUSED1Y",
  "record": "UNUSED-DATA",
  "total_length": 80,
  "status": "DEAD_CODE",
  "note": "Not referenced by any active program. Structurally identical to CSUSR01Y.SEC-USER-DATA. Do not translate.",
  "fields": [
    {
      "name": "UNUSED-ID",
      "level": "05",
      "offset": 0,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code — mirrors SEC-USR-ID from CSUSR01Y"
    },
    {
      "name": "UNUSED-FNAME",
      "level": "05",
      "offset": 8,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code — mirrors SEC-USR-FNAME"
    },
    {
      "name": "UNUSED-LNAME",
      "level": "05",
      "offset": 28,
      "length": 20,
      "pic": "X(20)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code — mirrors SEC-USR-LNAME"
    },
    {
      "name": "UNUSED-PWD",
      "level": "05",
      "offset": 48,
      "length": 8,
      "pic": "X(08)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code — mirrors SEC-USR-PWD (plaintext password field)"
    },
    {
      "name": "UNUSED-TYPE",
      "level": "05",
      "offset": 56,
      "length": 1,
      "pic": "X(01)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code — mirrors SEC-USR-TYPE"
    },
    {
      "name": "UNUSED-FILLER",
      "level": "05",
      "offset": 57,
      "length": 23,
      "pic": "X(23)",
      "usage": "DISPLAY",
      "normalization": "none",
      "scale": 0,
      "signed": false,
      "description": "Dead code padding"
    }
  ]
}
```

---

## Cross-Reference: File → Copybook → FD

| VSAM DD Name        | Copybook   | Record Name         | Primary Key               | Alternate Keys   |
| :------------------ | :--------- | :------------------ | :------------------------ | :--------------- |
| ACCTFILE            | CVACT01Y   | ACCOUNT-RECORD      | ACCT-ID                   | —                |
| CARDFILE            | CVACT02Y   | CARD-RECORD         | CARD-NUM                  | —                |
| XREFFILE            | CVACT03Y   | CARD-XREF-RECORD    | XREF-CARD-NUM             | XREF-ACCT-ID     |
| CUSTFILE            | CVCUS01Y   | CUSTOMER-RECORD     | CUST-ID                   | —                |
| TCATBALF            | CVTRA01Y   | TRAN-CAT-BAL-RECORD | TRAN-CAT-KEY (composite)  | —                |
| DISCGRP             | CVTRA02Y   | DIS-GROUP-RECORD    | DIS-GROUP-KEY (composite) | —                |
| TRANSACT / TRANFILE | CVTRA05Y   | TRAN-RECORD         | TRAN-ID                   | —                |
| DALYTRAN            | CVTRA06Y   | DALYTRAN-RECORD     | — (sequential)            | —                |
| USRSEC              | CSUSR01Y   | SEC-USER-DATA       | SEC-USR-ID                | —                |

---

## Cross-Reference: Financial Field Normalization Summary

All financial amounts in this system must be treated as `java.math.BigDecimal` with explicit `RoundingMode`. The table below summarizes every financial field and the required normalization:

| Field                      | Copybook   | PIC       | USAGE   | Normalization         | Scale   |
| :------------------------- | :--------- | :-------- | :------ | :-------------------- | :-----: |
| ACCT-CURR-BAL              | CVACT01Y   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| ACCT-CREDIT-LIMIT          | CVACT01Y   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| ACCT-CASH-CREDIT-LIMIT     | CVACT01Y   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| ACCT-CURR-CYC-CREDIT       | CVACT01Y   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| ACCT-CURR-CYC-DEBIT        | CVACT01Y   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| TRAN-CAT-BAL               | CVTRA01Y   | S9(09)V99 | DISPLAY | overpunch-sign-decode | 2       |
| DIS-INT-RATE               | CVTRA02Y   | S9(04)V99 | DISPLAY | overpunch-sign-decode | 2       |
| TRAN-AMT                   | CVTRA05Y   | S9(09)V99 | DISPLAY | overpunch-sign-decode | 2       |
| DALYTRAN-AMT               | CVTRA06Y   | S9(09)V99 | DISPLAY | overpunch-sign-decode | 2       |
| TRNX-AMT                   | COSTM01    | S9(09)V99 | DISPLAY | overpunch-sign-decode | 2       |
| EXP-ACCT-CURR-BAL          | CVEXPORT   | S9(10)V99 | COMP-3  | COMP-3/BigDecimal     | 2       |
| EXP-ACCT-CASH-CREDIT-LIMIT | CVEXPORT   | S9(10)V99 | COMP-3  | COMP-3/BigDecimal     | 2       |
| EXP-ACCT-CREDIT-LIMIT      | CVEXPORT   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| EXP-ACCT-CURR-CYC-CREDIT   | CVEXPORT   | S9(10)V99 | DISPLAY | overpunch-sign-decode | 2       |
| EXP-ACCT-CURR-CYC-DEBIT    | CVEXPORT   | S9(10)V99 | COMP    | binary-decode         | 2       |
| EXP-TRAN-AMT               | CVEXPORT   | S9(09)V99 | COMP-3  | COMP-3/BigDecimal     | 2       |

> **Rounding**: Unless the COBOL source uses the `ROUNDED` keyword explicitly, use `RoundingMode.DOWN` (truncation) to match default COBOL arithmetic. Search for `ROUNDED` in `CBACT04C.cbl` and `CBTRN02C.cbl` before finalizing the harness rounding mode for each operation.
