package modernization.app.batch;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

import modernization.app.common.batch.BatchAbortException;
import modernization.app.common.time.Db2Timestamps;

/**
 * Interest Calculation — batch job step INTCALC
 *
 * Reads the transaction category balance file (TCATBALF) sequentially.
 * For each account group encountered, looks up the applicable annual
 * interest rate from the disclosure group file (DISCGRP), computes
 * monthly interest as (balance × annualRate) / 1200, and accumulates
 * it into a per-account running total.  When the account ID changes,
 * the accumulated interest is added to the account's current balance
 * and the account record is rewritten.
 *
 * Source:  CBACT04C.cbl, CardDemo v2.0-25-gdb72e6b
 * JCL:     INTCALC step; optional PARM= CCYYMMDD processing date
 * Files:   TCATBALF (sequential in), XREFFILE (random in),
 *          DISCGRP  (random in),     ACCTFILE (I/O random),
 *          TRANSACT (sequential out)
 *
 * -----------------------------------------------------------------------
 * LATENT_BUG — last-account interest silently dropped
 * -----------------------------------------------------------------------
 * The COBOL PERFORM UNTIL END-OF-FILE = 'Y' evaluates its condition
 * BEFORE each iteration.  When 1000-TCATBALF-GET-NEXT sets
 * END-OF-FILE = 'Y', control falls through to the bottom of the loop
 * body and the outer PERFORM UNTIL exits immediately.  The ELSE branch
 * at CBACT04C.cbl L219 that would call 1050-UPDATE-ACCOUNT for the final
 * account is therefore dead code — it is never reached.
 * Consequence: the last account's accumulated WS-TOTAL-INT is silently
 * discarded; ACCOUNT-FILE is never rewritten for that account.
 * This bug is replicated exactly per Equivalence-First methodology.
 * -----------------------------------------------------------------------
 */
public class CBACT04C {

    // -----------------------------------------------------------------------
    // File path configuration — JCL DD names map to environment variables;
    // defaults point to the ASCII sample data shipped with CardDemo.
    // -----------------------------------------------------------------------

    private static final String DEFAULT_DATA_DIR =
            "app/data/ASCII";

    private static String ddPath(String ddName, String defaultFile) {
        String env = System.getenv(ddName);
        return (env != null && !env.isBlank()) ? env
                : DEFAULT_DATA_DIR + "/" + defaultFile;
    }

    // -----------------------------------------------------------------------
    // Record types — immutable value objects per copybook layouts
    // -----------------------------------------------------------------------

    /**
     * CVTRA01Y.cpy — Transaction Category Balance Record, 50 bytes.
     * VSAM KSDS, sequential access, DD TCATBALF.
     * Record offsets: TRANCAT-ACCT-ID[0,11), TRANCAT-TYPE-CD[11,13),
     *                 TRANCAT-CD[13,17), TRAN-CAT-BAL[17,28)
     */
    record TranCatBalRecord(
            String acctId,       // TRANCAT-ACCT-ID  PIC 9(11) offset 0
            String typeCd,       // TRANCAT-TYPE-CD  PIC X(02) offset 11
            String catCd,        // TRANCAT-CD       PIC 9(04) offset 13
            BigDecimal balance   // TRAN-CAT-BAL     PIC S9(09)V99 offset 17
    ) {}

    /**
     * CVACT03Y.cpy — Card Cross-Reference Record, 50 bytes.
     * VSAM KSDS, random access by alternate key XREF-ACCT-ID, DD XREFFILE.
     * Record offsets: XREF-CARD-NUM[0,16), XREF-CUST-ID[16,25),
     *                 XREF-ACCT-ID[25,36)
     */
    record CardXrefRecord(
            String cardNum,  // XREF-CARD-NUM  PIC X(16) offset 0
            String custId,   // XREF-CUST-ID   PIC 9(09) offset 16
            String acctId    // XREF-ACCT-ID   PIC 9(11) offset 25
    ) {}

    /**
     * CVTRA02Y.cpy — Disclosure Group / Interest Rate Record, 50 bytes.
     * VSAM KSDS, random access by composite key, DD DISCGRP.
     * Record offsets: DIS-ACCT-GROUP-ID[0,10), DIS-TRAN-TYPE-CD[10,12),
     *                 DIS-TRAN-CAT-CD[12,16), DIS-INT-RATE[16,22)
     *
     * DIS-INT-RATE is PIC S9(04)V99 — raw value e.g. 1500 means 15.00% APR.
     */
    record DisGroupRecord(
            String groupId,        // DIS-ACCT-GROUP-ID  PIC X(10)     offset 0
            String typeCd,         // DIS-TRAN-TYPE-CD   PIC X(02)     offset 10
            String catCd,          // DIS-TRAN-CAT-CD    PIC 9(04)     offset 12
            BigDecimal intRate     // DIS-INT-RATE       PIC S9(04)V99 offset 16
    ) {}

    /**
     * CVACT01Y.cpy — Account Record, 300 bytes.
     * VSAM KSDS, random access and rewrite, DD ACCTFILE.
     *
     * Mutable: CBACT04C modifies ACCT-CURR-BAL, ACCT-CURR-CYC-CREDIT,
     * and ACCT-CURR-CYC-DEBIT via 1050-UPDATE-ACCOUNT.  All other fields
     * are read-only from this program's perspective.
     */
    static final class AccountRecord {

        // Field byte offsets — CVACT01Y.cpy
        static final int OFF_ACCT_ID          =   0; // PIC 9(11)     len 11
        static final int OFF_ACTIVE_STATUS    =  11; // PIC X(01)     len  1
        static final int OFF_CURR_BAL         =  12; // PIC S9(10)V99 len 12
        static final int OFF_CREDIT_LIMIT     =  24; // PIC S9(10)V99 len 12
        static final int OFF_CASH_CREDIT_LIM  =  36; // PIC S9(10)V99 len 12
        static final int OFF_OPEN_DATE        =  48; // PIC X(10)     len 10
        static final int OFF_EXPIRAION_DATE   =  58; // PIC X(10)     len 10  -- deliberate typo
        static final int OFF_REISSUE_DATE     =  68; // PIC X(10)     len 10
        static final int OFF_CURR_CYC_CREDIT  =  78; // PIC S9(10)V99 len 12
        static final int OFF_CURR_CYC_DEBIT   =  90; // PIC S9(10)V99 len 12
        static final int OFF_ADDR_ZIP         = 102; // PIC X(10)     len 10
        static final int OFF_GROUP_ID         = 112; // PIC X(10)     len 10
        // FILLER PIC X(178) at 122 — preserved verbatim in rawBytes
        static final int RECORD_LENGTH        = 300;

        final String acctId;
        final String groupId;
        BigDecimal currBal;
        BigDecimal currCycCredit;
        BigDecimal currCycDebit;
        private final byte[] rawBytes; // full 300-byte record for faithful rewrite

        AccountRecord(String acctId, String groupId,
                      BigDecimal currBal, BigDecimal currCycCredit, BigDecimal currCycDebit,
                      byte[] rawBytes) {
            this.acctId        = acctId;
            this.groupId       = groupId;
            this.currBal       = currBal;
            this.currCycCredit = currCycCredit;
            this.currCycDebit  = currCycDebit;
            this.rawBytes      = Arrays.copyOf(rawBytes, rawBytes.length);
        }

        /**
         * Serialize back to a 300-character flat record, replacing only the
         * three mutable fields.  All other bytes are preserved from the
         * original read (important for the REWRITE semantic).
         */
        String serialize() {
            byte[] out = Arrays.copyOf(rawBytes, RECORD_LENGTH);
            writeAsciiField(out, OFF_CURR_BAL,        formatSignedDisplay(currBal,        10, 2));
            writeAsciiField(out, OFF_CURR_CYC_CREDIT, formatSignedDisplay(currCycCredit,  10, 2));
            writeAsciiField(out, OFF_CURR_CYC_DEBIT,  formatSignedDisplay(currCycDebit,   10, 2));
            return new String(out, java.nio.charset.StandardCharsets.US_ASCII);
        }

        private static void writeAsciiField(byte[] buf, int offset, String value) {
            byte[] src = value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            System.arraycopy(src, 0, buf, offset, Math.min(src.length, buf.length - offset));
        }
    }

    // -----------------------------------------------------------------------
    // Program state — mirrors COBOL WORKING-STORAGE
    // -----------------------------------------------------------------------

    private boolean endOfFile    = false;                   // END-OF-FILE PIC X VALUE 'N'
    private String  lastAcctNum  = " ".repeat(11);          // WS-LAST-ACCT-NUM PIC X(11) SPACES
    private BigDecimal monthlyInt = BigDecimal.ZERO;        // WS-MONTHLY-INT PIC S9(09)V99
    private BigDecimal totalInt   = BigDecimal.ZERO;        // WS-TOTAL-INT   PIC S9(09)V99
    private boolean firstTime     = true;                   // WS-FIRST-TIME  PIC X VALUE 'Y'
    private int     recordCount   = 0;                      // WS-RECORD-COUNT PIC 9(09)
    private int     tranIdSuffix  = 0;                      // WS-TRANID-SUFFIX PIC 9(06)

    // Current working records (targets of READ INTO)
    private TranCatBalRecord  currentTcatBal;
    private CardXrefRecord    currentXref;
    private DisGroupRecord    currentDisGroup;
    private AccountRecord     currentAccount;

    // In-memory VSAM simulation (demo harness — not production VSAM)
    private Iterator<TranCatBalRecord> tcatBalIter;
    private Map<String, AccountRecord>  accountFileByAcctId;  // keyed by trimmed ACCT-ID
    private Map<String, CardXrefRecord> xrefFileByAcctId;     // keyed by trimmed XREF-ACCT-ID
    private Map<String, DisGroupRecord> discGrpByKey;         // keyed by 16-char composite key
    private Set<String>                 modifiedAcctIds;      // tracks which accounts need rewrite
    private PrintWriter                 transactWriter;

    private final String parmDate; // PARM-DATE PIC X(10) from LINKAGE SECTION

    // -----------------------------------------------------------------------
    // main — entry point
    // CBACT04C.cbl L180: PROCEDURE DIVISION USING EXTERNAL-PARMS
    // EXTERNAL-PARMS: PARM-LENGTH PIC S9(04) COMP, PARM-DATE PIC X(10)
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // JCL PARM-DATE maps to the first command-line argument.
        // The COBOL PARM is PIC X(10) — pad/truncate to exactly 10 chars.
        String rawParm = (args.length > 0 && !args[0].isBlank()) ? args[0] : "";
        String parmDate = String.format("%-10s", rawParm).substring(0, 10);
        try {
            new CBACT04C(parmDate).run();
        } catch (BatchAbortException e) {
            // 9999-ABEND-PROGRAM (CEE3ABD): map the abend code to a process exit.
            System.exit(e.code());
        }
    }

    public CBACT04C(String parmDate) {
        this.parmDate = parmDate;
    }

    // -----------------------------------------------------------------------
    // run — top-level procedure division
    // CBACT04C.cbl L181-232
    // -----------------------------------------------------------------------

    void run() {
        System.out.println("START OF EXECUTION OF PROGRAM CBACT04C");

        openTcatbalFile();   // 0000-TCATBALF-OPEN
        openXrefFile();      // 0100-XREFFILE-OPEN
        openDiscGrpFile();   // 0200-DISCGRP-OPEN
        openAccountFile();   // 0300-ACCTFILE-OPEN
        openTransactFile();  // 0400-TRANFILE-OPEN

        mainProcessingLoop();

        closeTcatbalFile();   // 9000-TCATBALF-CLOSE
        closeXrefFile();      // 9100-XREFFILE-CLOSE
        closeDiscGrpFile();   // 9200-DISCGRP-CLOSE
        closeAccountFile();   // 9300-ACCTFILE-CLOSE
        closeTransactFile();  // 9400-TRANFILE-CLOSE

        System.out.println("END OF EXECUTION OF PROGRAM CBACT04C");
    }

    // -----------------------------------------------------------------------
    // mainProcessingLoop — CBACT04C.cbl L188-222
    //
    // LATENT_BUG: The COBOL PERFORM UNTIL checks the condition BEFORE each
    // iteration (WITH TEST BEFORE is COBOL's default).  When endOfFile
    // becomes true inside getNextTcatbalRecord(), the current iteration
    // continues to its end with no record processing (the inner
    // "if (!endOfFile)" guard fails), then the outer while(!endOfFile)
    // re-evaluates and exits immediately.  The else-branch below —
    // the final updateAccount() call — is therefore unreachable dead code.
    //
    // In COBOL terms: the "ELSE PERFORM 1050-UPDATE-ACCOUNT" at L219 only
    // executes when END-OF-FILE ≠ 'N', but PERFORM UNTIL END-OF-FILE = 'Y'
    // only enters the body when END-OF-FILE = 'N'.  These conditions are
    // mutually exclusive when END-OF-FILE is strictly 'Y' or 'N'.
    //
    // Consequence: the last account's WS-TOTAL-INT is silently discarded.
    // Replicated exactly — no fix applied.
    // -----------------------------------------------------------------------

    private void mainProcessingLoop() {
        while (!endOfFile) {
            if (!endOfFile) { // always true at loop entry — ELSE below is dead code
                getNextTcatbalRecord(); // 1000-TCATBALF-GET-NEXT
                if (!endOfFile) {
                    recordCount++;
                    System.out.println(formatTranCatBalForDisplay(currentTcatBal));

                    if (!currentTcatBal.acctId().equals(lastAcctNum)) {
                        // Account boundary: flush previous account's interest
                        if (!firstTime) {
                            updateAccount(); // 1050-UPDATE-ACCOUNT
                        } else {
                            firstTime = false;
                        }
                        totalInt = BigDecimal.ZERO;
                        lastAcctNum = currentTcatBal.acctId();

                        // Load fresh account data for the new account
                        getAcctData();  // 1100-GET-ACCT-DATA
                        getXrefData();  // 1110-GET-XREF-DATA
                    }

                    // Look up this category's interest rate
                    getInterestRate(); // 1200-GET-INTEREST-RATE

                    if (currentDisGroup.intRate().compareTo(BigDecimal.ZERO) != 0) {
                        computeInterest(); // 1300-COMPUTE-INTEREST
                        computeFees();     // 1400-COMPUTE-FEES (stub — to be implemented)
                    }
                }
            } else {
                // DEAD CODE — replicated from CBACT04C.cbl L219-221.
                // This branch can never be reached: while (!endOfFile) guarantees
                // endOfFile is false when we enter the body, so the else arm of
                // "if (!endOfFile)" is structurally unreachable.
                updateAccount(); // 1050-UPDATE-ACCOUNT
            }
        }
    }

    // -----------------------------------------------------------------------
    // 0000-TCATBALF-OPEN  (CBACT04C.cbl L234-250)
    // Loads the TCATBALF flat file into a list for sequential iteration.
    // -----------------------------------------------------------------------

    private void openTcatbalFile() {
        String path = ddPath("TCATBALF", "tcatbal.txt");
        try {
            List<TranCatBalRecord> records = new ArrayList<>();
            List<String> lines = Files.readAllLines(Path.of(path));
            for (String line : lines) {
                // Strip Windows CRLF; pad to minimum record length
                String rec = line.stripTrailing();
                if (rec.length() >= 28) {
                    records.add(parseTranCatBalRecord(rec));
                }
            }
            tcatBalIter = records.iterator();
        } catch (IOException e) {
            abend("ERROR OPENING TRANSACTION CATEGORY BALANCE: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 0100-XREFFILE-OPEN  (CBACT04C.cbl L252-268)
    // Loads the XREFFILE into a HashMap keyed by XREF-ACCT-ID (alternate key).
    // -----------------------------------------------------------------------

    private void openXrefFile() {
        String path = ddPath("XREFFILE", "cardxref.txt");
        xrefFileByAcctId = new HashMap<>();
        try {
            for (String line : Files.readAllLines(Path.of(path))) {
                String rec = line.stripTrailing();
                if (rec.length() >= 36) {
                    CardXrefRecord xref = parseCardXrefRecord(rec);
                    // Keyed by XREF-ACCT-ID (alternate key per FD-XREF-ACCT-ID)
                    xrefFileByAcctId.put(xref.acctId().trim(), xref);
                }
            }
        } catch (IOException e) {
            abend("ERROR OPENING CROSS REF FILE: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 0200-DISCGRP-OPEN  (CBACT04C.cbl L270-286)
    // Loads the DISCGRP file into a HashMap keyed by the 16-char composite key.
    // -----------------------------------------------------------------------

    private void openDiscGrpFile() {
        String path = ddPath("DISCGRP", "discgrp.txt");
        discGrpByKey = new HashMap<>();
        try {
            for (String line : Files.readAllLines(Path.of(path))) {
                String rec = line.stripTrailing();
                if (rec.length() >= 22) {
                    DisGroupRecord dg = parseDisGroupRecord(rec);
                    discGrpByKey.put(buildDiscGrpKey(dg.groupId(), dg.typeCd(), dg.catCd()), dg);
                }
            }
        } catch (IOException e) {
            abend("ERROR OPENING DALY REJECTS FILE: " + e.getMessage()); // matches COBOL typo
        }
    }

    // -----------------------------------------------------------------------
    // 0300-ACCTFILE-OPEN  (CBACT04C.cbl L288-305)
    // Loads all account records into a mutable HashMap (simulates VSAM I-O).
    // -----------------------------------------------------------------------

    private void openAccountFile() {
        String path = ddPath("ACCTFILE", "acctdata.txt");
        accountFileByAcctId = new LinkedHashMap<>(); // ordered for deterministic output
        modifiedAcctIds = new LinkedHashSet<>();
        try {
            for (String line : Files.readAllLines(Path.of(path))) {
                String rec = line.stripTrailing();
                if (rec.length() >= 11) {
                    AccountRecord acct = parseAccountRecord(rec);
                    accountFileByAcctId.put(acct.acctId.trim(), acct);
                }
            }
        } catch (IOException e) {
            abend("ERROR OPENING ACCOUNT MASTER FILE: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 0400-TRANFILE-OPEN  (CBACT04C.cbl L307-323)
    // Opens the TRANSACT output file for sequential writing.
    // -----------------------------------------------------------------------

    private void openTransactFile() {
        String path = ddPath("TRANSACT", "transact_output.txt");
        try {
            // Ensure parent directories exist
            Path p = Path.of(path);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            transactWriter = new PrintWriter(new BufferedWriter(new FileWriter(path)));
        } catch (IOException e) {
            abend("ERROR OPENING TRANSACTION FILE: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 1000-TCATBALF-GET-NEXT  (CBACT04C.cbl L325-348)
    // Advances the iterator; sets endOfFile on exhaustion.
    // -----------------------------------------------------------------------

    private void getNextTcatbalRecord() {
        if (tcatBalIter.hasNext()) {
            currentTcatBal = tcatBalIter.next();
        } else {
            endOfFile = true;
            System.out.println("ERROR READING TRANSACTION CATEGORY FILE"); // mimics EOF path display
        }
    }

    // -----------------------------------------------------------------------
    // 1050-UPDATE-ACCOUNT  (CBACT04C.cbl L350-370)
    // Adds accumulated interest to ACCT-CURR-BAL; zeros cycle credits/debits;
    // rewrites the account record.
    // -----------------------------------------------------------------------

    private void updateAccount() {
        // ADD WS-TOTAL-INT TO ACCT-CURR-BAL
        // Both S9(09)V99 and S9(10)V99; result truncated to S9(10)V99 (scale 2, RoundingMode.DOWN)
        currentAccount.currBal = currentAccount.currBal
                .add(totalInt)
                .setScale(2, RoundingMode.DOWN);

        // MOVE 0 TO ACCT-CURR-CYC-CREDIT / ACCT-CURR-CYC-DEBIT
        currentAccount.currCycCredit = BigDecimal.ZERO.setScale(2);
        currentAccount.currCycDebit  = BigDecimal.ZERO.setScale(2);

        // REWRITE FD-ACCTFILE-REC FROM ACCOUNT-RECORD
        accountFileByAcctId.put(currentAccount.acctId.trim(), currentAccount);
        modifiedAcctIds.add(currentAccount.acctId.trim());
    }

    // -----------------------------------------------------------------------
    // 1100-GET-ACCT-DATA  (CBACT04C.cbl L372-391)
    // Random read of ACCOUNT-FILE by current account ID.
    // -----------------------------------------------------------------------

    private void getAcctData() {
        // FD-ACCT-ID was set to TRANCAT-ACCT-ID before this call
        String key = currentTcatBal.acctId().trim();
        currentAccount = accountFileByAcctId.get(key);
        if (currentAccount == null) {
            System.out.println("ACCOUNT NOT FOUND: " + currentTcatBal.acctId());
            abend("ERROR READING ACCOUNT FILE");
        }
    }

    // -----------------------------------------------------------------------
    // 1110-GET-XREF-DATA  (CBACT04C.cbl L393-413)
    // Random read of XREF-FILE by XREF-ACCT-ID (alternate key).
    // FD-XREF-ACCT-ID was set to TRANCAT-ACCT-ID before this call.
    // -----------------------------------------------------------------------

    private void getXrefData() {
        String key = currentTcatBal.acctId().trim();
        currentXref = xrefFileByAcctId.get(key);
        if (currentXref == null) {
            System.out.println("ACCOUNT NOT FOUND: " + currentTcatBal.acctId());
            abend("ERROR READING XREF FILE");
        }
    }

    // -----------------------------------------------------------------------
    // 1200-GET-INTEREST-RATE  (CBACT04C.cbl L415-440)
    // Random read of DISCGRP-FILE by composite key:
    //   (ACCT-GROUP-ID, TRANCAT-TYPE-CD, TRANCAT-CD)
    // On status 23 (not found), falls back to "DEFAULT" group.
    // -----------------------------------------------------------------------

    private void getInterestRate() {
        String groupId = padRight(currentAccount.groupId, 10);
        String typeCd  = currentTcatBal.typeCd();
        String catCd   = currentTcatBal.catCd();
        String key     = buildDiscGrpKey(groupId, typeCd, catCd);

        currentDisGroup = discGrpByKey.get(key);

        if (currentDisGroup == null) {
            // File status 23 — record not found
            System.out.println("DISCLOSURE GROUP RECORD MISSING");
            System.out.println("TRY WITH DEFAULT GROUP CODE");
            getDefaultInterestRate(typeCd, catCd); // 1200-A-GET-DEFAULT-INT-RATE
        }
    }

    // -----------------------------------------------------------------------
    // 1200-A-GET-DEFAULT-INT-RATE  (CBACT04C.cbl L443-460)
    // Retry with "DEFAULT   " as the group ID.  ABends if DEFAULT not found.
    // -----------------------------------------------------------------------

    private void getDefaultInterestRate(String typeCd, String catCd) {
        // MOVE 'DEFAULT' TO FD-DIS-ACCT-GROUP-ID — PIC X(10) = "DEFAULT   "
        String defaultGroupId = "DEFAULT   ";
        String key = buildDiscGrpKey(defaultGroupId, typeCd, catCd);

        currentDisGroup = discGrpByKey.get(key);

        if (currentDisGroup == null) {
            System.out.println("ERROR READING DEFAULT DISCLOSURE GROUP");
            abend("DISCGRP-STATUS: record not found for DEFAULT group");
        }
    }

    // -----------------------------------------------------------------------
    // 1300-COMPUTE-INTEREST  (CBACT04C.cbl L462-470)
    //
    // COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
    //
    // No ROUNDED keyword — COBOL truncates (RoundingMode.DOWN).
    // TRAN-CAT-BAL:   PIC S9(09)V99 (scale 2)
    // DIS-INT-RATE:   PIC S9(04)V99 (scale 2; e.g. 15.00 = 15% APR)
    // WS-MONTHLY-INT: PIC S9(09)V99 (scale 2)
    //
    // Divisor 1200 = 12 months × 100 (rate is stored as a percentage).
    // COBOL truncates the result to fit WS-MONTHLY-INT (S9(09)V99, scale 2).
    // -----------------------------------------------------------------------

    private void computeInterest() {
        // (balance × annualRate) / 1200, truncated to 2 decimal places
        monthlyInt = currentTcatBal.balance()
                .multiply(currentDisGroup.intRate())
                .divide(new BigDecimal("1200"), 2, RoundingMode.DOWN); // CBACT04C.cbl L464-465

        // ADD WS-MONTHLY-INT TO WS-TOTAL-INT
        totalInt = totalInt.add(monthlyInt).setScale(2, RoundingMode.DOWN);

        writeTx(); // 1300-B-WRITE-TX
    }

    // -----------------------------------------------------------------------
    // 1300-B-WRITE-TX  (CBACT04C.cbl L473-515)
    // Builds a TRAN-RECORD (CVTRA05Y) and writes it to TRANSACT-FILE.
    //
    // TRAN-ID construction via STRING DELIMITED BY SIZE:
    //   PARM-DATE (PIC X(10)) + WS-TRANID-SUFFIX (PIC 9(06)) = 16 chars.
    // -----------------------------------------------------------------------

    private void writeTx() {
        tranIdSuffix++; // ADD 1 TO WS-TRANID-SUFFIX

        // STRING PARM-DATE, WS-TRANID-SUFFIX DELIMITED BY SIZE INTO TRAN-ID
        // PARM-DATE is PIC X(10) (full field width); suffix is PIC 9(06) zero-padded.
        String tranId = parmDate + String.format("%06d", tranIdSuffix); // CBACT04C.cbl L476-480

        // TRAN-TYPE-CD: MOVE '01' to PIC X(02) — "01"
        String typeCd = "01"; // CBACT04C.cbl L482

        // TRAN-CAT-CD: MOVE '05' to PIC 9(04) — numeric right-justify → "0005"
        String catCd = "0005"; // CBACT04C.cbl L483

        // TRAN-SOURCE: MOVE 'System' to PIC X(10) → space-padded to 10
        String source = String.format("%-10s", "System"); // CBACT04C.cbl L484

        // STRING 'Int. for a/c ', ACCT-ID DELIMITED BY SIZE INTO TRAN-DESC
        // 'Int. for a/c ' = 13 chars, ACCT-ID (PIC 9(11)) = 11 chars → 24 chars in field
        String desc = String.format("%-100s", "Int. for a/c " + currentAccount.acctId); // CBACT04C.cbl L485-489

        // TRAN-AMT = WS-MONTHLY-INT (PIC S9(09)V99)
        BigDecimal tranAmt = monthlyInt; // CBACT04C.cbl L490

        // TRAN-MERCHANT-ID: MOVE 0 to PIC 9(09) → "000000000"
        String merchantId = "000000000"; // CBACT04C.cbl L491

        // TRAN-MERCHANT-NAME/CITY/ZIP: MOVE SPACES to PIC X fields
        String merchantName = " ".repeat(50); // CBACT04C.cbl L492
        String merchantCity = " ".repeat(50); // CBACT04C.cbl L493
        String merchantZip  = " ".repeat(10); // CBACT04C.cbl L494

        // TRAN-CARD-NUM: MOVE XREF-CARD-NUM (PIC X(16)) — from current xref record
        String cardNum = padRight(currentXref.cardNum(), 16); // CBACT04C.cbl L495

        // PERFORM Z-GET-DB2-FORMAT-TIMESTAMP → YYYY-MM-DD-HH.MM.SS.NN0000
        // Externalised to common.time.Db2Timestamps (shared with CBTRN02C/CBEXPORT/COBIL00C).
        String ts = Db2Timestamps.currentDb2Timestamp(); // CBACT04C.cbl L496-498

        String tranRecord = buildTranRecord(tranId, typeCd, catCd, source, desc,
                tranAmt, merchantId, merchantName, merchantCity, merchantZip,
                cardNum, ts, ts);

        transactWriter.print(tranRecord); // WRITE FD-TRANFILE-REC FROM TRAN-RECORD
        if (transactWriter.checkError()) {
            abend("ERROR WRITING TRANSACTION RECORD");
        }
    }

    // -----------------------------------------------------------------------
    // 1400-COMPUTE-FEES  (CBACT04C.cbl L518-520)
    // STUB — "To be implemented" in original COBOL source.
    // Replicated as empty method per Equivalence-First: do not add logic.
    // -----------------------------------------------------------------------

    private void computeFees() {
        // stub — CBACT04C.cbl L518-520: "* To be implemented"
    }

    // -----------------------------------------------------------------------
    // Close paragraphs — 9000–9400  (CBACT04C.cbl L522-611)
    // -----------------------------------------------------------------------

    private void closeTcatbalFile() {
        tcatBalIter = null; // release reference
    }

    private void closeXrefFile() {
        xrefFileByAcctId = null;
    }

    private void closeDiscGrpFile() {
        discGrpByKey = null;
    }

    /**
     * 9300-ACCTFILE-CLOSE (CBACT04C.cbl L577-593)
     * Writes back all modified account records to simulate VSAM REWRITE.
     * Modified records are written to ACCTFILE_OUT (default: acctdata_output.txt).
     */
    private void closeAccountFile() {
        if (modifiedAcctIds == null || modifiedAcctIds.isEmpty()) return;

        String outPath = ddPath("ACCTFILE_OUT", "acctdata_output.txt");
        try {
            Path p = Path.of(outPath);
            if (p.getParent() != null) Files.createDirectories(p.getParent());
            try (PrintWriter pw = new PrintWriter(new FileWriter(outPath))) {
                for (AccountRecord acct : accountFileByAcctId.values()) {
                    pw.println(acct.serialize());
                }
            }
        } catch (IOException e) {
            abend("ERROR CLOSING ACCOUNT FILE: " + e.getMessage());
        }
        accountFileByAcctId = null;
    }

    private void closeTransactFile() {
        if (transactWriter != null) {
            transactWriter.flush();
            transactWriter.close();
        }
    }

    // -----------------------------------------------------------------------
    // 9999-ABEND-PROGRAM  (CBACT04C.cbl L628-632)
    // Externalised to common.batch.BatchAbortException (shared across all batch
    // programs).  BatchAbortException.abort() reproduces the 'ABENDING PROGRAM'
    // DISPLAY and carries abend code 999; main() maps it to System.exit.
    // -----------------------------------------------------------------------

    private void abend(String message) {
        throw BatchAbortException.abort(message);
    }

    // -----------------------------------------------------------------------
    // Record parsers — fixed-length ASCII flat file → typed records
    // All signed numeric fields use EBCDIC-to-ASCII overpunch encoding
    // (the format used by the CardDemo ASCII data files in app/data/ASCII/).
    // -----------------------------------------------------------------------

    /** CVTRA01Y.cpy: parse a 50-byte TRAN-CAT-BAL-RECORD line */
    private TranCatBalRecord parseTranCatBalRecord(String rec) {
        String acctId  = substring(rec,  0, 11);  // TRANCAT-ACCT-ID  PIC 9(11)
        String typeCd  = substring(rec, 11, 13);  // TRANCAT-TYPE-CD  PIC X(02)
        String catCd   = substring(rec, 13, 17);  // TRANCAT-CD       PIC 9(04)
        String balRaw  = substring(rec, 17, 28);  // TRAN-CAT-BAL     PIC S9(09)V99 (11 bytes)
        return new TranCatBalRecord(acctId, typeCd, catCd, parseSignedDisplay(balRaw, 2));
    }

    /** CVACT03Y.cpy: parse a 50-byte CARD-XREF-RECORD line */
    private CardXrefRecord parseCardXrefRecord(String rec) {
        String cardNum = substring(rec,  0, 16);  // XREF-CARD-NUM  PIC X(16)
        String custId  = substring(rec, 16, 25);  // XREF-CUST-ID   PIC 9(09)
        String acctId  = substring(rec, 25, 36);  // XREF-ACCT-ID   PIC 9(11)
        return new CardXrefRecord(cardNum, custId, acctId);
    }

    /** CVTRA02Y.cpy: parse a 50-byte DIS-GROUP-RECORD line */
    private DisGroupRecord parseDisGroupRecord(String rec) {
        String groupId = substring(rec,  0, 10);  // DIS-ACCT-GROUP-ID  PIC X(10)
        String typeCd  = substring(rec, 10,  2);  // DIS-TRAN-TYPE-CD   PIC X(02)
        String catCd   = substring(rec, 12,  4);  // DIS-TRAN-CAT-CD    PIC 9(04)
        String rateRaw = substring(rec, 16,  6);  // DIS-INT-RATE       PIC S9(04)V99 (6 bytes)
        return new DisGroupRecord(groupId, typeCd, catCd, parseSignedDisplay(rateRaw, 2));
    }

    /** CVACT01Y.cpy: parse a 300-byte ACCOUNT-RECORD line */
    private AccountRecord parseAccountRecord(String rec) {
        // Pad to full record length to handle short lines
        if (rec.length() < AccountRecord.RECORD_LENGTH) {
            rec = String.format("%-300s", rec);
        }
        String acctId      = substring(rec, AccountRecord.OFF_ACCT_ID,     11); // PIC 9(11)
        String groupId     = substring(rec, AccountRecord.OFF_GROUP_ID,     10); // PIC X(10)
        String currBalRaw  = substring(rec, AccountRecord.OFF_CURR_BAL,     12); // PIC S9(10)V99
        String cycCrdRaw   = substring(rec, AccountRecord.OFF_CURR_CYC_CREDIT, 12);
        String cycDbtRaw   = substring(rec, AccountRecord.OFF_CURR_CYC_DEBIT,  12);

        BigDecimal currBal       = parseSignedDisplay(currBalRaw,  2);
        BigDecimal currCycCredit = parseSignedDisplay(cycCrdRaw,   2);
        BigDecimal currCycDebit  = parseSignedDisplay(cycDbtRaw,   2);

        byte[] rawBytes = rec.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        if (rawBytes.length < AccountRecord.RECORD_LENGTH) {
            rawBytes = Arrays.copyOf(rawBytes, AccountRecord.RECORD_LENGTH);
            // Fill padding bytes with ASCII space
            Arrays.fill(rawBytes, rec.length(), AccountRecord.RECORD_LENGTH, (byte) 0x20);
        }
        return new AccountRecord(acctId, groupId, currBal, currCycCredit, currCycDebit, rawBytes);
    }

    // -----------------------------------------------------------------------
    // Numeric encoding/decoding — EBCDIC-to-ASCII overpunch
    //
    // COBOL DISPLAY signed numerics embed the sign in the last digit byte.
    // When EBCDIC files are converted to ASCII the mapping is:
    //
    //   Positive zone (EBCDIC 0xCn → ASCII):
    //     digit 0 → '{' (0x7B),  digits 1-9 → 'A'-'I' (0x41-0x49)
    //
    //   Negative zone (EBCDIC 0xDn → ASCII):
    //     digit 0 → '}' (0x7D),  digits 1-9 → 'J'-'R' (0x4A-0x52)
    //
    //   Plain ASCII digit (no sign encoding): treated as positive.
    //
    // See: Copybook_Data_Dictionary.md §"Migration Gotchas" #1 (overpunch).
    // -----------------------------------------------------------------------

    /**
     * Parse a COBOL DISPLAY signed numeric field (SIGN IS TRAILING, embedded).
     * Handles EBCDIC-to-ASCII overpunch and plain ASCII digit fallback.
     *
     * @param raw   fixed-length raw field string from the flat file
     * @param scale implied decimal places (V digits in PIC clause)
     * @return      decoded BigDecimal value with the given scale
     */
    static BigDecimal parseSignedDisplay(String raw, int scale) {
        if (raw == null || raw.isBlank()) return BigDecimal.ZERO.setScale(scale);

        String trimmed = raw.stripTrailing();
        if (trimmed.isEmpty()) return BigDecimal.ZERO.setScale(scale);

        char lastCh = trimmed.charAt(trimmed.length() - 1);
        String leadingDigits = trimmed.substring(0, trimmed.length() - 1);
        boolean negative;
        char replacedLastDigit;

        if (lastCh == '{') {
            // Positive digit 0 (EBCDIC 0xC0 → ASCII 0x7B)
            negative = false; replacedLastDigit = '0';
        } else if (lastCh >= 'A' && lastCh <= 'I') {
            // Positive digits 1-9 (EBCDIC 0xC1-0xC9 → ASCII 0x41-0x49)
            negative = false; replacedLastDigit = (char) ('0' + (lastCh - 'A' + 1));
        } else if (lastCh == '}') {
            // Negative digit 0 (EBCDIC 0xD0 → ASCII 0x7D)
            negative = true;  replacedLastDigit = '0';
        } else if (lastCh >= 'J' && lastCh <= 'R') {
            // Negative digits 1-9 (EBCDIC 0xD1-0xD9 → ASCII 0x4A-0x52)
            negative = true;  replacedLastDigit = (char) ('0' + (lastCh - 'J' + 1));
        } else if (Character.isDigit(lastCh)) {
            // Plain digit — no overpunch, treat as positive (common in test data)
            negative = false; replacedLastDigit = lastCh;
        } else if (lastCh == '+') {
            // Sign-separate trailing positive (alternative encoding)
            negative = false;
            String digits = leadingDigits;
            return new BigDecimal(digits.isEmpty() ? "0" : digits)
                    .movePointLeft(scale).setScale(scale, RoundingMode.DOWN);
        } else if (lastCh == '-') {
            // Sign-separate trailing negative
            negative = true;
            String digits = leadingDigits;
            BigDecimal v = new BigDecimal(digits.isEmpty() ? "0" : digits)
                    .movePointLeft(scale).setScale(scale, RoundingMode.DOWN);
            return v.negate();
        } else {
            // Unknown encoding — treat as zero
            return BigDecimal.ZERO.setScale(scale);
        }

        String digits = leadingDigits + replacedLastDigit;
        digits = digits.trim().replaceAll("^\\s+", ""); // strip leading spaces
        if (digits.isEmpty()) return BigDecimal.ZERO.setScale(scale);

        try {
            BigDecimal value = new BigDecimal(digits)
                    .movePointLeft(scale)
                    .setScale(scale, RoundingMode.DOWN);
            return negative ? value.negate() : value;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(scale);
        }
    }

    /**
     * Format a BigDecimal as a COBOL DISPLAY signed numeric field with
     * EBCDIC-to-ASCII overpunch encoding in the last byte.
     *
     * @param value      the BigDecimal to format
     * @param intDigits  number of integer digits (before the implied decimal)
     * @param scale      number of decimal digits (after the implied decimal)
     * @return           fixed-length string of length (intDigits + scale)
     */
    static String formatSignedDisplay(BigDecimal value, int intDigits, int scale) {
        int totalLen = intDigits + scale;
        boolean negative = value.signum() < 0;
        BigDecimal abs = value.abs().setScale(scale, RoundingMode.DOWN);

        // Shift to integer, format with leading zeros
        BigDecimal shifted = abs.movePointRight(scale);
        String digits;
        try {
            long longVal = shifted.longValueExact();
            digits = String.format("%0" + totalLen + "d", longVal);
        } catch (ArithmeticException e) {
            // Value too large for long — use string manipulation
            digits = shifted.toBigInteger().toString();
            if (digits.length() < totalLen) {
                digits = "0".repeat(totalLen - digits.length()) + digits;
            } else if (digits.length() > totalLen) {
                digits = digits.substring(digits.length() - totalLen); // truncate left
            }
        }

        // Encode sign in last character (EBCDIC-to-ASCII overpunch)
        int lastDigitVal = digits.charAt(digits.length() - 1) - '0';
        char encodedLast;
        if (negative) {
            encodedLast = (lastDigitVal == 0) ? '}' : (char) (0x49 + lastDigitVal); // J=0x4A..R=0x52
        } else {
            encodedLast = (lastDigitVal == 0) ? '{' : (char) (0x40 + lastDigitVal); // A=0x41..I=0x49
        }

        return digits.substring(0, digits.length() - 1) + encodedLast;
    }

    // -----------------------------------------------------------------------
    // TRAN-RECORD builder — CVTRA05Y.cpy, 350 bytes
    // -----------------------------------------------------------------------

    /**
     * Assembles a fixed-length 350-byte TRAN-RECORD per CVTRA05Y.cpy layout.
     * Field offsets: TRAN-ID[0,16), TYPE-CD[16,18), CAT-CD[18,22),
     *   SOURCE[22,32), DESC[32,132), AMT[132,143), MERCHANT-ID[143,152),
     *   MERCHANT-NAME[152,202), MERCHANT-CITY[202,252), MERCHANT-ZIP[252,262),
     *   CARD-NUM[262,278), ORIG-TS[278,304), PROC-TS[304,330), FILLER[330,350)
     */
    private String buildTranRecord(
            String tranId, String typeCd, String catCd, String source,
            String desc, BigDecimal amt, String merchantId,
            String merchantName, String merchantCity, String merchantZip,
            String cardNum, String origTs, String procTs) {

        StringBuilder sb = new StringBuilder(350);
        sb.append(padRight(tranId,       16));  // TRAN-ID          PIC X(16) offset 0
        sb.append(padRight(typeCd,        2));  // TRAN-TYPE-CD     PIC X(02) offset 16
        sb.append(padRight(catCd,         4));  // TRAN-CAT-CD      PIC 9(04) offset 18
        sb.append(padRight(source,       10));  // TRAN-SOURCE      PIC X(10) offset 22
        sb.append(padRight(desc,        100));  // TRAN-DESC        PIC X(100) offset 32
        sb.append(formatSignedDisplay(amt, 9, 2)); // TRAN-AMT     PIC S9(09)V99 offset 132 (11 bytes)
        sb.append(padRight(merchantId,    9));  // TRAN-MERCHANT-ID PIC 9(09) offset 143
        sb.append(padRight(merchantName, 50));  // MERCHANT-NAME    PIC X(50) offset 152
        sb.append(padRight(merchantCity, 50));  // MERCHANT-CITY    PIC X(50) offset 202
        sb.append(padRight(merchantZip,  10));  // MERCHANT-ZIP     PIC X(10) offset 252
        sb.append(padRight(cardNum,      16));  // TRAN-CARD-NUM    PIC X(16) offset 262
        sb.append(padRight(origTs,       26));  // TRAN-ORIG-TS     PIC X(26) offset 278
        sb.append(padRight(procTs,       26));  // TRAN-PROC-TS     PIC X(26) offset 304
        sb.append(" ".repeat(20));              // FILLER           PIC X(20) offset 330

        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Build the 16-char composite DISCGRP key: groupId(10) + typeCd(2) + catCd(4) */
    private static String buildDiscGrpKey(String groupId, String typeCd, String catCd) {
        return padRight(groupId, 10) + padRight(typeCd, 2) + padRight(catCd, 4);
    }

    /** Extract substring safely, padding with spaces if the source is too short */
    private static String substring(String s, int from, int length) {
        if (s == null) return " ".repeat(length);
        int end = from + length;
        if (s.length() >= end) return s.substring(from, end);
        if (s.length() <= from) return " ".repeat(length);
        return s.substring(from) + " ".repeat(end - s.length());
    }

    /** Right-pad a string with spaces to the target width, or truncate if too long */
    private static String padRight(String s, int width) {
        if (s == null) return " ".repeat(width);
        if (s.length() >= width) return s.substring(0, width);
        return String.format("%-" + width + "s", s);
    }

    /** Format a TRAN-CAT-BAL-RECORD for the DISPLAY statement */
    private static String formatTranCatBalForDisplay(TranCatBalRecord r) {
        return String.format("%-11s%-2s%-4s %s",
                r.acctId(), r.typeCd(), r.catCd(), r.balance().toPlainString());
    }
}
