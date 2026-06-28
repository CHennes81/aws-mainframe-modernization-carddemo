# Changelog

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
