# Discrepancy Register — CardDemo CBACT04C

**Program:** CBACT04C (Interest Calculation — INTCALC batch)
**Methodology:** Equivalence-First Migration
**Author:** Christopher Hennes
**Last updated:** 2026-06-28

---

## Purpose

This register records every divergence observed between the GnuCOBOL golden master output
and the Java translation output. Each entry is classified, root-caused, and dispositioned.
No divergence is hand-waved. The register is a living artifact: entries are added as new
test cycles reveal new divergences; existing entries are updated when resolved.

**Classification codes:**

| Code                 | Meaning                                                                                                                                                                  |
| :-----               | :--------                                                                                                                                                                |
| `ROUNDING_ARTIFACT`  | Numeric result differs because of arithmetic implementation differences (truncation vs rounding, BCD vs binary). Not a translation defect if Java matches IBM semantics. |
| `FIELD_NORM_GAP`     | Field representation differs (encoding, padding) but decoded values are identical. Veritas normalizer resolves; no code change needed.                                   |
| `FORMATTING_DIFF`    | Output format differs (spaces vs NUL, sign encoding) but content is semantically equivalent.                                                                             |
| `LATENT_BUG`         | The COBOL has a confirmed bug. Java replicates it exactly. Bug is logged here and deferred to a post-migration remediation engagement.                                   |
| `TRANSLATION_DEFECT` | Java produces a wrong value that does NOT match the COBOL oracle. Requires a Java fix.                                                                                   |
| `UNKNOWN`            | Not yet classified. Investigation pending.                                                                                                                               |

**Oracle note:** The authoritative oracle for this program is **IBM Enterprise COBOL** running
on the production mainframe. The GnuCOBOL golden master is a proxy, suitable for structural
validation (field layout, IDs, record order, string content) but not for authoritative
arithmetic validation — see DR-001 for the specific arithmetic non-conformances identified.

---

## Register

---

### DR-001 — GnuCOBOL arithmetic non-conformance: TRAN-AMT values

**Classification:** `ROUNDING_ARTIFACT`
**Status:** Documented — Java is CORRECT; GnuCOBOL is the diverging party
**Affected records:** 43 of 50 (second GM cycle, enriched test data)
**Affected field:** TRAN-AMT, offset 132, length 11, PIC S9(09)V99

#### Symptom

After full Veritas normalization (timestamps masked, NUL→SPACE, sign-encoding independent
numeric decode), 43 of 50 records show a TRAN-AMT value difference between GnuCOBOL GM
and Java output. Java values match IBM Enterprise COBOL truncation semantics exactly
(verified against the algebraic formula); GnuCOBOL values do not.

#### Root cause: rounding (all 50 positive-balance accounts)

IBM Enterprise COBOL COMPUTE without the ROUNDED keyword **truncates** intermediate results
toward zero. The COBOL formula is:

```
COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
```

With `TRAN-CAT-BAL = $50.00` (PIC S9(09)V99) and `DIS-INT-RATE = 15.00` (PIC S9(04)V99):

| System                              | Intermediate                  | Result            |
| :-------                            | :------------                 | :-------          |
| IBM COBOL (truncate toward zero)    | 50.00 × 15.00 / 1200 = 0.6250 | **$0.62**         |
| GnuCOBOL `--std=ibm-strict`         | same inputs                   | **$0.63** (wrong) |
| Java BigDecimal / RoundingMode.DOWN | same inputs                   | **$0.62** ✓       |

GnuCOBOL systematically produces results that are 1–several cents higher than the correct
IBM truncation result. The divergence grows with balance size: at $40,000 the error is
$3.66. The exact mechanism is unknown (binary FP vs BCD, or different rounding step in
GnuCOBOL's COMPUTE pipeline), but the effect is consistent and reproducible.

#### Root cause: sign loss (negative-balance accounts)

GnuCOBOL with `--std=ibm-strict` on an ASCII system does **not** correctly interpret
EBCDIC overpunch sign encoding (zone digits J–R and `}`) in DISPLAY SIGN IS TRAILING
fields when those fields are read from ASCII flat files. The compile default uses ASCII
sign encoding (0x70+digit for negative), so when GnuCOBOL reads a field like
`0000005000}` (negative $50.00 in EBCDIC overpunch), it does not decode the `}` as a
negative sign — the field is treated as positive, and the interest calculation produces
a positive result for a negative balance.

All 15 test accounts with negative balances (accounts 16–20, 26–30, 50 in the enriched
corpus) produce positive TRAN-AMT values in the GnuCOBOL GM. Java correctly produces
negative values for these accounts.

#### Java behavior

Java uses `BigDecimal` throughout with `RoundingMode.DOWN` (truncation toward zero),
matching IBM Enterprise COBOL semantics. Verification confirms Java is 50/50 correct
against the algebraic formula for all 50 accounts, positive and negative balances.

#### GnuCOBOL remediation options (without changing COBOL source)

| Option                                             | What it does                                                                                                                                                           | Likely effect                                                                  |
| :------                                            | :------------                                                                                                                                                          | :--------------                                                                |
| Add `-fsign=EBCDIC` to all compile commands        | Forces GnuCOBOL to use EBCDIC overpunch for DISPLAY sign bytes. Flag syntax: `-fsign=EBCDIC` (confirmed via `cobc --help` 3.2.0)                                       | Fixes sign loss on negative balances; applied in build scripts                 |
| Use IBM-safe test data                             | Include balances that are exact multiples of $4.00 (400 cents) with rates that are multiples of 3% — these produce exact integer-cent interest with no rounding needed (verified: 400 × 300 / 120000 = 1 cent, 400 × 2700 / 120000 = 9 cents exactly). Note: $0.80 works only for 15% APR; $4.00 is required for the full 3%–27% rate range used in the enriched test corpus. | Eliminates rounding divergence in the test corpus without fixing the underlying GnuCOBOL behavior |
| No arithmetic flag exists in GnuCOBOL 3.2 for this | `cobc --help` confirmed: `-farithmetic-osvs` reduces precision (OS/VS mode, wrong direction); `-fbinary-truncate` applies to COMP fields only; no DISPLAY arith flag   | No compiler switch can fix DISPLAY arithmetic rounding                         |
| Capture GM on IBM Enterprise COBOL                 | Production mainframe run with same inputs                                                                                                                              | Authoritative oracle; resolves all arithmetic questions definitively           |

The `-fsign=EBCDIC` flag is the highest-priority remediation: it fixes the sign loss
defect with a single compile flag, applied consistently to both VSAM-LOADER and CBACT04C
via the build scripts (`modernization/build-gm.sh`). Applied as of 2026-06-27.

#### Veritas normalization

The Veritas comparator decodes both sides to integer cent-values before comparing:
- GnuCOBOL: handles plain ASCII digits (0x30–0x39) and 0x70+n encoding
- Java: handles standard EBCDIC overpunch (A–I, {, J–R, })
- After decode, both sides should represent the same logical value — when they don't,
  that is the arithmetic divergence logged here.

#### Disposition

`ROUNDING_ARTIFACT`: Java is correct per IBM semantics. GnuCOBOL GM is not an
authoritative arithmetic oracle for this program. Authoritative proof awaits IBM
Enterprise COBOL validation. This register entry remains open until that validation is
performed.

---

### DR-002 — NUL vs SPACE in PIC X field padding

**Classification:** `FIELD_NORM_GAP`
**Status:** Resolved by Veritas normalizer — no Java code change needed
**Affected records:** All 50 (universal)
**Affected fields:** TRAN-DESC (offset 32–131, PIC X(100)) trailing bytes after string content;
FILLER (offset 330–349, PIC X(20))

#### Symptom

GnuCOBOL initializes working-storage to LOW-VALUES (0x00 = NUL) by default. IBM
Enterprise COBOL initializes PIC X fields to SPACES (0x20). CBACT04C's 1300-B-WRITE-TX
paragraph uses a STRING statement to write the first 24 bytes of TRAN-DESC:

```cobol
STRING 'Int. for a/c ', ACCT-ID DELIMITED BY SIZE INTO TRAN-DESC
```

The STRING statement writes 24 bytes; the remaining 76 bytes retain the working-storage
initialization value. On GnuCOBOL that is NUL (0x00); on IBM it would be SPACE (0x20).
Similarly, FILLER at the end of each TRAN-RECORD retains its initial value.

| System      | Trailing bytes in TRAN-DESC | FILLER content  |
| :-------    | :-------------------------- | :-------------- |
| GnuCOBOL GM | 0x00 (NUL)                  | 0x00 (NUL)      |
| Java output | 0x20 (SPACE)                | 0x20 (SPACE)    |
| IBM COBOL   | 0x20 (SPACE)                | 0x20 (SPACE)    |

#### Disposition

Java behavior is **correct** per IBM COBOL semantics. GnuCOBOL NUL is an artifact of
its working-storage initialization, not a reflection of mainframe behavior. No Java code
change is warranted.

Veritas normalizer rule: treat 0x00 as 0x20 in all PIC X fields before comparison. After
this normalization, TRAN-DESC and FILLER match 50/50.

---

### DR-003 — TRAN-AMT sign encoding: GnuCOBOL plain digits vs Java EBCDIC overpunch

**Classification:** `FIELD_NORM_GAP`
**Status:** Pre-existing; confirmed in second GM cycle; resolved by Veritas normalizer
**Affected records:** All 50 (positive values) — **see DR-001 for negative values**
**Affected field:** TRAN-AMT, offset 132–142 (last byte at 142 carries the sign)

#### Symptom

GnuCOBOL in ASCII mode writes plain ASCII digit bytes for positive DISPLAY SIGN IS
TRAILING fields. Java produces standard EBCDIC-to-ASCII overpunch (the convention used
on mainframes and by IBM-compatible systems):

| Last digit value  | GnuCOBOL last byte  | Java last byte  | EBCDIC convention  |
| :---------------- | :------------------ | :-------------- | :----------------- |
| 0 (positive)      | `0` (0x30)          | `{` (0x7B)      | `{`                |
| 3 (positive)      | `3` (0x33)          | `C` (0x43)      | `C`                |
| 6 (positive)      | `6` (0x36)          | `F` (0x46)      | `F`                |

Both encode the same numeric value. This is a representation difference only.

#### Disposition

Veritas normalizer rule: decode both sides to integer cent-values before comparison
(handles plain digits, EBCDIC overpunch A–I/{, and GnuCOBOL's 0x70+n encoding). After
decode, values that are numerically equal are treated as equivalent. Values that differ
after decode fall under DR-001.

---

### DR-004 — LATENT BUG: last-account interest silently dropped

**Classification:** `LATENT_BUG`
**Status:** Confirmed; replicated exactly in Java; deferred to post-migration remediation
**Affected records:** Account 50 (last in TCATBALF sort order) in each run
**Affected file:** ACCTFILE — the last account's REWRITE never executes

#### Description

CBACT04C's main loop is structured as:

```cobol
PERFORM UNTIL END-OF-FILE = 'Y'
    READ TCATBALF-FILE ...
    IF END-OF-FILE = 'N'
        ... process current record ...
        IF new account boundary:
            PERFORM 1050-UPDATE-ACCOUNT   ← flushes PREVIOUS account's interest
            ...
    ELSE
        PERFORM 1050-UPDATE-ACCOUNT       ← dead code: condition is mutually exclusive
    END-IF
END-PERFORM.
```

The PERFORM UNTIL condition (`END-OF-FILE = 'Y'`) and the inner IF (`END-OF-FILE = 'N'`)
are mutually exclusive. The ELSE branch (`PERFORM 1050-UPDATE-ACCOUNT` for the EOF case)
is **structurally unreachable**. When the last TCATBALF record is processed, the account
boundary flush for that last account never fires because there is no "next" record to
trigger the boundary check.

**Effect:** The last account processed has its WS-TOTAL-INT accumulated correctly, and
its TRAN-RECORD is written to TRANSACT, but 1050-UPDATE-ACCOUNT is never called — so
the account's ACCT-CURR-BAL is not updated in ACCTFILE. The interest for the last
account is "phantom": it appears in the transaction file but the account balance never
reflects it.

**Hyrum's Law consideration:** Downstream systems may depend on the last account's
balance NOT being updated (if they compute it independently). This bug must not be fixed
during migration; it must be surfaced to the business and fixed only after confirming no
downstream dependency exists.

#### Java replication

The Java translation replicates the bug faithfully:
- `mainProcessingLoop()` uses `while (!endOfFile)` with an inner `if (!endOfFile)` check
- The ELSE branch (`updateAccount()`) is unreachable for the same structural reason
- This is documented in the Java code as a DEAD CODE comment citing the COBOL source

#### Disposition

Per Equivalence-First methodology: this is logged, not fixed. Post-migration remediation
should include: (1) audit of downstream systems that consume ACCTFILE; (2) confirm no
system depends on the last account's balance NOT being updated; (3) fix and re-validate.

---

### DR-005 — "DISCLOSURE GROUP RECORD MISSING" console messages (test data gap)

**Classification:** `FIELD_NORM_GAP` (console output only — not in TRANSACT output)
**Status:** Resolved — test data restructured 2026-06-28; exactly 1 intentional fallback preserved
**Affected program behavior:** Previously all 50 accounts; now exactly 1 account (acc50) falls through to DEFAULT
**Affected console output:** Both COBOL and Java; not in the TRANSACT output file

#### Description

The DISPLAY statements at CBACT04C.cbl lines 418–419 fire inside `1200-GET-INTEREST-RATE`
when a DISCGRP READ returns status 23 (record not found). This is normal COBOL behavior:
when an account's specific group is absent from DISCGRP, the program gracefully falls back
to the `DEFAULT   ` group.

**Original root cause:** The 50 test accounts in `acctdata.txt` carried blank ACCT-GROUP-ID
fields (`'          '`). No group ID matched anything in `discgrp.txt`, so all 50 accounts
fired the fallback and computed interest at the DEFAULT rate (15%). The messages were
diagnostic noise indicating a data gap, not a program error.

**Resolution (2026-06-28):** The test corpus was restructured as follows:

`discgrp.txt` was redesigned from 51 rows (3 groups × 17 type/cat combinations) to 6 rows
(5 named groups + DEFAULT, each with exactly 1 row for the single type/cat combination
present in `tcatbal.txt` — type=01, cat=0001):

| Group ID     | APR    | Target FICO range | Notes                            |
| :---------   | :----  | :---------------- | :------                          |
| `PRIME     ` |  3.00% | 740–850           | Best rate; highest credit quality |
| `PREFERRED ` |  9.00% | 670–739           |                                   |
| `STANDARD  ` | 15.00% | 580–669           |                                   |
| `NONPREF   ` | 21.00% | 500–579           |                                   |
| `SUBPRIME  ` | 27.00% | 300–499           | Highest rate; lowest credit quality |
| `DEFAULT   ` | 15.00% | fallback          | Used when group ID not found      |

`acctdata.txt` was updated to set ACCT-GROUP-ID (offset 112, len 10) for all 50 accounts:
accounts 01–49 carry one of the five named group IDs; account 50 carries `ORPHAN    `
which does not exist in `discgrp.txt`, intentionally triggering the fallback path exactly
once. This preserves coverage of the DEFAULT fallback code path.

`tcatbal.txt` balances and `custdata.txt` FICO scores were updated simultaneously (see
DR-001 IBM-safe section). FICO scores were corrected to the valid 300–850 range and
correlated with the account's interest rate group (higher FICO → lower rate).

**Post-fix behavior:** Exactly 1 "DISCLOSURE GROUP RECORD MISSING" message fires per run
(for acc50). All other 49 accounts resolve their group on the first lookup. The DEFAULT
fallback path remains tested. No COBOL source was changed.

#### Disposition

Resolved. The primary DISCGRP lookup path is now exercised for 49 of 50 accounts. The
DEFAULT fallback path is still exercised for account 50. Console messages reduced from
50 to 1 per run. Full equivalence confirmed: 50/50 TRANSACT records match between
GnuCOBOL GM and Java output after Veritas normalization (2026-06-28 test cycle).

---

## Summary table

| ID     | Field                           | Classification               | Java correct?                            | Status                                 |
| :---   | :------                         | :--------------              | :-------------                           | :-------                               |
| ID     | Field                           | Classification               | Java correct?                            | Status                                             |
| :---   | :------                         | :--------------              | :-------------                           | :-------                                           |
| DR-001 | TRAN-AMT (value)                | `ROUNDING_ARTIFACT`          | ✓ Yes — GnuCOBOL diverges                | Open — IBM-safe data masks it; IBM mainframe needed |
| DR-002 | TRAN-DESC, FILLER (padding)     | `FIELD_NORM_GAP`             | ✓ Yes — Java uses SPACE (IBM behavior)   | Resolved by Veritas normalizer                     |
| DR-003 | TRAN-AMT (sign encoding)        | `FIELD_NORM_GAP`             | ✓ Equivalent — both decode to same value | Resolved by Veritas normalizer                     |
| DR-004 | ACCTFILE last-account update    | `LATENT_BUG`                 | ✓ Yes — Java replicates faithfully       | Deferred to post-migration remediation             |
| DR-005 | Console output / DISCGRP lookup | `FIELD_NORM_GAP` (test data) | N/A — not in TRANSACT output             | Resolved — test data restructured 2026-06-28       |

---

## Veritas normalization rules (as of this register)

Rules implemented or required in the Veritas comparator for CBACT04C:

1. **Timestamp masking**: mask offsets 278–303 (`TRAN-ORIG-TS`) and 304–329 (`TRAN-PROC-TS`) before comparison. (Implemented.)
2. **NUL→SPACE**: replace 0x00 with 0x20 in all PIC X fields before comparison. (DR-002.)
3. **TRAN-AMT sign-independent numeric decode**: decode both sides' TRAN-AMT to integer cent-value before comparing; handle GnuCOBOL plain digits, EBCDIC overpunch, and 0x70+n encoding. (DR-003.)

After applying all three rules and using IBM-safe test data (balances that are multiples
of $4.00 with rates that are multiples of 3%), the Java output matches the GnuCOBOL GM
**50/50 on all fields** — including TRAN-AMT values (2026-06-28 test cycle). The
GnuCOBOL arithmetic non-conformance documented in DR-001 does not manifest when all
interest calculations yield exact integer-cent results with no rounding required.

When non-IBM-safe balances are used, DR-001 divergences reappear in TRAN-AMT. IBM-safe
test data is a mitigation strategy, not a fix for the underlying GnuCOBOL behavior.
Authoritative arithmetic proof requires an IBM Enterprise COBOL run.
