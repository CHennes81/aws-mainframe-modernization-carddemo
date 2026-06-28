package modernization.app.common.batch;

/**
 * Formats a two-byte file status code into the CardDemo {@code FILE STATUS IS: NNNN}
 * display string — the Java home for the {@code 9910-DISPLAY-IO-STATUS} paragraph copied
 * across the batch programs.
 *
 * <p>Faithful to {@code CBACT04C.cbl:635-648}:
 * <pre>
 *   IF  IO-STATUS NOT NUMERIC OR IO-STAT1 = '9'
 *       MOVE IO-STAT1 TO IO-STATUS-04(1:1)        first display digit = status byte 1
 *       MOVE 0        TO TWO-BYTES-BINARY
 *       MOVE IO-STAT2 TO TWO-BYTES-RIGHT          low byte = status byte 2
 *       MOVE TWO-BYTES-BINARY TO IO-STATUS-0403   remaining 3 digits = binary value of byte 2
 *   ELSE
 *       MOVE '0000'    TO IO-STATUS-04            "0000"
 *       MOVE IO-STATUS TO IO-STATUS-04(3:2)       last two digits = the numeric status
 * </pre>
 *
 * <p><b>Equivalence note.</b> The first {@code CBACT04C} translation took a different
 * (descriptive-message) path on I/O errors and did not emit this line; the happy-path
 * golden master is unaffected. Wiring this formatter into the batch error paths and
 * reconciling the error-path output is an equivalence-completion task tracked in the
 * discrepancy register, not part of the structural refactor that introduced this class.
 *
 * <p><b>EBCDIC/ASCII note.</b> On the non-numeric branch, {@code IO-STAT2}'s byte value is
 * expanded as a number. On the mainframe that is the EBCDIC code point; in this ASCII demo
 * it is the ASCII code point. The host value differs for the same glyph and must be
 * reconciled when validating against EBCDIC-sourced output.
 */
public final class VsamStatus {

    private VsamStatus() {}

    /** Literal prefix from {@code DISPLAY 'FILE STATUS IS: NNNN' IO-STATUS-04}; the {@code NNNN} stays in the output. */
    private static final String DISPLAY_PREFIX = "FILE STATUS IS: NNNN";

    /**
     * The 4-digit {@code IO-STATUS-04} value for a two-character file status.
     *
     * @param stat1 first status byte ({@code IO-STAT1})
     * @param stat2 second status byte ({@code IO-STAT2})
     * @return the 4-character display code
     */
    public static String toDisplayCode(char stat1, char stat2) {
        boolean numeric = Character.isDigit(stat1) && Character.isDigit(stat2);
        if (!numeric || stat1 == '9') {
            int lowByte = stat2 & 0xFF; // TWO-BYTES-RIGHT -> TWO-BYTES-BINARY
            return stat1 + String.format("%03d", lowByte);
        }
        return "00" + stat1 + stat2; // '0000' with positions 3:2 overlaid by the status
    }

    /**
     * The full {@code DISPLAY} line for a two-character file status, including the literal
     * {@code NNNN} placeholder that the COBOL leaves in the output.
     *
     * @param ioStatus the two-character file status (e.g. {@code "23"}, {@code "9 "})
     * @return the line written by {@code 9910-DISPLAY-IO-STATUS}
     */
    public static String displayLine(String ioStatus) {
        char stat1 = ioStatus.length() > 0 ? ioStatus.charAt(0) : ' ';
        char stat2 = ioStatus.length() > 1 ? ioStatus.charAt(1) : ' ';
        return DISPLAY_PREFIX + toDisplayCode(stat1, stat2);
    }
}
