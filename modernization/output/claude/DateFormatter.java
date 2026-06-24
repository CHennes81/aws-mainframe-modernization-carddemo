package modernization.output.claude;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Java equivalent of the COBDATFT assembler module (app/asm/COBDATFT.asm).
 *
 * COBDATFT provides two date conversion modes triggered by a type indicator:
 *   Type '1': compact  YYYYMMDD  (8 chars)  →  display  YYYY-MM-DD  (10 chars)
 *   Type '2': display  YYYY-MM-DD (10 chars) →  compact  YYYYMMDD    (8 chars)
 *
 * Calling convention in COBOL (from CBACT01C and similar programs):
 *   CALL 'COBDATFT' USING CODATECN-REC
 * where CODATECN-REC (copybook CODATECN) contains a type byte, input area,
 * and output area (REDEFINES overlay on input — same physical storage).
 *
 * Migration notes:
 *   - The assembler module overwrites the input field with the converted result
 *     via REDEFINES. This Java equivalent returns a new String instead.
 *   - Invalid input (non-parseable date) returns the input unchanged, matching
 *     the assembler's silent pass-through behavior on bad data.
 *   - No rounding involved; these are pure string transformations.
 */
public final class DateFormatter {

    private static final DateTimeFormatter COMPACT  = DateTimeFormatter.BASIC_ISO_DATE;  // yyyyMMdd
    private static final DateTimeFormatter DISPLAY  = DateTimeFormatter.ISO_LOCAL_DATE;  // yyyy-MM-dd

    private DateFormatter() {}

    /**
     * Type 1: YYYYMMDD → YYYY-MM-DD
     *
     * COBDATFT type indicator '1'. Used by CBACT01C to format stored account
     * dates for report output.
     *
     * @param yyyymmdd  8-character compact date string (e.g. "20250520")
     * @return          10-character display date string (e.g. "2025-05-20"),
     *                  or the original input if it cannot be parsed
     */
    public static String compactToDisplay(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() < 8) {
            return yyyymmdd;
        }
        try {
            LocalDate date = LocalDate.parse(yyyymmdd.substring(0, 8), COMPACT);
            return date.format(DISPLAY);
        } catch (DateTimeParseException e) {
            return yyyymmdd; // pass-through on bad data, matching assembler behavior
        }
    }

    /**
     * Type 2: YYYY-MM-DD → YYYYMMDD
     *
     * COBDATFT type indicator '2'. Used when storing dates back into compact
     * format after display-side manipulation.
     *
     * @param yyyyDashMmDashDd  10-character display date string (e.g. "2025-05-20")
     * @return                  8-character compact date string (e.g. "20250520"),
     *                          or the original input if it cannot be parsed
     */
    public static String displayToCompact(String yyyyDashMmDashDd) {
        if (yyyyDashMmDashDd == null || yyyyDashMmDashDd.length() < 10) {
            return yyyyDashMmDashDd;
        }
        try {
            LocalDate date = LocalDate.parse(yyyyDashMmDashDd.substring(0, 10), DISPLAY);
            return date.format(COMPACT);
        } catch (DateTimeParseException e) {
            return yyyyDashMmDashDd;
        }
    }
}
