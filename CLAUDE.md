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
