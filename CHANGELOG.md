# Changelog

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
