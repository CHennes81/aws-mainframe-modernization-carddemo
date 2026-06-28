package modernization.app.common.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/**
 * CCYYMMDD date validation — the Java home for the {@code CSUTLDPY.cpy} /
 * {@code CSUTLDWY.cpy} edit suite copied into {@code COACTUPC} (open date, expiry date,
 * reissue date, date of birth).
 *
 * <p>Returns the shared {@link ValidationResult}, which is what unifies this class with
 * {@link FieldValidator} — a common result shape, not a base class.
 *
 * <h2>CEEDAYS replacement</h2>
 * {@code CSUTLDPY.EDIT-DATE-LE} delegates calendar-validity checking to {@code CSUTLDTC.cbl},
 * which wraps the IBM Language Environment service {@code CEEDAYS}. There is no JVM
 * equivalent of CEEDAYS; {@link LocalDate} with {@link ResolverStyle#STRICT} reproduces the
 * validity check (rejecting e.g. {@code 2023-02-29}) without the LE runtime dependency.
 *
 * <p><b>Status: logic complete, equivalence pending.</b> This translation is derived from
 * {@code CSUTLDPY} semantics but is not yet proven against a frozen golden master — no CICS
 * program has been captured. Per Equivalence-First it must be diffed against COACTUPC's
 * golden master before cutover. Paragraph citations are provided for that reconciliation.
 */
public final class DateValidator {

    private DateValidator() {}

    /** STRICT resolver rejects impossible dates (no Feb 30); replaces {@code CEEDAYS}. */
    private static final DateTimeFormatter STRICT_CCYYMMDD =
            DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    /**
     * Full CCYYMMDD edit: presence, numeric, century/year, month, day, and calendar
     * cross-validation (leap year / month length).
     *
     * <p>Covers {@code EDIT-YEAR-CCYY}, {@code EDIT-MONTH}, {@code EDIT-DAY},
     * {@code EDIT-DAY-MONTH-YEAR}, and {@code EDIT-DATE-LE} (CSUTLDPY.cpy).
     *
     * @param ccyymmdd  the 8-character candidate date
     * @param fieldName the field under edit (COBOL {@code WS-EDIT-VARIABLE-NAME})
     */
    public static ValidationResult validateCcyymmdd(String ccyymmdd, String fieldName) {
        // EDIT-YEAR-CCYY: blank / LOW-VALUES rejected (CSUTLDPY.cpy:30-31)
        if (ccyymmdd == null || ccyymmdd.isBlank()) {
            return ValidationResult.error(fieldName, "Date must be supplied");
        }
        String v = ccyymmdd.trim();
        // EDIT-YEAR-CCYY: must be numeric (CSUTLDPY.cpy:48); length is fixed PIC 9(08)
        if (v.length() != 8 || !v.chars().allMatch(Character::isDigit)) {
            return ValidationResult.error(fieldName, "Date must be 8 numeric digits (CCYYMMDD)");
        }
        // EDIT-DATE-LE: calendar validity via STRICT parse (replaces CEEDAYS)
        try {
            STRICT_CCYYMMDD.parse(v);
        } catch (Exception e) {
            return ValidationResult.error(fieldName, "Date is not a valid calendar date");
        }
        return ValidationResult.ok();
    }

    /**
     * Date-of-birth edit: a valid CCYYMMDD date that is not in the future.
     *
     * <p>Covers {@code EDIT-DATE-OF-BIRTH} (CSUTLDPY.cpy:341), which compares the input
     * against the current date via {@code FUNCTION INTEGER-OF-DATE} and rejects future dates.
     *
     * @param ccyymmdd  the 8-character candidate date of birth
     * @param fieldName the field under edit
     */
    public static ValidationResult validateDateOfBirth(String ccyymmdd, String fieldName) {
        ValidationResult base = validateCcyymmdd(ccyymmdd, fieldName);
        if (base.isError()) {
            return base;
        }
        LocalDate dob = LocalDate.parse(ccyymmdd.trim(), STRICT_CCYYMMDD);
        if (dob.isAfter(LocalDate.now())) { // EDIT-DATE-OF-BIRTH future-date check
            return ValidationResult.error(fieldName, "Date of birth cannot be in the future");
        }
        return ValidationResult.ok();
    }
}
