# Shared Component Architecture — CardDemo Java Target

**Author:** Christopher Hennes
**Created:** 2026-06-27
**Status:** Approved — implementation in progress
**Methodology:** Equivalence-First Migration

---

## Purpose

The CardDemo COBOL corpus carries eight recurring code artifacts that appear, copied
verbatim or near-verbatim, across many programs. This document is the architectural
plan for re-homing those artifacts into a cohesive, layered Java source tree so the
generated codebase reads like idiomatic Java written by an engineer who understood the
business logic — not a mechanical paragraph-for-paragraph transliteration (JOBOL).

This document records **decisions and rationale only**. It cites the COBOL source for
every component. It does not, by itself, assert behavioral equivalence — equivalence is
proven per-program against a frozen golden master, and several components below are
scaffolded ahead of the golden master for the CICS programs they derive from. Those are
flagged explicitly.

---

## The eight artifacts

| #  | COBOL artifact                                              | Java home                                       | Layer             | Source programs                        |
|:---|:------------------------------------------------------------|:------------------------------------------------|:------------------|:---------------------------------------|
| 1  | `CSUTLDPY.cpy` + `CSUTLDWY.cpy` + `CSUTLDTC.cbl`            | `DateValidator`                                 | common.validation | COACTUPC                               |
| 2a | `9910-DISPLAY-IO-STATUS`                                    | `VsamStatus`                                    | common.batch      | 8 batch programs                       |
| 2b | `9999-ABEND-PROGRAM`                                        | `BatchAbortException`                           | common.batch      | 8 batch programs                       |
| 3  | `Z-GET-DB2-FORMAT-TIMESTAMP` / `1050-GENERATE-TIMESTAMP`    | `Db2Timestamps`                                 | common.time       | CBACT04C, CBTRN02C, CBEXPORT, COBIL00C |
| 4  | `POPULATE-HEADER-INFO`                                      | `AbstractScreenController#populateHeader`       | cics              | 12 CICS programs                       |
| 5  | `CSSTRPFY.cpy` (`YYYY-STORE-PFKEY`)                         | `AttentionKey`                                  | cics              | 5 CICS programs                        |
| 6  | `RETURN-TO-PREV-SCREEN`                                     | `AbstractScreenController#returnToPreviousScreen` | cics            | 9 CICS programs                        |
| 7  | `1215`–`1280` field edits in `COACTUPC`                     | `FieldValidator`                                | common.validation | COACTUPC                               |
| 8  | `CSLKPCDY.cpy`                                              | `UsGeographyData`                               | common.validation | COACTUPC (via #7)                      |

---

## Target source tree

The project's established convention is **package name mirrors directory path from the
repository root** (e.g. the first translation shipped as `modernization.output.claude`).
This plan keeps that convention so the existing root-level `javac` / `java` build model
continues to work unchanged.

```
modernization/app/
├── batch/                              package modernization.app.batch
│   ├── CBACT04C.java                   Interest calculation (INTCALC) — TRANSLATED & VERIFIED
│   └── DateFormatter.java              COBDATFT assembler equivalent (used by batch)
├── cics/                               package modernization.app.cics
│   ├── AttentionKey.java               #5  — EIBAID → logical key (pure, complete)
│   └── AbstractScreenController.java    #4 + #6 — screen base class (SCAFFOLD)
└── common/
    ├── validation/                     package modernization.app.common.validation
    │   ├── ValidationResult.java       shared result type (complete)
    │   ├── DateValidator.java          #1  — CCYYMMDD validation (logic, pending GM)
    │   ├── FieldValidator.java         #7  — generic field edits (SCAFFOLD)
    │   └── UsGeographyData.java        #8  — NANP/state/zip tables (SCAFFOLD)
    ├── time/                           package modernization.app.common.time
    │   └── Db2Timestamps.java          #3  — DB2 timestamp generation (complete)
    └── batch/                          package modernization.app.common.batch
        ├── VsamStatus.java             #2a — file-status formatting (complete)
        └── BatchAbortException.java     #2b — fatal termination (complete)
```

> **Production note.** Path-based package names (`modernization.app.*`) are a demo-build
> convenience, not a best practice. A production cutover would remap this tree to a Maven
> `src/main/java` layout under a real root package (`com.carddemo.{batch,cics,common.*}`).
> The package boundaries and dependency rules below are unaffected by that remapping.

---

## Layering rule (the one constraint that makes it work)

```
        cics  ───────┐
                     ├──►  common  (validation · time · batch)
        batch ───────┘
```

**`common` imports nothing from `cics` or `batch`.** This single rule is what lets the
validators and utilities serve both the online (CICS) and batch worlds. A validator takes
plain `String` / `BigDecimal` in and returns a `ValidationResult` out; it never knows
whether a 3270 terminal or a sequential file reader called it. Any component that needs
CICS runtime services (commarea, XCTL, SEND MAP) belongs in `cics`, never in `common`.

---

## Decisions and rationale, by artifact

### #1 + #7 + #8 — validation family

Items 1, 7, and 8 are placed together in `common.validation` as a **package of three
cohesive classes plus a shared result type** — not a single god-class.

- **`DateValidator` (#1)** and **`FieldValidator` (#7)** share an output shape, not
  behavior. Both replace the COBOL `WS-RETURN-MSG` / `INPUT-ERROR` flag pair. They are
  unified through a shared **return type** — `ValidationResult` — not an inheritance
  hierarchy. Static methods returning a common record matches the stateless nature of the
  COBOL edit paragraphs.
- **`UsGeographyData` (#8)** is reference *data* consumed by `FieldValidator`, not
  validation logic. The NANP area-code, US state, and state+zip tables (from
  `CSLKPCDY.cpy`) live in their own class within the same package. Same package = correct
  coupling; merging data and logic into `FieldValidator` would mix concerns.

The COBOL author already extracted the date validators into copybooks (`CSUTLDPY/DWY`)
for reuse; the field edits in `COACTUPC` follow the same pattern and belong in the same
shared layer — callable from batch and any future online screen without duplication.

### #2 — split diagnostics from control flow

`9910-DISPLAY-IO-STATUS` and `9999-ABEND-PROGRAM` are adjacent in every batch program but
are two different concerns:

- **`VsamStatus` (`common.batch`)** — formats a 2-byte file status into the CardDemo
  `FILE STATUS IS: NNNN` display string. Pure function, fully unit-testable. Faithful to
  `CBACT04C.cbl:635-648`, including the literal `NNNN` placeholder that remains in the
  output and the binary-byte expansion of `IO-STAT2` on the non-numeric branch.
- **`BatchAbortException` (`common.batch`)** — replaces the `CEE3ABD` hard abend with an
  idiomatic unchecked exception carrying the abend code (default `999`). Thrown from
  anywhere; a single top-level handler in `main` maps it to `System.exit(code)`. This
  removes the deepest, most-copied legacy idiom from every batch program.

The term **"abend"** (ABnormal END) is retired in favor of **"abort"** — the cleanest
modern equivalent. ("Fatal"/"panic" read as log levels and were rejected.)

> **Equivalence note.** The first CBACT04C translation folded I/O errors into descriptive
> `System.out` messages on the *error* path and did not emit `FILE STATUS IS: NNNN…`. The
> happy-path golden master is unaffected (no I/O error occurs), so equivalence holds. Fully
> wiring `VsamStatus` into the batch error paths and reconciling the error-path output is an
> equivalence-completion task tracked in the discrepancy register, **not** part of this
> structural refactor.

### #3 — timestamp generation → `common.time`

`Db2Timestamps.currentDb2Timestamp()` is extracted verbatim from the verified
`CBACT04C.Z-GET-DB2-FORMAT-TIMESTAMP` logic: `YYYY-MM-DD-HH.MM.SS.hh0000`, where `hh` is
hundredths of a second and `0000` is a fixed suffix (`CBACT04C.cbl:140` format comment).

Three COBOL variants diverge on sub-second precision (CBACT04C/CBTRN02C pad hundredths to
six digits with `0000`; CBEXPORT writes a literal `.00`; COBIL00C uses CICS `ASKTIME`).
`Db2Timestamps` exposes the CBACT04C-faithful method as canonical and documents the
variants in javadoc; the divergence is logged for the discrepancy register.

### #4 + #6 — screen base class (NOT lumped with #5)

`POPULATE-HEADER-INFO` (#4) and `RETURN-TO-PREV-SCREEN` (#6) both operate on shared
session state (the commarea) and per-screen output, and both are duplicated across a dozen
programs. That is a **template-method / base-class** concern, not a static-utility one.
They become `protected` methods on `AbstractScreenController`, which the translated screen
programs will extend. The XCTL transfer in #6 becomes a controller redirect.

This is a **scaffold**: no CICS program has been translated yet and the commarea / screen
DTO infrastructure does not exist. The class fixes the shape and cites the COBOL; method
bodies are filled when the first screen program's golden master is captured.

### #5 — attention key resolver (its own pure utility)

`CSSTRPFY.cpy` maps the one-byte `EIBAID` to a logical key. This is a pure, deterministic
`byte → enum` function with no session state — the opposite of #4/#6 — so it stays out of
the controller and becomes the `AttentionKey` enum plus a `resolve(byte)` method. The
PF13–PF24 → PF01–PF12 "shifted key" collapse is a business rule preserved in the mapping
and noted for the discrepancy register. Fully implemented and unit-testable now.

---

## Implementation status

| Component                  | Status                        | Basis                                                                 |
|:---------------------------|:------------------------------|:----------------------------------------------------------------------|
| `ValidationResult`         | **Complete**                  | Self-contained value type                                             |
| `Db2Timestamps`            | **Complete**                  | Extracted from verified CBACT04C                                      |
| `BatchAbortException`      | **Complete**                  | Extracted from verified CBACT04C                                      |
| `VsamStatus`               | **Complete**                  | Faithful to `CBACT04C.cbl:635-648`                                    |
| `AttentionKey`             | **Complete**                  | Deterministic `CSSTRPFY` mapping                                      |
| `DateValidator`            | **Logic complete, pending GM**| `CSUTLDPY/DWY` semantics; awaits COACTUPC golden master               |
| `FieldValidator`           | **Scaffold**                  | Signatures + citations; awaits COACTUPC golden master                 |
| `UsGeographyData`          | **Data complete**             | All 5 tables populated verbatim from `CSLKPCDY.cpy`; API methods live |
| `AbstractScreenController` | **Scaffold**                  | Awaits first CICS translation + commarea DTO                          |

**Methodology guardrail.** Per Equivalence-First, no component derived from a CICS program
is asserted equivalent until that program's golden master is frozen. `UsGeographyData` is
an exception: its content is static reference data extracted verbatim from `CSLKPCDY.cpy`
and does not depend on runtime behavior, so it is fully populated ahead of the golden master.
The remaining scaffolds (`FieldValidator`, `AbstractScreenController`) carry exact signatures
and COBOL citations so the verified implementation drops in without disturbing callers.

---

## Verification of the structural refactor

The only behavior-bearing change in this pass is wiring the **already-verified** CBACT04C
to the extracted `Db2Timestamps` and `BatchAbortException`. The refactor is proven by
recompiling the whole `modernization/app` tree, re-running CBACT04C on the sample corpus
(PARM date `2024-06-15`), and diffing its 50-record / 17,500-byte output against the
**frozen proven translation** (`modernization/output/claude/transact_output_Java.txt`),
normalizing the two 26-byte timestamp columns (`TRAN-ORIG-TS` offset 278, `TRAN-PROC-TS`
offset 304 per `CVTRA05Y.cpy`).

**Result: 0 of 50 records differ** after timestamp normalization — the relocation is
behavior-preserving. The proven translation is used as the equivalence anchor (rather than
the COBOL golden master directly) because it isolates *this refactor's* effect from any
pre-existing COBOL↔Java divergence.

> **Pre-existing discrepancy surfaced during verification (not introduced here).** Diffing
> against the COBOL golden master shows the `TRAN-AMT` field rendering positive zero as the
> overpunch byte `{` (Java) versus a plain `0` (GnuCOBOL golden master) across all 50
> records. This divergence is present in the original proven translation too; both forms
> decode to `+0.00`, so it is a Veritas `FIELD_NORM_GAP` / `FORMATTING_DIFF`. Logged here
> for the discrepancy register; it is orthogonal to the component re-architecture.
