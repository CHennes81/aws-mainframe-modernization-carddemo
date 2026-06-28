package modernization.app.common.validation;

/**
 * Outcome of a single field edit.
 *
 * <p>This is the Java replacement for the COBOL {@code WS-RETURN-MSG} /
 * {@code INPUT-ERROR} flag pair that every CardDemo edit paragraph shares
 * (see {@code CSUTLDWY.cpy} and the {@code 1215}-{@code 1280} edits in
 * {@code COACTUPC.cbl}). Both {@link DateValidator} and {@link FieldValidator}
 * return this type, which is what unifies the validation family — a shared
 * result shape rather than an inheritance hierarchy.
 *
 * <p>Immutable. A valid result carries no field name or message; an error
 * result carries both.
 */
public record ValidationResult(boolean valid, String fieldName, String message) {

    private static final ValidationResult OK = new ValidationResult(true, null, null);

    /** A successful edit (COBOL: {@code INPUT-ERROR} stays off, {@code WS-RETURN-MSG} blank). */
    public static ValidationResult ok() {
        return OK;
    }

    /**
     * A failed edit.
     *
     * @param fieldName the field that failed (COBOL: {@code WS-EDIT-VARIABLE-NAME})
     * @param message   the human-readable reason (COBOL: {@code WS-RETURN-MSG})
     */
    public static ValidationResult error(String fieldName, String message) {
        return new ValidationResult(false, fieldName, message);
    }

    /** @return {@code true} when the edit failed (COBOL: {@code INPUT-ERROR}). */
    public boolean isError() {
        return !valid;
    }
}
