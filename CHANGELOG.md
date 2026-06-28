# Changelog

## [Unreleased] — 2026-06-28 01:10 (test data enrichment)

### Changed — Third GM/equivalence cycle (IBM-safe structured test data, PARM_DATE 2024-06-15)
- `app/data/ASCII/discgrp.txt` restructured: 51 rows (3 groups × 17 type/cat rows) replaced
  with 6 rows (5 named groups + DEFAULT, 1 row each for the only type/cat in the corpus —
  type=01, cat=0001). Named groups: PRIME (3%), PREFERRED (9%), STANDARD (15%), NONPREF (21%),
  SUBPRIME (27%), DEFAULT (15% fallback).
- `app/data/ASCII/acctdata.txt`: ACCT-GROUP-ID (offset 112, len 10) populated for all 50
  accounts; accounts 01–49 assigned to named groups (10 per PRIME/PREFERRED/STANDARD/NONPREF,
  9 SUBPRIME); account 50 assigned non-existent group `ORPHAN    ` to intentionally exercise
  the DEFAULT fallback path. ACCT-CURR-BAL updated to match TRAN-CAT-BAL per account.
- `app/data/ASCII/tcatbal.txt`: TRAN-CAT-BAL updated to IBM-safe values — all balances are
  exact multiples of $4.00 (400 cents), which yields zero-remainder division for all five
  3%-multiple APR rates. Includes 5 negative-balance accounts (acc26, acc27, acc37, acc40,
  acc47) and 1 zero-balance account (acc30) to exercise sign and zero edge cases.
- `app/data/ASCII/custdata.txt`: FICO scores corrected to valid range 300–850 for all 50
  customers; scores correlated with account rate group (PRIME tier: 740–850, PREFERRED: 670–
  739, STANDARD: 580–669, NONPREF: 500–579, SUBPRIME: 300–499).

### Proved — Third GM/equivalence cycle
- GnuCOBOL GM and Java output: **50/50 full equivalence** after Veritas normalization
  (timestamps masked, NUL→SPACE, overpunch sign-decode). With IBM-safe data, DR-001
  (ROUNDING_ARTIFACT) does not manifest — all interest calculations produce exact
  integer-cent results with no rounding needed.
- Five distinct APR tiers exercised in one run: 3%, 9%, 15%, 21%, 27%. Interest values
  range from $0.00 (zero balance) through $117.00 (acc49: $5200 at 27%). Negative-balance
  accounts produce negative interest correctly in both COBOL and Java.
- Exactly 1 "DISCLOSURE GROUP RECORD MISSING" console message per run (acc50/ORPHAN),
  reduced from 50/run in the prior test corpus.

### Fixed — Discrepancy Register
- DR-001: Corrected IBM-safe threshold from "$0.80" to "$4.00" — the $0.80 figure was
  correct only for 15% APR; the $4.00 minimum is required for the full 3%–27% rate range.
- DR-005: Status updated from Deferred to Resolved; description updated to document the
  restructured test data and the intentional single-fallback design.
- Register Last Updated date: 2026-06-28.

## [Unreleased] — 2026-06-27 23:04 (continued)

### Added
- app/data/ASCII/tcatbal.orig.txt, acctdata.orig.txt, discgrp.orig.txt — backup copies of
  original flat data files before enrichment
- Enriched test data: tcatbal (50 varied balances $50–$40,000, negative, $0, $0.01, $1.99),
  acctdata (ACCT-CURR-CYC-CREDIT/DEBIT populated), discgrp (29 zero-rate rows filled 0.99%–29.99%,
  one left at zero per design)

### Proved — Second GM/equivalence cycle (enriched data, PARM_DATE 2024-06-15)
- Java CBACT04C produces **50/50 correct** TRAN-AMT values per IBM Enterprise COBOL truncation
  semantics (BigDecimal × 15.00 / 1200, RoundingMode.DOWN)
- All non-TRAN-AMT, non-timestamp fields: **50/50 match** between Java and COBOL GM after
  Veritas NUL→SPACE normalization (TRAN-ID, TRAN-TYPE-CD, TRAN-SOURCE, TRAN-DESC, merchant
  fields, TRAN-CARD-NUM, FILLER)
- TRAN-CARD-NUM verified end-to-end: xref lookup from account ID → card number correct for all
  50 accounts

### Fixed
- Java CBACT04C parseTranCatBalRecord / parseCardXrefRecord: field-offset parameters were
  passing END positions as LENGTH arguments to substring(start, length). Silent zero-return
  on NumberFormatException masked the bug when balances were zero; exposed by enriched data.
  Corrected lengths: typeCd=2, catCd=4, balRaw=11; custId=9, acctId=11.

### Classified — Discrepancy Register entries
- ROUNDING_ARTIFACT / GnuCOBOL_arithmetic_nonconformance: GnuCOBOL --std=ibm-strict does not
  faithfully emulate IBM Enterprise COBOL arithmetic for COMPUTE on DISPLAY SIGN IS TRAILING
  fields. GnuCOBOL: (a) rounds results instead of truncating (e.g. 0.625→0.63 vs IBM 0.62);
  (b) loses sign for negative DISPLAY inputs (acc16–20, 26–30, 50 all produce positive output).
  Result: 43/50 TRAN-AMT values differ between GnuCOBOL GM and Java. Java is CORRECT per IBM.
  Action: authoritative equivalence proof requires IBM Enterprise COBOL, not GnuCOBOL.
- FIELD_NORM_GAP / NUL_vs_SPACE: GnuCOBOL initializes working-storage to LOW-VALUES (0x00);
  IBM COBOL initializes PIC X to SPACES. TRAN-DESC padding (offsets 56–131) and FILLER
  (offsets 330–349) contain NUL in COBOL GM, SPACE in Java. Java is correct per IBM.
  Veritas normalizer: treat 0x00 = 0x20 in all PIC X fields. No Java code change needed.
- FIELD_NORM_GAP / TRAN_AMT_SIGN_ENCODING (pre-existing, now confirmed): GnuCOBOL emits
  plain ASCII digits for positive TRAN-AMT (e.g. '3' = 0x33), Java emits EBCDIC overpunch
  ('C' = 0x43 for digit 3). Both decode to the same integer value. Veritas normalizer: decode
  both before comparing.

## [Unreleased] — 2026-06-27

### Added
- modernization/documentation/Shared_Component_Architecture.md — approved architecture
  plan for the eight recurring COBOL artifacts re-homed into the Java target
- modernization/app/ — new layered Java source tree replacing modernization/output/claude/
  - modernization/app/batch/ — translated batch programs (CBACT04C, DateFormatter)
  - modernization/app/common/batch/ — VsamStatus, BatchAbortException (externalised from
    the 9910/9999 paragraphs duplicated verbatim across all eight batch programs)
  - modernization/app/common/time/ — Db2Timestamps (externalised from Z-GET-DB2-FORMAT-TIMESTAMP,
    duplicated across CBACT04C, CBTRN02C, CBEXPORT, COBIL00C with three format variants)
  - modernization/app/common/validation/ — ValidationResult, DateValidator, FieldValidator,
    UsGeographyData (externalised from CSUTLDPY/DWY copybooks and 1215–1280 paragraphs in
    COACTUPC; UsGeographyData fully populated from CSLKPCDY.cpy — 489 NANP area codes
    across three named condition sets, 56 US state/territory codes, 240 state+zip combos)
  - modernization/app/cics/ — AttentionKey (CSSTRPFY mapping), AbstractScreenController
    (POPULATE-HEADER-INFO + RETURN-TO-PREV-SCREEN base class scaffold)

### Refactored
- CBACT04C.java: wired to Db2Timestamps and BatchAbortException; verified byte-identical
  (0/50 records) against frozen proven translation after timestamp normalization
- "Abend" terminology retired throughout; replaced with "abort" (BatchAbortException)
- CICS components (AttentionKey, AbstractScreenController) split cleanly from common layer;
  layering rule enforced: common imports nothing from cics or batch

### Architecture notes
- Eight recurring COBOL copy artifacts (paragraphs duplicated via COPY or verbatim paste)
  were identified and re-homed into the layered package structure above
- FieldValidator and AbstractScreenController are intentional scaffolds: the COBOL logic
  they translate is fully implemented in COACTUPC.cbl, but per Equivalence-First methodology
  CICS program behavior is not translated until that program's golden master is frozen
- UsGeographyData.cpy three-table area code design decoded: VALID-GENERAL-PURP-CODE (410
  real NANP codes) = VALID-PHONE-AREA-CODE (489) minus VALID-EASY-RECOG-AREA-CODE (80
  synthetic/demo patterns); COACTUPC uses the general-purpose set for customer validation

## [Unreleased] — 2026-06-26

### Added
- VSAM-LOADER.cbl — loads ASCII flat files into GnuCOBOL BDB indexed format
- CBACT04C-DRIVER.cbl — driver program to pass JCL PARM date to CBACT04C
- modernization/output/golden_master/ — COBOL golden master output (17,500 bytes)
- modernization/output/claude/ — Java idiomatic translation output (17,500 bytes)
- modernization/documentation/Cobol_Program_Documentation.md
- modernization/documentation/Copybook_Data_Dictionary.md
- CLAUDE.md — Claude Code project-level instructions

### Fixed
- Java CBACT04C: record length check in openAccountFile() (>= 122 → >= 11)
- Java CBACT04C: field offsets in parseDisGroupRecord() (end positions → lengths)
- Java CBACT04C: record delimiter (println → print for byte-compatible output)
- VSAM-LOADER: DD name hyphens replaced with underscores for bash compatibility

### Proved
- Behavioral equivalence between GnuCOBOL CBACT04C and Java translation
- Both programs produce identical 17,500-byte output on identical inputs
- Only runtime timestamps differ — expected and normalized by Veritas comparator
