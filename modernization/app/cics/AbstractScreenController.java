package modernization.app.cics;

/**
 * Base class for translated CICS screen programs — the Java home for the two paragraphs
 * duplicated across the online suite:
 *
 * <ul>
 *   <li>{@code POPULATE-HEADER-INFO} (#4, defined in 12 programs: {@code COBIL00C},
 *       {@code COADM01C}, {@code COMEN01C}, {@code COSGN00C}, {@code COTRN00C/01C/02C},
 *       {@code COUSR00C/01C/02C/03C}, {@code CORPT00C}) - stamps title, transaction id,
 *       program name, and the current date/time into the map's header fields.</li>
 *   <li>{@code RETURN-TO-PREV-SCREEN} (#6, defined in 9 programs) - back-navigation: default
 *       a blank target to the signon screen, stamp the commarea, reset the program context,
 *       then transfer control (CICS {@code XCTL} - a controller redirect in Java).</li>
 * </ul>
 *
 * <p>These are grouped here, rather than in a static utility, because both operate on shared
 * session state (the commarea) and per-screen output - a template-method / base-class
 * concern. The pure, stateless {@code EIBAID} mapping is intentionally <em>not</em> here; it
 * is {@link AttentionKey}.
 *
 * <p><b>Status: SCAFFOLD.</b> No CICS program has been translated and the commarea / screen
 * DTO infrastructure does not yet exist, so the method signatures use placeholder types
 * ({@code Object}) to be tightened once the first screen's golden master is captured. The
 * shape and COBOL citations are fixed; bodies follow under the golden master.
 */
public abstract class AbstractScreenController {

    /**
     * {@code POPULATE-HEADER-INFO}: move title, transaction id, program name, and the current
     * date ({@code CURDATEO}) and time ({@code CURTIMEO}) into the screen's header fields.
     *
     * @param mapOutput the screen output DTO (BMS map output area, e.g. {@code COTRN0AO})
     * @param tranId    the current transaction id
     * @param pgmName   the current program name
     */
    protected void populateHeader(Object mapOutput, String tranId, String pgmName) {
        throw new UnsupportedOperationException("populateHeader: pending first CICS golden master");
    }

    /**
     * {@code RETURN-TO-PREV-SCREEN}: default a blank target program to the signon screen
     * ({@code COSGN00C}), stamp the commarea with this program's identity, reset
     * {@code CDEMO-PGM-CONTEXT} to 0, and transfer control to the target (CICS {@code XCTL}).
     *
     * @param commArea       the CardDemo commarea (session state)
     * @param currentTranId  this program's transaction id
     * @param currentPgmName this program's program name
     */
    protected void returnToPreviousScreen(Object commArea, String currentTranId, String currentPgmName) {
        throw new UnsupportedOperationException("returnToPreviousScreen: pending first CICS golden master");
    }
}
