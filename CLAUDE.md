# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CardDemo is a mainframe credit card management application built in COBOL/CICS/JCL, designed to demonstrate AWS mainframe migration and modernization scenarios. It intentionally incorporates varied coding styles to exercise analysis and transformation tooling.

## Compiling COBOL Programs

### Local compilation (GnuCOBOL)

The compile scripts were written to run from within `app/cbl/` (using `../cpy/`), but the equivalent command from the project root is:

```bash
cobc -I app/cpy/ -T test.txt -tsymbols --std=ibm-strict app/cbl/YOURPROG.cbl
```

The `-I app/cpy/` flag is required to resolve copybook includes. For programs in optional modules, also add their copybook paths (e.g., `-I app/app-transaction-type-db2/cpy/`). Note: this targets GnuCOBOL for local syntax checking only — production compilation runs on the mainframe.

### Remote compilation (mainframe via FTP tunnel)

Requires an FTP tunnel on port 2121 (typically SSH-forwarded). The script substitutes the program name into a JCL template and submits it via JES:

```bash
scripts/remote_compile.sh <filename.cbl> .cbl <PROGNAME>
```

## Repository Structure

All application source lives under `app/`:

| Directory | Contents |
|:----------|:---------|
| `app/cbl/` | Main COBOL programs (online CICS + batch) |
| `app/bms/` | BMS map definitions (screen layouts) |
| `app/cpy/` | Shared copybooks (data structures) |
| `app/cpy-bms/` | Copybooks generated from BMS maps |
| `app/jcl/` | JCL batch jobs |
| `app/proc/` | JCL procedures (`REPROC.prc`, `TRANREPT.prc`) |
| `app/asm/` | Assembler modules (`COBDATFT`, `MVSWAIT`) |
| `app/csd/` | CICS resource definitions |
| `app/data/ASCII/` | Sample data in ASCII format (for local testing) |
| `app/data/EBCDIC/` | Sample data in EBCDIC format (for mainframe upload) |
| `app/catlg/` | Scheduler definitions (CA7, Control-M) and LISTCAT |

### Optional Extension Modules

Each optional module has its own subdirectory under `app/` with a similar layout that varies by type — DB2 modules include `bms/`, `cpy/`, `jcl/`, `ddl/`, and `dcl/`; simpler MQ-only modules may only have `cbl/` and `csd/`:

- `app/app-authorization-ims-db2-mq/` — Credit card authorizations using IMS, DB2, and MQ
- `app/app-transaction-type-db2/` — Transaction type management using DB2
- `app/app-vsam-mq/` — Account extraction via MQ (transactions CDRD, CDRA)

## Naming Conventions

CICS online programs follow a consistent three-part naming pattern:

- **Transaction ID** (4 chars): e.g., `CC00`, `CAVW`, `CTLI`
- **BMS Map** (7 chars): e.g., `COSGN00`, `COACTVW`, `COTRTLI`
- **COBOL Program** (8 chars, suffix `C`): e.g., `COSGN00C`, `COACTVWC`, `COTRTLIC`

Batch programs use a `CB` prefix (e.g., `CBTRN02C`, `CBACT04C`, `CBSTM03A`).

Copybooks use these prefixes to indicate their domain:
- `CV` — VSAM file layouts (accounts, cards, transactions, etc.)
- `CS` — Common/shared structures (messages, dates, utilities)
- `CO` — Online screen communication areas

## Application Architecture

### Online (CICS) Flow

1. User starts with transaction `CC00` → program `COSGN00C` (signon)
2. Authenticated regular users go to `CM00` → `COMEN01C` (main menu)
3. Admin users can access `CA00` → `COADM01C` (admin menu)
4. Each screen transaction is a self-contained CICS program that reads/writes VSAM files directly

### Data Storage

Core data is VSAM KSDS with alternate indexes (AIX):
- Account master, Card master, Customer master, Cross-reference (Card↔Account↔Customer)
- Transaction file (online VSAM + daily flat file for batch posting)
- Reference data: Disclosure groups, transaction categories, transaction types

### Batch Processing Order

The full batch cycle runs in this dependency order:
1. `CLOSEFIL` → close CICS files
2. Load/refresh VSAM masters (`ACCTFILE`, `CARDFILE`, `CUSTFILE`, `XREFFILE`)
3. `POSTTRAN` (`CBTRN02C`) — core transaction posting
4. `INTCALC` (`CBACT04C`) — interest calculation
5. `COMBTRAN` → combine daily and system transactions
6. `CREASTMT` (`CBSTM03A`) → produce statements
7. `OPENFIL` → reopen files for CICS

### VS Code Setup

The `.vscode/settings.json` configures the Broadcom COBOL Language Support extension with the local copybook paths — copybook resolution for intellisense uses `app/cpy/` and `app/cpy-bms/`.

*** User-Provided Instructions ***

What This Project Is

This is a professional COBOL modernization demo project built by an experienced software architect
(Sultan / Christopher Hennes) to demonstrate the Equivalence-First Migration methodology — a
rigorous, safety-first approach to migrating legacy mainframe COBOL systems to modern Java.

The codebase is the AWS CardDemo — a credit card / accounts-receivable application written in
IBM Enterprise COBOL with JCL, VSAM files, CICS screens, and embedded assembler (HLASM) routines.
It is used here as a realistic migration corpus, not a toy example.

The primary migration target is Java (idiomatic, maintainable Java — not JOBOL).
Secondary target is C#/.NET. JCL batch jobs migrate to Java/shell/scheduler equivalents.

The Equivalence-First Methodology (The Core Principle):

Behavioral equivalence is the central objective — not code translation.

The legacy COBOL system is the oracle. The Java translation must produce identical outputs for
identical inputs, field-for-field, before any cutover is considered. This is proven, not assumed.

The Migration Arc (follow this sequence strictly):

Dependency mapping — identify all files, copybooks, called programs, JCL DD statements
Business logic documentation — plain-English description of what the program does
Golden master capture — run the COBOL on a fixed input corpus; freeze outputs as canonical reference
Mechanical baseline — run opensourcecobol4j to produce a Java baseline (JOBOL — useful for contrast, not the deliverable)
Idiomatic Java translation — Claude Code + human judgment produces clean, maintainable Java
Normalized diff — compare both Java outputs against the golden master using the Veritas harness
Discrepancy register — classify every divergence; nothing is hand-waved
Write-up — document the methodology, the decisions, and the results

Do not skip steps. Do not reorder steps. The sequence is the methodology.


The Veritas Harness

COBOL Veritas is the field-aware normalized diff engine built for this project.
It is NOT a byte comparator. It normalizes field-by-field before comparing, handling:

COMP-3 / packed decimal → java.math.BigDecimal (never double for financial fields)
EBCDIC overpunch sign encoding on PIC S9 display fields
Trailing space normalization on PIC X fields (fixed-width COBOL vs Java strings)
COBOL ROUNDED vs truncation → RoundingMode.DOWN for default COBOL arithmetic;
RoundingMode.HALF_UP only where the COBOL source explicitly uses the ROUNDED keyword
Numeric display formatting (PIC edit clauses: zero suppression, sign placement, BLANK WHEN ZERO)
Date field representations (YYYYMMDD, YYMMDD, Julian YYDDD, pivot-year Y2K logic)

The harness produces a discrepancy register classifying every divergence:
ROUNDING_ARTIFACT / FORMATTING_DIFF / FIELD_NORM_GAP / LATENT_BUG /
TRANSLATION_DEFECT / UNKNOWN

The Veritas harness is implemented in Java. It is a separate, independent codebase from the
translated programs — this independence is intentional and critical to methodology credibility.

What You Will Be Asked To Do:

Analysis Tasks

Read COBOL source and copybooks — understand structure, data flows, file relationships
Identify all COPY statements and resolve copybook dependencies
Map JCL DD statements to their corresponding copybook record layouts
Identify COMP-3, PIC S9, PIC X, and date fields in copybooks — flag each for the normalizer
Identify ROUNDED keyword usage in all arithmetic operations — flag each explicitly
Identify dead code — paragraphs never PERFORMed; conditions never reached
Identify REDEFINES, OCCURS, OCCURS DEPENDING ON — flag as complexity hotspots
Identify assembler (HLASM) calls from COBOL — these are migration blockers requiring special handling
Identify JCL PARM parameters that affect program behavior (especially dates)
Map paragraph call graphs — which paragraphs PERFORM which others; identify the main loop

Documentation Tasks

Business logic documentation — plain-English description of program purpose, inputs, outputs,
processing flow, and edge cases. Write for a business analyst who has never seen COBOL.
Use section headers, bullet points, and plain language. No COBOL syntax in the output.
Copybook field inventory — for each output record type: field name, byte offset, length,
PIC clause, USAGE, normalization rule required
Date dependency documentation — every date field, its format, its source (JCL PARM, system
date, computed), and its role in the business logic
Discrepancy register entries — when diffing, classify and document every divergence

Translation Tasks

Produce idiomatic Java — not JOBOL. Target code should look like it was written by a
Java developer who understood the business logic, not a mechanical translator.
Use java.math.BigDecimal for all financial arithmetic — never double or float
Match COBOL rounding behavior explicitly — document the rounding mode chosen for every
arithmetic operation and cite the COBOL source that justified the choice
Map COBOL file I/O to Java equivalents — VSAM KSDS → appropriate Java I/O or in-memory
structure for the demo; document what a production implementation would use
Map JCL PARM parameters to Java command-line arguments or configuration
Preserve bug-for-bug compatibility during initial translation — do NOT fix COBOL bugs
during the migration pass. Log them in the discrepancy register as LATENT_BUG.
Fixing bugs is a separate, post-migration engagement.
Flag JOBOL patterns if you catch yourself producing them — deeply nested IFs that mirror
COBOL paragraph structure, procedural loops that should be streams, etc. Refactor them.

Harness Tasks (Veritas)

Generate test input records conforming to the copybook layout — cover normal cases,
boundary cases, and edge cases (zero balances, maximum values, negative amounts, leap-year dates)
Implement field parsers for each record type, driven by the copybook field inventory
Implement normalizers for each field type (see The Veritas Harness above)
Implement the comparator — field-by-field, normalized, zero-tolerance for financial fields
Generate the discrepancy register — CSV or JSON output, one row per divergence
Generate the summary report — records compared, fields compared, divergences by classification

Code Quality Standards

Java target version: Java 17 minimum (use records, sealed classes, pattern matching where appropriate)
No magic numbers — every COBOL field offset, length, or format constant must be a named constant
with a comment citing the copybook source (e.g., // CVTRA01Y.cpy, line 45)
Immutable value objects for record types where possible
BigDecimal arithmetic only for any field derived from a COBOL numeric field
Explicit RoundingMode on every BigDecimal divide or scale operation — never implicit
Unit tests for every normalizer method — these are the correctness proof for the harness
Comments cite the COBOL source — when translating a paragraph, include a comment with the
source paragraph name and line range (e.g., // CBACT04C.cbl 1300-COMPUTE-INTEREST L450-487)

What To Avoid

Do not produce JOBOL — Java that is structurally COBOL with Java syntax. The translated code
must be maintainable by a Java developer who has never seen the original COBOL.
Do not use double or float for financial fields — ever. BigDecimal only.
Do not fix bugs during translation — preserve legacy behavior exactly. Log bugs, don't fix them.
Do not infer copybook formats — if a field's type or format is ambiguous, say so explicitly
and ask rather than guessing. Wrong field types produce wrong normalization.
Do not byte-compare outputs — always use the field-aware normalizer. Raw byte comparison
produces false divergences on virtually every field.
Do not skip the golden master step — translation always happens after the golden master is
frozen, never before. This is non-negotiable for methodology credibility.
Do not assume JCL is irrelevant — JCL PARM values, DD statements, and PROC parameters
affect program behavior. Read the JCL before translating the COBOL.

Project Structure (CardDemo)

aws-mainframe-modernization-carddemo/
├── app/
│   ├── cbl/          # COBOL source programs (primary target)
│   ├── cpy/          # Copybooks (record layouts, constants, status codes)
│   ├── cpy-bms/      # BMS copybooks (CICS screen maps — not primary focus)
│   ├── jcl/          # JCL job streams (read these before translating batch programs)
│   ├── proc/         # JCL procedures (reusable JCL called from job streams)
│   ├── asm/          # HLASM assembler routines (secondary — handle when encountered)
│   ├── bms/          # CICS BMS screen definitions
│   └── data/         # Sample data files
├── app-authorization-ims-db2-mq/   # IMS+DB2+MQ variant — not primary focus
├── app-transaction-type-db2/        # DB2 variant — not primary focus
└── app-vsam-mq/                     # VSAM+MQ variant — secondary reference

Primary focus: app/cbl/ and app/cpy/
Start with: CBACT04C.cbl — pure batch, no CICS, VSAM I/O, interest calculation


Key Technical Context

GnuCOBOL

The local COBOL compiler is GnuCOBOL v3.2. It does not support JCL, JES, CICS, or VSAM.
Use it to compile and run batch programs (CBACT*) only. Do not attempt to compile CICS programs.

The Five-Way Intersection (Why This Project Exists)

The author's value proposition is the rare combination of:
COBOL literacy + decades of consulting + exec/stakeholder translation + AI fluency + facilitation.
The demo must demonstrate judgment, not just translation. Anyone can run a transpiler.
The methodology, the harness, and the writeup are the proof of judgment.

Bug-for-Bug Compatibility and the Discrepancy Register

During migration, replicate legacy behavior exactly — including bugs. Downstream systems may
depend on buggy behavior (Hyrum's Law). The discrepancy register is the artifact that makes
this explicit: every divergence is logged, classified, and dispositioned. That register becomes
a second engagement (post-migration remediation). Never hide divergences.

HLASM Assembler Routines

The app/asm/ directory contains assembler routines called by COBOL programs. These are
migration blockers — they cannot be transpiled automatically and require manual reverse
engineering. When a COBOL program calls an assembler routine, flag it explicitly, document
the interface (parameters, return values, side effects), and translate it separately.
The author has S/370 assembler experience and can validate assembler analysis.

Communication Style

Direct and technical — no hedging, no over-explanation of basics
Cite sources — always reference the specific COBOL line, paragraph name, or copybook field
that justifies a translation decision
Flag uncertainty explicitly — if a behavior is ambiguous, say so and offer options
Prioritize correctness over elegance — but flag inelegant translation decisions so they
can be revisited in the refactoring pass
Most-confronting issue first — lead with blockers and risks before explaining normal flow


Project: COBOL Veritas / CardDemo Migration Demo
Methodology: Equivalence-First Migration
Author: Christopher Hennes
Created: June 2026
