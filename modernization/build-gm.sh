#!/usr/bin/env bash
# =============================================================================
# build-gm.sh — Compile and run the COBOL golden master for CBACT04C
#
# Usage: modernization/build-gm.sh
#
# Run from the repository root or from the modernization/ directory; the script
# resolves all paths relative to its own location.
#
# Processing date is set in app/cbl/CBACT04C-DRIVER.cbl (PARM-DATE VALUE clause).
# To change the processing date, edit that file and re-run this script.
# Currently: 2024-06-15
#
# Compiler flags:
#   --std=ibm-strict   IBM COBOL syntax/dialect compatibility
#   -fsign=EBCDIC      Interpret DISPLAY SIGN IS TRAILING fields using EBCDIC
#                      overpunch zone encoding (A–I/{/J–R/}).  Without this flag,
#                      GnuCOBOL on an ASCII host uses a different default encoding
#                      (0x70+digit for negative) and silently misreads the sign
#                      byte in our flat ASCII data files, treating all negative
#                      balances as positive.
#
# No flag improves arithmetic truncation to match IBM Enterprise COBOL behavior:
#   -farithmetic-osvs  WRONG DIRECTION — limits intermediate precision (OS/VS mode)
#   -fbinary-truncate  BINARY/COMP fields only; has no effect on DISPLAY arithmetic
#   -fno-trunc         Disables truncation entirely (non-ANSI) — wrong direction
# The rounding divergence in TRAN-AMT (DR-001) cannot be corrected by a compile
# flag; it is baked into libcob's DISPLAY arithmetic pipeline.  See:
#   modernization/documentation/Discrepancy_Register.md — DR-001
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

COBOL_SRC="${REPO_ROOT}/app/cbl"
CPY_DIR="${REPO_ROOT}/app/cpy"
GM_DIR="${REPO_ROOT}/modernization/output/golden_master"
VSAM_DIR="${REPO_ROOT}/app/data/ASCII/VSAM"
DATA_DIR="${REPO_ROOT}/app/data/ASCII"

COBC_FLAGS="--std=ibm-strict -fsign=EBCDIC -I ${CPY_DIR}"

echo "=============================================="
echo " CBACT04C — COBOL Golden Master Build"
echo "=============================================="
echo "  Repo root : ${REPO_ROOT}"
echo "  COBC flags: ${COBC_FLAGS}"
echo ""

# --- Step 1: Compile VSAM-LOADER -------------------------------------------
echo "[1/4] Compiling VSAM-LOADER..."
cobc -x ${COBC_FLAGS} \
     -o "${GM_DIR}/vsam-loader" \
     "${COBOL_SRC}/VSAM-LOADER.cbl"
echo "      -> ${GM_DIR}/vsam-loader"

# --- Step 2: Compile CBACT04C as a dynamic module --------------------------
# CBACT04C-DRIVER calls CBACT04C via COBOL CALL, so CBACT04C must be compiled
# as a dynamically loadable module (.dylib on macOS, .so on Linux).
echo "[2/4] Compiling CBACT04C (dynamic module)..."
cobc -m ${COBC_FLAGS} \
     -o "${GM_DIR}/CBACT04C" \
     "${COBOL_SRC}/CBACT04C.cbl"
echo "      -> ${GM_DIR}/CBACT04C.dylib / .so"

# --- Step 3: Compile CBACT04C-DRIVER as executable -------------------------
echo "[3/4] Compiling CBACT04C-DRIVER (executable)..."
cobc -x ${COBC_FLAGS} \
     -o "${GM_DIR}/cbact04c" \
     "${COBOL_SRC}/CBACT04C-DRIVER.cbl"
echo "      -> ${GM_DIR}/cbact04c"

# --- Step 4: Load flat ASCII files into BDB indexed files ------------------
# Remove stale indexed files first — VSAM-LOADER opens them with OUTPUT mode
# but BDB may fail on corrupt/partial files from a prior interrupted run.
echo "[4/4] Loading indexed files via VSAM-LOADER..."
rm -f "${VSAM_DIR}/tcatbal"   \
      "${VSAM_DIR}/cardxref"  \
      "${VSAM_DIR}/cardxref.1" \
      "${VSAM_DIR}/acctdata"  \
      "${VSAM_DIR}/discgrp"

export TCATBAL_FLAT="${DATA_DIR}/tcatbal.txt"
export XREF_FLAT="${DATA_DIR}/cardxref.txt"
export ACCT_FLAT="${DATA_DIR}/acctdata.txt"
export DISCGRP_FLAT="${DATA_DIR}/discgrp.txt"
export TCATBALF="${VSAM_DIR}/tcatbal"
export XREFFILE="${VSAM_DIR}/cardxref"
export ACCTFILE="${VSAM_DIR}/acctdata"
export DISCGRP="${VSAM_DIR}/discgrp"

export COB_LIBRARY_PATH="${GM_DIR}"

"${GM_DIR}/vsam-loader"
echo "      Indexed files written to ${VSAM_DIR}/"

# --- Step 5: Run CBACT04C-DRIVER to produce the golden master --------------
# CBACT04C REWRITEs ACCTFILE (updating account balances), so the indexed files
# are mutated by this run.  Re-run Step 4 before Step 5 if you need a clean
# ACCTFILE (e.g., for a second run on the same data).
echo "[5/5] Running CBACT04C (see CBACT04C-DRIVER.cbl for PARM_DATE)..."
export TRANSACT="${GM_DIR}/transact_output_GM.txt"

"${GM_DIR}/cbact04c"

echo ""
echo "=============================================="
echo " Golden master written to:"
echo "   ${GM_DIR}/transact_output_GM.txt"
SIZE=$(wc -c < "${GM_DIR}/transact_output_GM.txt" | tr -d ' ')
RECS=$((SIZE / 350))
echo "   ${SIZE} bytes / ${RECS} records (350 bytes each)"
echo "=============================================="
