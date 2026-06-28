package modernization.app.common.time;

import java.time.LocalDateTime;

/**
 * DB2-format timestamp generation.
 *
 * <p>Java home for the {@code Z-GET-DB2-FORMAT-TIMESTAMP} paragraph that is copied
 * across {@code CBACT04C}, {@code CBTRN02C}, {@code CBEXPORT}, and (in a CICS
 * {@code ASKTIME} variant) {@code COBIL00C}. Extracted verbatim from the verified
 * {@code CBACT04C.Z-GET-DB2-FORMAT-TIMESTAMP} translation.
 *
 * <p>This is timestamp <em>generation</em>, deliberately kept separate from the
 * {@code common.validation} package: generating a value and validating one are
 * opposite responsibilities with different change drivers.
 *
 * <h2>Format variants across the corpus</h2>
 * <ul>
 *   <li><b>CBACT04C / CBTRN02C</b>: {@code YYYY-MM-DD-HH.MM.SS.hh0000} — hundredths of
 *       a second ({@code COB-MIL}) followed by a fixed {@code 0000} suffix
 *       (see {@code CBACT04C.cbl:140}, {@code DB2-REST VALUE '0000'}).</li>
 *   <li><b>CBEXPORT</b>: {@code YYYY-MM-DD HH:MM:SS.00} — space separator, literal
 *       {@code .00}. Documented divergence; logged for the discrepancy register.</li>
 *   <li><b>COBIL00C</b>: CICS {@code ASKTIME}/{@code FORMATTIME} producing the same
 *       shape as CBACT04C; {@code ASKTIME} is just a wall-clock read.</li>
 * </ul>
 *
 * The CBACT04C-faithful form is the canonical method below.
 */
public final class Db2Timestamps {

    private Db2Timestamps() {}

    /** Hundredths of a second per whole second (COBOL {@code COB-MIL} is 2 digits). */
    private static final int NANOS_PER_HUNDREDTH = 10_000_000;

    /**
     * Current DB2-format timestamp: {@code YYYY-MM-DD-HH.MM.SS.hh0000}.
     *
     * <p>Equivalent to {@code CBACT04C.Z-GET-DB2-FORMAT-TIMESTAMP} driven by
     * {@code FUNCTION CURRENT-DATE}.
     *
     * @return a 26-character DB2 timestamp string
     */
    public static String currentDb2Timestamp() {
        return format(LocalDateTime.now());
    }

    /**
     * Deterministic formatter — separated from {@link #currentDb2Timestamp()} so the
     * pure formatting logic is unit-testable without a wall-clock dependency.
     *
     * @param now the instant to render
     * @return a 26-character DB2 timestamp string {@code YYYY-MM-DD-HH.MM.SS.hh0000}
     */
    public static String format(LocalDateTime now) {
        int hundredths = now.getNano() / NANOS_PER_HUNDREDTH; // COB-MIL — truncate ns
        return String.format("%04d-%02d-%02d-%02d.%02d.%02d.%02d0000",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                now.getHour(), now.getMinute(), now.getSecond(),
                hundredths);
    }
}
