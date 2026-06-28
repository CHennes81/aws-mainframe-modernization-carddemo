package modernization.app.common.validation;

import java.util.Set;

/**
 * US geographic reference data — the Java home for the five lookup tables in
 * {@code CSLKPCDY.cpy} (1,318 lines, ~59 KB).
 *
 * <p>All data is extracted verbatim from the copybook; nothing is fabricated.
 *
 * <h2>Area code table design</h2>
 * {@code CSLKPCDY} defines three distinct 88-level condition names on the same
 * {@code WS-US-PHONE-AREA-CODE-TO-EDIT PIC XXX} field, forming a clean partition:
 *
 * <ul>
 *   <li>{@link #ALL_NANP_AREA_CODES} (489 codes, {@code VALID-PHONE-AREA-CODE}) — the
 *       complete NANP universe: real geographic codes plus "easy-to-recognize" patterns
 *       (200, 211, 222, 333, 555 …) useful for demos and test data generation.</li>
 *   <li>{@link #EASY_RECOGNIZABLE_AREA_CODES} (80 codes, {@code VALID-EASY-RECOG-AREA-CODE})
 *       — the easy-to-recognize patterns exclusively. Every code repeats a digit
 *       (222, 333, 444, 555 …) or follows an obvious sequence (211, 311, 411 …). These
 *       are identifiable as synthetic test-data numbers at a glance.</li>
 *   <li>{@link #GENERAL_PURPOSE_AREA_CODES} (410 codes, {@code VALID-GENERAL-PURP-CODE})
 *       — real geographic area codes only; exactly {@code ALL_NANP} minus
 *       {@code EASY_RECOGNIZABLE}. <b>This is the set used by {@code COACTUPC.cbl:2298}
 *       to validate customer phone numbers</b> (line: {@code IF VALID-GENERAL-PURP-CODE}).</li>
 * </ul>
 *
 * <p>Relationship: {@code GENERAL_PURPOSE = ALL_NANP − EASY_RECOGNIZABLE} (verified
 * programmatically against the copybook).
 *
 * <h2>Other tables</h2>
 * <ul>
 *   <li>{@link #STATE_CODES} (56 codes, {@code VALID-US-STATE-CODE}) — all 50 US states
 *       plus DC, AS, GU, MP, PR, VI. Used by {@code COACTUPC.cbl:2495}.</li>
 *   <li>{@link #STATE_ZIP_PREFIXES} (240 4-char codes, {@code VALID-US-STATE-ZIP-CD2-COMBO})
 *       — valid {@code state + first-2-of-zip} combinations (e.g. {@code "CA90"}, {@code "NY10"}).
 *       Used by {@code COACTUPC.cbl:2542}. Constructed at call time as
 *       {@code stateCode + zip.substring(0,2)}.</li>
 * </ul>
 *
 * <p>Source: {@code app/cpy/CSLKPCDY.cpy}, CardDemo v2.0-25-gdb72e6b.
 * Data sourced by the COBOL author from NANPA ({@code nationalnanpa.com}) and USPS.
 */
public final class UsGeographyData {

    private UsGeographyData() {}

    // ------------------------------------------------------------------
    // VALID-PHONE-AREA-CODE (CSLKPCDY.cpy:30) — 489 codes
    // Complete NANP area code universe (real geographic + easy-recognizable).
    // Not directly used by COACTUPC; provided for completeness and test data
    // generation scenarios.
    // TODO: Check for Reference — confirm whether any other COBOL program in the
    // corpus tests VALID-PHONE-AREA-CODE. If no caller exists this constant is
    // dead code and can be removed; if a caller is found, wire it up then.
    // ------------------------------------------------------------------
    public static final Set<String> ALL_NANP_AREA_CODES = Set.of(
        "200", "201", "202", "203", "204", "205", "206", "207", "208", "209",
        "210", "211", "212", "213", "214", "215", "216", "217", "218", "219",
        "220", "222", "223", "224", "225", "226", "228", "229", "231", "233",
        "234", "236", "239", "240", "242", "244", "246", "248", "249", "250",
        "251", "252", "253", "254", "255", "256", "260", "262", "264", "266",
        "267", "268", "269", "270", "272", "276", "277", "279", "281", "284",
        "288", "289", "299", "300", "301", "302", "303", "304", "305", "306",
        "307", "308", "309", "310", "311", "312", "313", "314", "315", "316",
        "317", "318", "319", "320", "321", "322", "323", "325", "326", "330",
        "331", "332", "333", "334", "336", "337", "339", "340", "341", "343",
        "344", "345", "346", "347", "351", "352", "355", "360", "361", "364",
        "365", "366", "367", "368", "377", "380", "385", "386", "388", "399",
        "400", "401", "402", "403", "404", "405", "406", "407", "408", "409",
        "410", "411", "412", "413", "414", "415", "416", "417", "418", "419",
        "422", "423", "424", "425", "430", "431", "432", "433", "434", "435",
        "437", "438", "440", "441", "442", "443", "444", "445", "447", "448",
        "450", "455", "458", "463", "464", "466", "469", "470", "473", "474",
        "475", "477", "478", "479", "480", "484", "488", "499", "500", "501",
        "502", "503", "504", "505", "506", "507", "508", "509", "510", "511",
        "512", "513", "514", "515", "516", "517", "518", "519", "520", "522",
        "530", "531", "533", "534", "539", "540", "541", "544", "548", "551",
        "555", "559", "561", "562", "563", "564", "566", "567", "570", "571",
        "572", "573", "574", "575", "577", "579", "580", "581", "582", "585",
        "586", "587", "588", "599", "600", "601", "602", "603", "604", "605",
        "606", "607", "608", "609", "610", "611", "612", "613", "614", "615",
        "616", "617", "618", "619", "620", "622", "623", "626", "628", "629",
        "630", "631", "633", "636", "639", "640", "641", "644", "646", "647",
        "649", "650", "651", "655", "656", "657", "658", "659", "660", "661",
        "662", "664", "666", "667", "669", "670", "671", "672", "677", "678",
        "680", "681", "682", "683", "684", "688", "689", "699", "700", "701",
        "702", "703", "704", "705", "706", "707", "708", "709", "711", "712",
        "713", "714", "715", "716", "717", "718", "719", "720", "721", "722",
        "724", "725", "726", "727", "731", "732", "733", "734", "737", "740",
        "742", "743", "744", "747", "753", "754", "755", "757", "758", "760",
        "762", "763", "765", "766", "767", "769", "770", "771", "772", "773",
        "774", "775", "777", "778", "779", "780", "781", "782", "784", "785",
        "786", "787", "788", "799", "800", "801", "802", "803", "804", "805",
        "806", "807", "808", "809", "810", "811", "812", "813", "814", "815",
        "816", "817", "818", "819", "820", "822", "825", "826", "828", "829",
        "830", "831", "832", "833", "838", "839", "840", "843", "844", "845",
        "847", "848", "849", "850", "854", "855", "856", "857", "858", "859",
        "860", "862", "863", "864", "865", "866", "867", "868", "869", "870",
        "872", "873", "876", "877", "878", "888", "899", "900", "901", "902",
        "903", "904", "905", "906", "907", "908", "909", "910", "911", "912",
        "913", "914", "915", "916", "917", "918", "919", "920", "922", "925",
        "928", "929", "930", "931", "933", "934", "936", "937", "938", "939",
        "940", "941", "943", "944", "945", "947", "948", "949", "951", "952",
        "954", "955", "956", "959", "966", "970", "971", "972", "973", "977",
        "978", "979", "980", "983", "984", "985", "986", "988", "989"
    );

    // ------------------------------------------------------------------
    // VALID-EASY-RECOG-AREA-CODE (CSLKPCDY.cpy:931) — 80 codes
    // Easy-to-recognise "synthetic" patterns: repeating digits (222, 333,
    // 555 …) and N11 service codes (211, 311, 411 …). Used to mark test
    // and demo data as synthetic; excluded from customer-facing validation.
    // TODO: Check for Reference — confirm whether any other COBOL program in the
    // corpus tests VALID-EASY-RECOG-AREA-CODE. If no caller exists this constant
    // is dead code and can be removed; if a caller is found, wire it up then.
    // ------------------------------------------------------------------
    public static final Set<String> EASY_RECOGNIZABLE_AREA_CODES = Set.of(
        "200", "211", "222", "233", "244", "255", "266", "277", "288", "299",
        "300", "311", "322", "333", "344", "355", "366", "377", "388", "399",
        "400", "411", "422", "433", "444", "455", "466", "477", "488", "499",
        "500", "511", "522", "533", "544", "555", "566", "577", "588", "599",
        "600", "611", "622", "633", "644", "655", "666", "677", "688", "699",
        "700", "711", "722", "733", "744", "755", "766", "777", "788", "799",
        "800", "811", "822", "833", "844", "855", "866", "877", "888", "899",
        "900", "911", "922", "933", "944", "955", "966", "977", "988", "999"
    );

    // ------------------------------------------------------------------
    // VALID-GENERAL-PURP-CODE (CSLKPCDY.cpy:521) — 410 codes
    // Real geographic NANP area codes only (ALL_NANP minus EASY_RECOGNIZABLE).
    // THIS IS THE SET USED BY COACTUPC.cbl:2298 to validate customer phones.
    // ------------------------------------------------------------------
    public static final Set<String> GENERAL_PURPOSE_AREA_CODES = Set.of(
        "201", "202", "203", "204", "205", "206", "207", "208", "209", "210",
        "212", "213", "214", "215", "216", "217", "218", "219", "220", "223",
        "224", "225", "226", "228", "229", "231", "234", "236", "239", "240",
        "242", "246", "248", "249", "250", "251", "252", "253", "254", "256",
        "260", "262", "264", "267", "268", "269", "270", "272", "276", "279",
        "281", "284", "289", "301", "302", "303", "304", "305", "306", "307",
        "308", "309", "310", "312", "313", "314", "315", "316", "317", "318",
        "319", "320", "321", "323", "325", "326", "330", "331", "332", "334",
        "336", "337", "339", "340", "341", "343", "345", "346", "347", "351",
        "352", "360", "361", "364", "365", "367", "368", "380", "385", "386",
        "401", "402", "403", "404", "405", "406", "407", "408", "409", "410",
        "412", "413", "414", "415", "416", "417", "418", "419", "423", "424",
        "425", "430", "431", "432", "434", "435", "437", "438", "440", "441",
        "442", "443", "445", "447", "448", "450", "458", "463", "464", "469",
        "470", "473", "474", "475", "478", "479", "480", "484", "501", "502",
        "503", "504", "505", "506", "507", "508", "509", "510", "512", "513",
        "514", "515", "516", "517", "518", "519", "520", "530", "531", "534",
        "539", "540", "541", "548", "551", "559", "561", "562", "563", "564",
        "567", "570", "571", "572", "573", "574", "575", "579", "580", "581",
        "582", "585", "586", "587", "601", "602", "603", "604", "605", "606",
        "607", "608", "609", "610", "612", "613", "614", "615", "616", "617",
        "618", "619", "620", "623", "626", "628", "629", "630", "631", "636",
        "639", "640", "641", "646", "647", "649", "650", "651", "656", "657",
        "658", "659", "660", "661", "662", "664", "667", "669", "670", "671",
        "672", "678", "680", "681", "682", "683", "684", "689", "701", "702",
        "703", "704", "705", "706", "707", "708", "709", "712", "713", "714",
        "715", "716", "717", "718", "719", "720", "721", "724", "725", "726",
        "727", "731", "732", "734", "737", "740", "742", "743", "747", "753",
        "754", "757", "758", "760", "762", "763", "765", "767", "769", "770",
        "771", "772", "773", "774", "775", "778", "779", "780", "781", "782",
        "784", "785", "786", "787", "801", "802", "803", "804", "805", "806",
        "807", "808", "809", "810", "812", "813", "814", "815", "816", "817",
        "818", "819", "820", "825", "826", "828", "829", "830", "831", "832",
        "838", "839", "840", "843", "845", "847", "848", "849", "850", "854",
        "856", "857", "858", "859", "860", "862", "863", "864", "865", "867",
        "868", "869", "870", "872", "873", "876", "878", "901", "902", "903",
        "904", "905", "906", "907", "908", "909", "910", "912", "913", "914",
        "915", "916", "917", "918", "919", "920", "925", "928", "929", "930",
        "931", "934", "936", "937", "938", "939", "940", "941", "943", "945",
        "947", "948", "949", "951", "952", "954", "956", "959", "970", "971",
        "972", "973", "978", "979", "980", "983", "984", "985", "986", "989"
    );

    // ------------------------------------------------------------------
    // VALID-US-STATE-CODE (CSLKPCDY.cpy:1013) — 56 codes
    // All 50 states plus DC, AS (Am. Samoa), GU (Guam), MP (N. Mariana Is.),
    // PR (Puerto Rico), VI (US Virgin Islands).
    // Used by COACTUPC.cbl:2495 (1270-EDIT-US-STATE-CD).
    // ------------------------------------------------------------------
    public static final Set<String> STATE_CODES = Set.of(
        "AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE",
        "FL", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY",
        "LA", "MA", "MD", "ME", "MI", "MN", "MO", "MP", "MS", "MT",
        "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK",
        "OR", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VA",
        "VI", "VT", "WA", "WI", "WV", "WY"
    );

    // ------------------------------------------------------------------
    // VALID-US-STATE-ZIP-CD2-COMBO (CSLKPCDY.cpy:1073) — 240 4-char codes
    // Valid state + first-2-of-zip concatenations (e.g. "CA90", "NY10").
    // Used by COACTUPC.cbl:2542 (1280-EDIT-US-STATE-ZIP-CD) via
    //   STRING state-cd zip(1:2) INTO US-STATE-AND-FIRST-ZIP2.
    // Pass stateCode + zip.substring(0, 2) to isValidStateZip().
    // ------------------------------------------------------------------
    public static final Set<String> STATE_ZIP_PREFIXES = Set.of(
        "AA34", "AE90", "AE91", "AE92", "AE93", "AE94", "AE95", "AE96", "AE97", "AE98",
        "AK99", "AL35", "AL36", "AP96", "AR71", "AR72", "AS96", "AZ85", "AZ86", "CA90",
        "CA91", "CA92", "CA93", "CA94", "CA95", "CA96", "CO80", "CO81", "CT60", "CT61",
        "CT62", "CT63", "CT64", "CT65", "CT66", "CT67", "CT68", "CT69", "DC20", "DC56",
        "DC88", "DE19", "FL32", "FL33", "FL34", "FM96", "GA30", "GA31", "GA39", "GU96",
        "HI96", "IA50", "IA51", "IA52", "ID83", "IL60", "IL61", "IL62", "IN46", "IN47",
        "KS66", "KS67", "KY40", "KY41", "KY42", "LA70", "LA71", "MA10", "MA11", "MA12",
        "MA13", "MA14", "MA15", "MA16", "MA17", "MA18", "MA19", "MA20", "MA21", "MA22",
        "MA23", "MA24", "MA25", "MA26", "MA27", "MA55", "MD20", "MD21", "ME39", "ME40",
        "ME41", "ME42", "ME43", "ME44", "ME45", "ME46", "ME47", "ME48", "ME49", "MH96",
        "MI48", "MI49", "MN55", "MN56", "MO63", "MO64", "MO65", "MO72", "MP96", "MS38",
        "MS39", "MT59", "NC27", "NC28", "ND58", "NE68", "NE69", "NH30", "NH31", "NH32",
        "NH33", "NH34", "NH35", "NH36", "NH37", "NH38", "NJ70", "NJ71", "NJ72", "NJ73",
        "NJ74", "NJ75", "NJ76", "NJ77", "NJ78", "NJ79", "NJ80", "NJ81", "NJ82", "NJ83",
        "NJ84", "NJ85", "NJ86", "NJ87", "NJ88", "NJ89", "NM87", "NM88", "NV88", "NV89",
        "NY10", "NY11", "NY12", "NY13", "NY14", "NY50", "NY54", "NY63", "OH43", "OH44",
        "OH45", "OK73", "OK74", "OR97", "PA15", "PA16", "PA17", "PA18", "PA19", "PR60",
        "PR61", "PR62", "PR63", "PR64", "PR65", "PR66", "PR67", "PR68", "PR69", "PR70",
        "PR71", "PR72", "PR73", "PR74", "PR75", "PR76", "PR77", "PR78", "PR79", "PR90",
        "PR91", "PR92", "PR93", "PR94", "PR95", "PR96", "PR97", "PR98", "PW96", "RI28",
        "RI29", "SC29", "SD57", "TN37", "TN38", "TX73", "TX75", "TX76", "TX77", "TX78",
        "TX79", "TX88", "UT84", "VA20", "VA22", "VA23", "VA24", "VI80", "VI82", "VI83",
        "VI84", "VI85", "VT50", "VT51", "VT52", "VT53", "VT54", "VT56", "VT57", "VT58",
        "VT59", "WA98", "WA99", "WI53", "WI54", "WV24", "WV25", "WV26", "WY82", "WY83"
    );

    // ------------------------------------------------------------------
    // Public API — mirrors the three COACTUPC lookup calls
    // ------------------------------------------------------------------

    /**
     * @return true if {@code areaCode} is a real geographic NANP area code
     *         ({@code VALID-GENERAL-PURP-CODE}, {@code CSLKPCDY.cpy:521}).
     *         Rejects easy-to-recognise synthetic codes (555, 222, etc.).
     *         This is the check performed by {@code COACTUPC.cbl:2298}.
     */
    public static boolean isValidAreaCode(String areaCode) {
        return GENERAL_PURPOSE_AREA_CODES.contains(areaCode);
    }

    /**
     * @return true if {@code stateCode} is a valid US state/territory code
     *         ({@code VALID-US-STATE-CODE}, {@code CSLKPCDY.cpy:1013}).
     *         Includes all 50 states, DC, and five US territories.
     *         Used by {@code COACTUPC.cbl:2495}.
     */
    public static boolean isValidStateCode(String stateCode) {
        return STATE_CODES.contains(stateCode);
    }

    /**
     * @param stateCode   two-letter state/territory code
     * @param zipFirstTwo first two digits of the zip code
     * @return true if the combination is valid ({@code VALID-US-STATE-ZIP-CD2-COMBO},
     *         {@code CSLKPCDY.cpy:1073}). Used by {@code COACTUPC.cbl:2542}.
     */
    public static boolean isValidStateZip(String stateCode, String zipFirstTwo) {
        return STATE_ZIP_PREFIXES.contains(stateCode + zipFirstTwo);
    }
}
