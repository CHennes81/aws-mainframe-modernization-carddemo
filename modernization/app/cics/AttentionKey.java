package modernization.app.cics;

/**
 * Logical attention key derived from the CICS {@code EIBAID} byte — the Java home for the
 * {@code YYYY-STORE-PFKEY} routine in {@code CSSTRPFY.cpy} (used by {@code COACTUPC},
 * {@code COACTVWC}, {@code COCRDLIC}, {@code COCRDSLC}, {@code COCRDUPC}).
 *
 * <p>This is a pure, deterministic {@code byte -> key} mapping with no session state, which
 * is why it lives on its own rather than inside {@link AbstractScreenController}: the COBOL
 * copybook is a stateless {@code EVALUATE} over {@code EIBAID}, the opposite of the
 * commarea-mutating header/navigation paragraphs.
 *
 * <p><b>Shifted-key business rule.</b> {@code CSSTRPFY} collapses PF13-PF24 onto PF01-PF12
 * (the 3270 "shifted PF key" convention): pressing PF13 is reported as {@link #PF01}, PF24
 * as {@link #PF12}, and so on. That rule is preserved here — the enum has no PF13-PF24
 * members, and {@link #resolve(byte)} maps both ranges to PF01-PF12. The collapse is noted
 * for the discrepancy register.
 *
 * <p>The byte constants are the standard IBM 3270 AID values from the CICS {@code DFHAID}
 * copybook (EBCDIC). {@code EIBAID} carries the EBCDIC code point on the host; a Java/CICS
 * runtime that surfaces it differently must translate before calling {@link #resolve(byte)}.
 */
public enum AttentionKey {
    ENTER, CLEAR, PA1, PA2, PA3,
    PF01, PF02, PF03, PF04, PF05, PF06,
    PF07, PF08, PF09, PF10, PF11, PF12,
    /** Any AID byte not recognised by {@code CSSTRPFY} (the {@code EVALUATE} has no match). */
    UNKNOWN;

    // ---- DFHAID standard 3270 AID byte values (EBCDIC) ----
    private static final int DFHENTER = 0x7D;
    private static final int DFHCLEAR = 0x6D;
    private static final int DFHPA1   = 0x6C, DFHPA2 = 0x6E, DFHPA3 = 0x6B;
    private static final int DFHPF1  = 0xF1, DFHPF2  = 0xF2, DFHPF3  = 0xF3, DFHPF4  = 0xF4;
    private static final int DFHPF5  = 0xF5, DFHPF6  = 0xF6, DFHPF7  = 0xF7, DFHPF8  = 0xF8;
    private static final int DFHPF9  = 0xF9, DFHPF10 = 0x7A, DFHPF11 = 0x7B, DFHPF12 = 0x7C;
    private static final int DFHPF13 = 0xC1, DFHPF14 = 0xC2, DFHPF15 = 0xC3, DFHPF16 = 0xC4;
    private static final int DFHPF17 = 0xC5, DFHPF18 = 0xC6, DFHPF19 = 0xC7, DFHPF20 = 0xC8;
    private static final int DFHPF21 = 0xC9, DFHPF22 = 0x4A, DFHPF23 = 0x4B, DFHPF24 = 0x4C;

    /**
     * Resolve a raw {@code EIBAID} byte to its logical key, applying the PF13-PF24 collapse.
     *
     * @param eibaid the CICS attention identifier byte
     * @return the logical key, or {@link #UNKNOWN} if the byte matches no AID
     */
    public static AttentionKey resolve(byte eibaid) {
        switch (eibaid & 0xFF) {
            case DFHENTER: return ENTER;
            case DFHCLEAR: return CLEAR;
            case DFHPA1:   return PA1;
            case DFHPA2:   return PA2;
            case DFHPA3:   return PA3;
            case DFHPF1:  case DFHPF13: return PF01;
            case DFHPF2:  case DFHPF14: return PF02;
            case DFHPF3:  case DFHPF15: return PF03;
            case DFHPF4:  case DFHPF16: return PF04;
            case DFHPF5:  case DFHPF17: return PF05;
            case DFHPF6:  case DFHPF18: return PF06;
            case DFHPF7:  case DFHPF19: return PF07;
            case DFHPF8:  case DFHPF20: return PF08;
            case DFHPF9:  case DFHPF21: return PF09;
            case DFHPF10: case DFHPF22: return PF10;
            case DFHPF11: case DFHPF23: return PF11;
            case DFHPF12: case DFHPF24: return PF12;
            default: return UNKNOWN;
        }
    }
}
