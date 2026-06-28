package modernization.app.common.batch;

/**
 * Fatal, unrecoverable batch termination — the idiomatic-Java replacement for the
 * {@code 9999-ABEND-PROGRAM} paragraph copied verbatim into every CardDemo batch
 * program ({@code CBACT01C}-{@code 04C}, {@code CBTRN02C}/{@code 03C}, {@code CBCUS01C},
 * {@code CBSTM03A}, ...).
 *
 * <p>The COBOL paragraph displays {@code 'ABENDING PROGRAM'} and calls the LE service
 * {@code CEE3ABD} with abend code {@code 999} ({@code CBACT04C.cbl:628-632}). Rather than
 * terminate the JVM from deep in the call stack, this exception is thrown and caught by a
 * single top-level handler in the program's {@code main}, which maps {@link #code()} to
 * {@code System.exit}. The legacy term <em>abend</em> (ABnormal END) is retired in favour
 * of <em>abort</em>.
 */
public class BatchAbortException extends RuntimeException {

    /** Abend code issued by {@code 9999-ABEND-PROGRAM} via {@code CEE3ABD} ({@code MOVE 999 TO ABCODE}). */
    public static final int DEFAULT_ABORT_CODE = 999;

    private final int code;

    public BatchAbortException(String message) {
        this(DEFAULT_ABORT_CODE, message);
    }

    public BatchAbortException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** @return the abend code (COBOL {@code ABCODE}); {@link #DEFAULT_ABORT_CODE} unless overridden. */
    public int code() {
        return code;
    }

    /**
     * Faithful reproduction of {@code 9999-ABEND-PROGRAM}'s observable side effect: emit the
     * {@code 'ABENDING PROGRAM'} line and the caller-supplied diagnostic, then return the
     * exception to throw. Centralising the {@code DISPLAY} here removes the duplicated idiom
     * from every batch program.
     *
     * <p>Usage: {@code throw BatchAbortException.abort("ERROR READING ACCOUNT FILE");}
     *
     * @param message the diagnostic the caller would have displayed before invoking the paragraph
     * @return the exception to throw (returned rather than thrown so {@code throw} stays at the call site)
     */
    public static BatchAbortException abort(String message) {
        System.out.println("ABENDING PROGRAM");
        System.out.println(message);
        return new BatchAbortException(DEFAULT_ABORT_CODE, message);
    }
}
