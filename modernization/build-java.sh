#!/usr/bin/env bash
# =============================================================================
# build-java.sh — Compile and run the Java CBACT04C equivalence translation
#
# Usage: modernization/build-java.sh [PARM_DATE]
#        PARM_DATE defaults to 2024-06-15 if omitted.
#
# Run from the repository root or from the modernization/ directory; the script
# resolves all paths relative to its own location.
#
# Output is written to:
#   modernization/output/claude/transact_output_Java.txt
#
# All Java source files under modernization/app/ are compiled together.
# Class files are emitted into the repository root (matching the package path
# modernization/app/batch, modernization/app/common/*, etc.) — the same
# flat layout that was used in all prior equivalence runs.
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

JAVA_SRC="${REPO_ROOT}/modernization/app"
OUTPUT_DIR="${REPO_ROOT}/modernization/output/claude"
PARM_DATE="${1:-2024-06-15}"

echo "=============================================="
echo " CBACT04C — Java Equivalence Build"
echo "=============================================="
echo "  Repo root : ${REPO_ROOT}"
echo "  PARM_DATE : ${PARM_DATE}"
echo ""

cd "${REPO_ROOT}"

# --- Step 1: Compile all Java sources --------------------------------------
echo "[1/2] Compiling Java sources under modernization/app/..."
javac -cp . \
      -d . \
      $(find "${JAVA_SRC}" -name "*.java")
echo "      Class files emitted to repo root (package path layout)"

# --- Step 2: Run CBACT04C -------------------------------------------------
# File paths resolve via environment variables (TCATBALF, XREFFILE, DISCGRP,
# ACCTFILE, TRANSACT); if unset, ddPath() falls back to app/data/ASCII/<file>.
# The defaults match the current test corpus, so no env vars need to be set
# unless you want to redirect input or output to a non-default location.
echo "[2/2] Running modernization.app.batch.CBACT04C (PARM_DATE=${PARM_DATE})..."
export TRANSACT="${OUTPUT_DIR}/transact_output_Java.txt"
java -cp . modernization.app.batch.CBACT04C "${PARM_DATE}"

echo ""
echo "=============================================="
echo " Java output written to:"
echo "   ${OUTPUT_DIR}/transact_output_Java.txt"
SIZE=$(wc -c < "${OUTPUT_DIR}/transact_output_Java.txt" | tr -d ' ')
RECS=$((SIZE / 350))
echo "   ${SIZE} bytes / ${RECS} records (350 bytes each)"
echo "=============================================="
