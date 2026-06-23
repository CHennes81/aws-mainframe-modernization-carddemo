# CardDemo COBOL Program Documentation

**Project:** AWS Mainframe Modernization — CardDemo  
**Source Language:** IBM COBOL / CICS / JCL  
**Audience:** Business Analysts, Modernization Engineers  
**Generated from:** Source files in `app/cbl/`, copybooks in `app/cpy/`, assembler in `app/asm/`

---

## Table of Contents

### CICS Online Programs (User-Facing Screens)
| Program | Transaction ID | Description |
|---------|---------------|-------------|
| [COSGN00C](#cosgn00c) | CC00 | Sign-On / Login |
| [COMEN01C](#comen01c) | CM00 | Main Menu |
| [COADM01C](#coadm01c) | CA00 | Admin Menu |
| [COACTVWC](#coactvwc) | CAVW | Account View (Read-Only) |
| [COACTUPC](#coactupc) | CAUP | Account Update |
| [COBIL00C](#cobil00c) | CB00 | Bill Payment |
| [COCRDLIC](#cocrdlic) | CCLI | Card List |
| [COCRDSLC](#cocrdslc) | CCDL | Card Detail (Read-Only) |
| [COCRDUPC](#cocrdupc) | CCUP | Card Update |
| [COTRN00C](#cotrn00c) | CT00 | Transaction List |
| [COTRN01C](#cotrn01c) | CT01 | Transaction View (Read-Only) |
| [COTRN02C](#cotrn02c) | CT02 | Transaction Add |
| [CORPT00C](#corpt00c) | CR00 | Report Submission |
| [COUSR00C](#cousr00c) | CU00 | User List |
| [COUSR01C](#cousr01c) | CU01 | User Add |
| [COUSR02C](#cousr02c) | CU02 | User Update |
| [COUSR03C](#cousr03c) | CU03 | User Delete |

### Batch Programs (Scheduled / Overnight Processing)
| Program | Job Step Name | Description |
|---------|--------------|-------------|
| [CBACT01C](#cbact01c) | — | Account Master File Report |
| [CBACT02C](#cbact02c) | — | Card Master File Report |
| [CBACT03C](#cbact03c) | — | Card Cross-Reference Report |
| [CBACT04C](#cbact04c) | INTCALC | Interest Calculation |
| [CBCUS01C](#cbcus01c) | — | Customer Master File Report |
| [CBEXPORT](#cbexport) | — | Full Data Export |
| [CBIMPORT](#cbimport) | — | Branch Data Import |
| [CBSTM03A](#cbstm03a) | CREASTMT | Statement Generator |
| [CBSTM03B](#cbstm03b) | — | Statement File I/O Subroutine |
| [CBTRN01C](#cbtrn01c) | — | Transaction Validation (Read-Only) |
| [CBTRN02C](#cbtrn02c) | POSTTRAN | Transaction Posting |
| [CBTRN03C](#cbtrn03c) | — | Transaction Detail Report |
| [COBSWAIT](#cobswait) | — | Timed Wait Utility |

### Standalone Utility Programs
| Program | Description |
|---------|-------------|
| [CSUTLDTC](#csutldtc) | Date Validation Utility |

### Assembler Modules
| Module | Description |
|--------|-------------|
| [COBDATFT](#cobdatft) | Date Format Converter |
| [MVSWAIT](#mvswait) | Interval Timer Wait |

---

<div style="page-break-before: always"></div>

---

## CICS Online Programs

---

<a name="cosgn00c"></a>
## COSGN00C — Sign-On / Login

**Transaction ID:** `CC00`  
**BMS Map:** `COSGN00` / Map `COSGN0A`  
**Entry Point:** This is the first program a user sees; it is the application entry point.

### Purpose

COSGN00C is the login screen for the CardDemo application. It accepts a user ID and password, validates them against the user security file, and routes the authenticated user to the appropriate main menu based on their user type (administrator vs. regular user).

### Inputs

| Source | Description |
|--------|-------------|
| Screen | User ID (up to 8 characters) |
| Screen | Password (up to 8 characters) |
| VSAM File `USRSEC` | User security master — holds user ID, first name, last name, password (plaintext), and user type for each registered user |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Login form with error messages if credentials are invalid |
| COMMAREA | Populated with authenticated user's ID, name, and type for use by downstream programs |
| Transfer to `COADM01C` | If user type is `A` (Administrator) |
| Transfer to `COMEN01C` | If user type is `U` (Regular User) |

### Processing Flow

1. **First visit (no prior COMMAREA):** The program displays a blank login form and waits for user input.

2. **User presses Enter:**
   - Both the user ID and password fields are checked; if either is blank, an error message is displayed and the user is asked to re-enter.
   - The entered user ID is used as the key to look up the record in the `USRSEC` security file.
   - If the user is not found (CICS response code 13 = record not found), an "Invalid credentials" error is displayed — the same generic message used for a wrong password, intentionally giving no hint as to which field was wrong.
   - If the user is found, the entered password is uppercased and compared to the stored password (also uppercased). This makes password comparisons case-insensitive.
   - On a successful match, the user's ID, name, and type are stored in the shared communication area (COMMAREA) for use throughout the session.
   - Administrator users (type `A`) are transferred to the Admin Menu (`COADM01C`).
   - Regular users (type `U`) are transferred to the Main Menu (`COMEN01C`).

3. **PF3 / PF12 (Clear / Exit):** Clears the screen and returns to the login prompt.

### Security Notes

- Passwords are stored in plaintext in the VSAM file. There is no hashing or encryption.
- The same error message is shown for "user not found" and "wrong password" to prevent user enumeration.

### Date / Parameter Dependencies

None. This program does not use dates or accept job parameters.

---

<div style="page-break-before: always"></div>

---

<a name="comen01c"></a>
## COMEN01C — Main Menu

**Transaction ID:** `CM00`  
**BMS Map:** `COMEN01` / Map `COMEN1A`

### Purpose

COMEN01C displays the main navigation menu for regular (non-admin) users. It presents up to 11 numbered menu options, each mapping to a downstream CICS transaction. The user selects an option by number and is transferred to the appropriate program.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Single-character option selection (1–11) |
| COMMAREA | Authenticated user context from COSGN00C (user ID, name, type) |
| Copybook `COMEN02Y` | Defines the 11 menu options and their associated program names |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Numbered menu with current date/time in the header |
| Transfer | CICS XCTL to the selected program (e.g., Account View, Card List, etc.) |

### Menu Options (as defined in COMEN02Y)

| # | Function | Program |
|---|----------|---------|
| 1 | View/Update Account | COACTUPC |
| 2 | View Credit Card | COACTVWC |
| 3 | List Credit Cards | COCRDLIC |
| 4 | Transaction List | COTRN00C |
| 5 | Pay Bill | COBIL00C |
| 6 | View Reports | CORPT00C |
| 7–11 | Additional options | As configured |

### Processing Flow

1. **First visit:** The menu is displayed with the current date and time. The user's name appears in the header.

2. **User enters an option number and presses Enter:**
   - The input is validated to ensure it is a number between 1 and the defined option count (11).
   - The program looks up the COBOL program name corresponding to that option in the menu option table.
   - One option (`COPAUS0C`) is special-cased: the program uses CICS INQUIRE to check whether it is installed before offering it as a choice. If not installed, the option is displayed as "not available."
   - A valid selection triggers a CICS XCTL (transfer of control) to the selected program, passing the COMMAREA.

3. **PF3 (Sign Off):** Transfers control back to `COSGN00C` to sign off.

4. **PF12 (Cancel):** Redisplays the menu.

### Date / Parameter Dependencies

The current date and time are obtained from the CICS system clock and displayed in the screen header. No batch parameters are used.

---

<div style="page-break-before: always"></div>

---

<a name="coadm01c"></a>
## COADM01C — Admin Menu

**Transaction ID:** `CA00`  
**BMS Map:** `COADM1A`

### Purpose

COADM01C displays the administration menu for users with administrator access. It provides access to user management functions (add, update, delete users) and transaction type maintenance. It operates identically in structure to the main menu but serves a different set of functions.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Single-character option selection (1–6) |
| COMMAREA | Authenticated user context (must have user type `A`) |
| Copybook `COADM02Y` | Defines 6 admin menu options and their program names |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Numbered admin menu |
| Transfer | CICS XCTL to the selected admin program |

### Admin Menu Options (as defined in COADM02Y)

| # | Function | Program |
|---|----------|---------|
| 1 | List Users | COUSR00C |
| 2 | Add User | COUSR01C |
| 3 | Update User | COUSR02C |
| 4 | Delete User | COUSR03C |
| 5 | List Transaction Types | COTRTLIC |
| 6 | Update Transaction Type | COTRTUPC |

### Processing Flow

1. **First visit:** Displays the 6 admin options with a header showing the current date and the admin user's name.

2. **User selects an option:**
   - Input is validated (must be 1–6).
   - The program checks if the target program name contains `'DUMMY'` in its first 5 characters; if so, it displays "This option is not yet installed" rather than transferring.
   - A PGMIDERR condition handler is set up to catch cases where the target program is not defined to CICS (displays a "program not found" error gracefully instead of abending).
   - Valid selections transfer control to the appropriate program via CICS XCTL.

3. **PF3 (Sign Off):** Transfers to `COSGN00C`.

### Date / Parameter Dependencies

Current date/time from CICS system clock for header display only.

---

<div style="page-break-before: always"></div>

---

<a name="coactvwc"></a>
## COACTVWC — Account View (Read-Only)

**Transaction ID:** `CAVW`  
**BMS Map:** `CACTVWA`

### Purpose

COACTVWC displays a complete, read-only snapshot of an account record together with the associated customer record. It is the "view details" function for accounts — no data can be changed from this screen.

### Inputs

| Source | Description |
|--------|-------------|
| Screen / COMMAREA | 11-digit Account ID (entered by user or passed by calling program) |
| VSAM File `CXACAIX` | Card cross-reference alternate index, keyed by Account ID — used to look up the card number and customer ID linked to the account |
| VSAM File `ACCTDAT` | Account master, keyed by Account ID — returns all account fields |
| VSAM File `CUSTDAT` | Customer master, keyed by Customer ID — returns all customer fields |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Full account details: balances, credit limits, dates, status, plus full customer name, address, phone, SSN (formatted with dashes), date of birth, FICO score |

### Processing Flow

1. **Enter or re-enter:**
   - On first entry the account ID is taken from the COMMAREA (set by the calling program, e.g., the Account Update screen or the Card List).
   - If no account ID is provided, the user is prompted to type one.

2. **Data retrieval (three-step read chain):**
   - **Step 1 — Cross-reference lookup:** The account ID is used to read the `CXACAIX` alternate index file. This returns the card number and the customer ID linked to that account. If not found, an error is shown ("Account not found in cross-reference file").
   - **Step 2 — Account master read:** The account ID is used to read `ACCTDAT` directly. Returns: account status, current balance, credit limit, cash credit limit, current-cycle credit/debit totals, open date, expiration date, reissue date, and group ID.
   - **Step 3 — Customer master read:** The customer ID (obtained in Step 1) is used to read `CUSTDAT`. Returns: name, address, SSN (displayed as XXX-XX-XXXX), date of birth, FICO score, phone numbers, government ID, EFT account, and primary card holder flag.

3. **Display:** All retrieved data is moved to the BMS screen map and sent to the terminal.

4. **PF3 (Return):** Returns to the calling program (stored in COMMAREA as the "from" program).

### Notable Details

- The SSN is split into 3 parts (3+2+4 digits) for display with dashes between parts; this is purely cosmetic.
- A duplicate paragraph label `0000-MAIN-EXIT` exists at lines 408 and 411 — this is a harmless code defect in the source.
- This program does not use optimistic locking because it is read-only.

### Date / Parameter Dependencies

No batch parameters. Current date/time from CICS clock for screen header.

---

<div style="page-break-before: always"></div>

---

<a name="coactupc"></a>
## COACTUPC — Account Update

**Transaction ID:** `CAUP`  
**BMS Map:** `CACTUPA`

### Purpose

COACTUPC is the most complex online program in the application. It allows authorized users to update both account master data (financial limits, balances, dates, status, group) and the associated customer demographic data (name, address, phone, SSN, date of birth, FICO score, government ID, EFT account) in a single screen. It implements optimistic locking to prevent two users from overwriting each other's changes.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Account ID (search key), then all editable account and customer fields |
| COMMAREA | From-program, user context, account/customer IDs from prior navigation |
| VSAM `CXACAIX` | Cross-reference alternate index (account ID → card number + customer ID) |
| VSAM `ACCTDAT` | Account master (read for display; read-with-lock for update) |
| VSAM `CUSTDAT` | Customer master (read for display; read-with-lock for update) |
| Copybook `CSUTLDWY` / `CSUTLDPY` | Date validation working storage and procedure paragraphs |
| Copybook `CSLKPCDY` | US phone area code table, US state code table, state+zip combination table |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Account + customer data for review/edit, with field-level error highlighting in red |
| VSAM `ACCTDAT` | Rewritten account record if update is confirmed |
| VSAM `CUSTDAT` | Rewritten customer record if update is confirmed |

### State Machine

The program uses an explicit state variable (`ACUP-CHANGE-ACTION`) to manage the multi-step update flow. The states are:

| State | Meaning |
|-------|---------|
| LOW-VALUES (initial) | Waiting for user to enter an account ID to search |
| `S` (Show details) | Account found; displaying current values for editing |
| `E` (Enter changes) | User is actively editing fields |
| `N` (No changes detected) | User pressed confirm but no fields were actually changed |
| `C` (Changes OK, not yet confirmed) | All edits passed validation; awaiting final Y/N confirmation |
| `L` (Lock error) | Could not obtain exclusive lock on the record |
| `F` (Failed) | Update was attempted but the CICS REWRITE failed |

### Processing Flow

1. **Account Search (State = initial / LOW-VALUES):**
   - User types an 11-digit account ID and presses Enter.
   - The program performs the same three-step read chain as COACTVWC: cross-reference → account master → customer master.
   - If found, current values are stored in `ACUP-OLD-DETAILS` (the "snapshot" for optimistic locking) and displayed on screen.
   - State advances to `S` (Show Details).

2. **Editing (State = S or E):**
   - All editable fields are unprotected (cursor-addressable).
   - User modifies desired fields.
   - On Enter, the program validates every field:

   **Account field validations:**
   - Active Status: must be `Y` or `N`
   - Current Balance, Credit Limit, Cash Credit Limit, Cycle Credit, Cycle Debit: must be valid signed decimal numbers
   - Open Date, Expiration Date, Reissue Date: each split into Year/Month/Day; validated for calendar correctness (including leap years) using the `CSUTLDPY` copybook paragraphs and the `CSUTLDTC` date utility
   - Group ID: must not be blank

   **Customer field validations:**
   - First Name, Last Name: required (not blank)
   - Middle Name: optional
   - SSN: three parts validated as numeric; combined SSN must not be all zeros
   - Date of Birth: validated as a valid calendar date; must not be a future date
   - FICO Score: must be numeric, 3 digits
   - Address Line 1: required
   - State Code: must be a valid US state/territory abbreviation (from the `CSLKPCDY` lookup table)
   - Zip Code: must be 5 digits and consistent with the selected state (from the state+zip combination table)
   - City: required
   - Country: required (hardcoded US validations apply)
   - Phone numbers (both primary and secondary): area code validated against NANPA table; prefix and line number must be numeric
   - Government ID: must not be blank
   - EFT Account ID: must not be blank
   - Primary Card Holder flag: must be `Y` or `N`

3. **Validation failure:** Any field that fails validation is highlighted in red; a `*` is placed in the field if it was left blank. The cursor is positioned at the first failing field. The user must correct all errors before proceeding.

4. **Change detection (State = E → C or N):**
   - If all fields pass validation, the program compares each new value to the corresponding stored-old value (`ACUP-OLD-DETAILS`).
   - If nothing changed, state is set to `N` and a "No changes detected" message is shown.
   - If changes are found, state is set to `C` and a confirmation prompt is displayed: "Press PF5 to confirm, PF12 to cancel."

5. **Confirmation and Write (PF5):**
   - The program re-reads both the account and customer records with an UPDATE (exclusive lock).
   - Before writing, `9700-CHECK-CHANGE-IN-REC` compares the freshly-locked data against `ACUP-OLD-DETAILS` field by field. If any field differs (i.e., another user updated the record while this user was editing), the update is rejected with a "data was changed by another user" message — this is the optimistic locking check.
   - If the locked data still matches the snapshot, the program writes back the new values:
     - Account update record (ACCT-UPDATE-RECORD) is assembled from `ACUP-NEW-DETAILS` and written with CICS REWRITE to `ACCTDAT`.
     - Customer update record (CUST-UPDATE-RECORD) is assembled and written with CICS REWRITE to `CUSTDAT`.
     - Phone numbers are re-assembled into `(aaa)bbb-cccc` format before writing.
     - Dates are re-assembled into `YYYY-MM-DD` format before writing.
   - If the customer REWRITE fails after the account REWRITE has already succeeded, a CICS SYNCPOINT ROLLBACK is issued to undo the account change.
   - On success, state is set to "changes okayed and done" and a success message is displayed.

6. **PF12 (Cancel):** Discards changes and redisplays the original values.

7. **PF3 (Return):** Returns to the calling program.

### Screen Attribute Management

- Fields are fully protected (read-only) when displaying original values.
- All editable fields are unprotected when the user is in edit mode.
- The Account ID field itself is always protected once an account is loaded (it is the primary key and cannot be changed).
- Customer ID is always protected (display only).
- Country code is always protected because all validations are US-specific.

### Date / Parameter Dependencies

- Relies on `CSUTLDTC` (and through it, the IBM Language Environment `CEEDAYS` service) for calendar date validation.
- No batch PARM date is accepted — dates come entirely from screen input.

---

<div style="page-break-before: always"></div>

---

<a name="cobil00c"></a>
## COBIL00C — Bill Payment

**Transaction ID:** `CB00`  
**BMS Map:** `COBIL0A`

### Purpose

COBIL00C allows a user to submit a payment toward their credit card account balance. It reduces the account's current balance by the payment amount and writes a corresponding payment transaction record.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Account ID, payment amount |
| COMMAREA | User context |
| VSAM `ACCTDAT` | Account master (read with UPDATE lock) |
| VSAM `TRANSACT` | Transaction file (browsed backward from end to find the last transaction ID; then written) |
| Copybook `CVACT01Y` | Account record layout |
| Copybook `CVTRA05Y` | Transaction record layout |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Confirmation of payment with new balance, or error messages |
| VSAM `ACCTDAT` | Account record rewritten with reduced balance |
| VSAM `TRANSACT` | New payment transaction record written |

### Processing Flow

1. **Display payment form:** User enters their account number and the payment amount.

2. **Validation:**
   - Account ID must not be blank.
   - Account ID is used to read the account with an UPDATE lock (exclusive). If the account is not found, an error is shown.
   - Payment amount must be a valid positive number.

3. **Generate next Transaction ID:**
   - The program browses the `TRANSACT` file backward from the very end (using a HIGH-VALUES start key) to find the most recently written transaction.
   - The new transaction ID is set to the last found ID plus 1 (numeric increment).
   - If no transactions exist yet, a seed ID is used.

4. **Post the payment:**
   - A new transaction record is built with:
     - Transaction type: `02` (payment)
     - Transaction category: `2`
     - Source: `POS TERM`
     - Amount: the entered payment amount (positive)
     - Card number, timestamps, merchant info from the account/session context
   - The transaction is written to `TRANSACT`.
   - The account balance is reduced: `New Balance = Current Balance − Payment Amount`.
   - The account record is rewritten to `ACCTDAT` with the new balance.

5. **Confirmation:** A success screen shows the payment amount and the updated balance.

6. **PF3 (Return):** Returns to the calling program.

### Date / Parameter Dependencies

Transaction timestamps (original and processing) are populated from the CICS system clock in `YYYY-MM-DD HH:MM:SS` format.

---

<div style="page-break-before: always"></div>

---

<a name="cocrdlic"></a>
## COCRDLIC — Card List

**Transaction ID:** `CCLI`  
**BMS Map:** `COCRDLI` / Map `CCRDLIA`

### Purpose

COCRDLIC displays a paginated list of credit cards stored in the card master file. It can be filtered by account ID or card number. Users can select a card to view its details or open it for editing.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Optional account ID filter, optional card number filter; selection code `S` (view) or `U` (update) against a listed card |
| COMMAREA | User context, previously selected account/card ID |
| VSAM `CARDDAT` | Card master file (browsed forward/backward for pagination) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Up to 7 card records per page, showing card number, account ID, active status, and embossed name |
| COMMAREA | Selected card number stored for passing to detail/update program |
| Transfer to `COCRDSLC` | When user marks a row with `S` (view detail) |
| Transfer to `COCRDUPC` | When user marks a row with `U` (update card) |

### Processing Flow

1. **Initial display:** If an account ID is in the COMMAREA (from a prior program), cards for that account are shown. Otherwise, all cards are shown starting from the beginning of the file.

2. **Pagination — Forward (PF8 / Next Page):**
   - Positions the VSAM browse cursor (`STARTBR`) at the first card number from the current page.
   - Reads forward with `READNEXT` to populate up to 7 screen rows.
   - The first and last card numbers on the page are saved in COMMAREA to enable backward navigation.

3. **Pagination — Backward (PF7 / Prev Page):**
   - Positions the browse cursor at the first card on the current page.
   - Reads backward with `READPREV` to find the previous page's records.
   - Redisplays those 7 records.

4. **Card selection:**
   - The user types `S` or `U` in the selection column next to a card row.
   - Only one selection is accepted per Enter key press.
   - `S` → transfers to `COCRDSLC` (Card Detail, read-only).
   - `U` → transfers to `COCRDUPC` (Card Update).
   - The selected card number is stored in COMMAREA before the transfer.

5. **PF3 (Return):** Returns to the calling program.

### Date / Parameter Dependencies

No dates processed. Screen header shows current date/time from CICS clock.

---

<div style="page-break-before: always"></div>

---

<a name="cocrdslc"></a>
## COCRDSLC — Card Detail (Read-Only)

**Transaction ID:** `CCDL`  
**BMS Map:** `COCRDSL` / Map `CCRDSLA`

### Purpose

COCRDSLC displays the full details of a single credit card record. It is a read-only view invoked from the Card List screen.

### Inputs

| Source | Description |
|--------|-------------|
| COMMAREA | Card number (passed from COCRDLIC) |
| VSAM `CARDDAT` | Card master file, read by card number |
| VSAM `CUSTDAT` | Customer master, read by customer ID to display cardholder name |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Card number, account ID, CVV code, embossed name, expiration date, active status, and associated customer name |

### Processing Flow

1. **Read card record:** The card number from the COMMAREA is used as the key to read `CARDDAT` directly. If not found, an error message is displayed.

2. **Read customer name:** The customer ID associated with the card is used to read `CUSTDAT` for the cardholder name.

3. **Display:** All card fields are shown in protected (non-editable) mode.

4. **PF3 (Return):** Returns to the Card List (`COCRDLIC`) or the originating program stored in COMMAREA.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cocrdupc"></a>
## COCRDUPC — Card Update

**Transaction ID:** `CCUP`  
**BMS Map:** `COCRDUP` / Map `CCRDUPA`

### Purpose

COCRDUPC allows authorized users to update a limited set of card attributes: the embossed name on the card, the card's active status (Y/N), and the expiration date (month and year separately). Like COACTUPC, it uses optimistic locking to detect concurrent modifications.

### Inputs

| Source | Description |
|--------|-------------|
| COMMAREA | Card number (passed from COCRDLIC) |
| Screen | Editable fields: embossed name, active status, expiry month, expiry year |
| VSAM `CARDDAT` | Card master (read for display; read-with-UPDATE-lock for write) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Card data with editable fields; error messages if validation fails |
| VSAM `CARDDAT` | Card record rewritten with updated fields |

### Processing Flow

1. **Load current card data:**
   - Card number from COMMAREA is used to read `CARDDAT`.
   - Current values are stored in `CCUP-OLD-DETAILS` (the optimistic-lock snapshot).
   - Editable fields are displayed in unprotected mode.

2. **User edits and presses Enter:**
   - Active Status: must be `Y` or `N`.
   - Expiry Month: must be a number from 1 to 12.
   - Expiry Year: must be a number from 1950 to 2099.
   - Embossed Name: must not be blank.

3. **Change detection:** The new values are compared against `CCUP-OLD-DETAILS`. If nothing changed, a "no changes detected" message is shown.

4. **Confirmation:** If changes are detected and valid, the user is prompted to confirm (PF5) or cancel (PF12).

5. **Write (PF5 — Confirm):**
   - The card record is re-read with UPDATE lock.
   - `9300-CHECK-CHANGE-IN-REC` compares the freshly-locked values against `CCUP-OLD-DETAILS`. If another user has changed the record in the interim, the update is rejected.
   - If unchanged, the card record is rewritten with the new values via CICS REWRITE.
   - A CICS SYNCPOINT is issued before transferring control back, to commit the update.

6. **PF3 (Return):** Returns to the calling program.

### Date / Parameter Dependencies

No date processing. Expiration year/month are numeric range-validated only (not checked against today's date).

---

<div style="page-break-before: always"></div>

---

<a name="cotrn00c"></a>
## COTRN00C — Transaction List

**Transaction ID:** `CT00`  
**BMS Map:** `COTRN00` / Map `COTRN0A`

### Purpose

COTRN00C displays a paginated list of transaction records from the transaction master file. The user can page forward and backward through the list and select a transaction to view its details.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Selection code `S` next to a transaction row |
| COMMAREA | First and last transaction IDs from the current page (for pagination) |
| VSAM `TRANSACT` | Transaction file (browsed using STARTBR/READNEXT/READPREV) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Up to 10 transaction rows per page showing: transaction ID, type, category, source, amount, and merchant name |
| COMMAREA | Selected transaction ID stored for passing to COTRN01C |
| Transfer to `COTRN01C` | On selection |

### Processing Flow

1. **Initial display:** Browses the `TRANSACT` file from the beginning and displays the first 10 transactions.

2. **PF8 (Next page):** Resumes the browse from the transaction after the last one shown, reads the next 10.

3. **PF7 (Previous page):** Repositions to the first transaction on the current page, then reads backward 10 records to reconstruct the previous page.

4. **Selection (`S`):** Saves the selected transaction ID in COMMAREA and transfers to `COTRN01C` (Transaction View).

5. **PF3 (Return):** Returns to the main menu.

### Date / Parameter Dependencies

No date filtering on this screen. Date/time in header from CICS clock.

---

<div style="page-break-before: always"></div>

---

<a name="cotrn01c"></a>
## COTRN01C — Transaction View (Read-Only)

**Transaction ID:** `CT01`  
**BMS Map:** `COTRN01` / Map `COTRN1A`

### Purpose

COTRN01C displays all fields of a single transaction record in read-only mode. It can be reached from the Transaction List or directly by typing the transaction ID.

### Inputs

| Source | Description |
|--------|-------------|
| COMMAREA / Screen | Transaction ID (16-character alphanumeric key) |
| VSAM `TRANSACT` | Transaction master file, read with UPDATE lock (for display only — the lock ensures a consistent read and is released on exit) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Full transaction record: ID, type, category, source, description, amount, merchant ID, merchant name, city, zip, card number, original transaction timestamp, processing timestamp |

### Processing Flow

1. **Read transaction:** The transaction ID is used to read `TRANSACT` with an UPDATE lock (READ UPDATE). Although this is a display-only program, the UPDATE lock is used to guarantee a consistent read.

2. **Display:** All fields are shown in protected mode.

3. **PF3 or PF5:** PF3 returns to the caller (COTRN00C or the main menu); PF5 also navigates to COTRN00C.

4. **PF4 (Clear):** Clears the displayed data.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cotrn02c"></a>
## COTRN02C — Transaction Add

**Transaction ID:** `CT02`  
**BMS Map:** `COTRN02` / Map `COTRN2A`

### Purpose

COTRN02C allows authorized users to manually add a new transaction record to the transaction file. The user can identify the account by either account ID or card number. The program validates both the account and the transaction date fields before writing.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Account ID or card number, transaction type, category, source, description, amount, merchant details, original transaction date, processing date, confirmation field (Y/N) |
| VSAM `CXACAIX` | Cross-reference alternate index (lookup by account ID) |
| VSAM `CCXREF` | Card cross-reference (lookup by card number) |
| VSAM `ACCTDAT` | Account master (to verify account exists) |
| VSAM `TRANSACT` | Transaction file (browsed backward from end to determine next ID; then written) |
| Program `CSUTLDTC` | Date validation utility called for original-date and processing-date fields |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Populated form for review; confirmation prompt; success or error messages |
| VSAM `TRANSACT` | New transaction record written |

### Processing Flow

1. **Account/Card lookup:**
   - If the user enters an Account ID, `CXACAIX` is queried (alternate index on the card cross-reference file, keyed by account ID) to retrieve the card number.
   - If the user enters a Card Number instead, `CCXREF` is read directly to retrieve the account ID and customer ID.
   - If neither is found, an error is displayed.

2. **Date validation:**
   - The original transaction date (entered by the user in MM/DD/YYYY format) is passed to `CSUTLDTC` for calendar validation.
   - The processing date is similarly validated.
   - If `CSUTLDTC` returns severity code 2513 ("insufficient data"), the program tolerates this and continues — this edge case occurs with certain date format strings and is considered non-fatal.

3. **Generate Transaction ID:**
   - The program browses `TRANSACT` backward from the end (using HIGH-VALUES start key) to find the highest existing transaction ID.
   - The new ID is the prior highest ID incremented by 1.

4. **Confirmation:**
   - A summary of the transaction is displayed.
   - The user must type `Y` in the confirmation field and press Enter (or PF5) to commit.
   - Typing `N` or pressing PF12 cancels.

5. **Write:** A complete transaction record is assembled and written to `TRANSACT` via CICS WRITE.

6. **PF5 (Copy last transaction):** Copies the field values from the most recently written transaction into the input form as a starting point.

7. **PF3 (Return):** Returns to the calling program.

### Date / Parameter Dependencies

Both the original-date and processing-date fields are validated using the `CSUTLDTC` date utility (which internally uses the IBM Language Environment `CEEDAYS` service). Current timestamp for the processing date defaults to the CICS system clock.

---

<div style="page-break-before: always"></div>

---

<a name="corpt00c"></a>
## CORPT00C — Report Submission

**Transaction ID:** `CR00`  
**BMS Map:** `CORPT00` / Map `CORPT0A`

### Purpose

CORPT00C provides an online screen that lets authorized users submit a batch transaction report job. The user optionally specifies a custom date range; the program validates those dates and then writes a JCL job stream to a CICS Transient Data Queue (TDQ) called `JOBS`, which causes the mainframe JES (Job Entry Subsystem) to pick it up and run the batch program `CBTRN03C`.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Optional start date and end date (in CCYYMMDD format) |
| COMMAREA | User context |
| Program `CSUTLDTC` | Date validation for the entered start/end dates |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Confirmation that the job was submitted, or validation error messages |
| TDQ `JOBS` | JCL job stream written record-by-record; JES picks this up and executes it as a batch job |

### Processing Flow

1. **Display report request form:** User optionally enters a start date and end date.

2. **Date validation (if dates entered):**
   - Each date is passed to `CSUTLDTC` for calendar validation.
   - If `CSUTLDTC` returns severity 2513 (a known edge case with the format string), the program treats this as non-fatal and proceeds.
   - If a date is genuinely invalid (e.g., month 13), an error is shown.

3. **JCL submission:**
   - If dates are valid (or not entered), the program writes a series of 80-character JCL records one at a time to the `JOBS` TDQ using CICS WRITEQ TD.
   - The JCL invokes the `TRANREPT` procedure (defined in `app/proc/TRANREPT.prc`) which in turn runs `CBTRN03C`.
   - The date range (if specified) is passed to `CBTRN03C` via JCL PARM.

4. **PF3 (Return):** Returns to the main menu (`COMEN01C`).

### How Online-to-Batch Submission Works

This is an example of online-to-batch integration: rather than calling a batch program directly (which is not possible under CICS), the online program writes JCL to a special queue. The CICS internal reader (connected to the `JOBS` TDQ) automatically submits that JCL to JES for batch execution. This is standard IBM mainframe practice for triggering batch jobs from CICS screens.

### Date / Parameter Dependencies

Start and end dates validated via `CSUTLDTC`. If no dates are supplied, `CBTRN03C` reports on all transactions.

---

<div style="page-break-before: always"></div>

---

<a name="cousr00c"></a>
## COUSR00C — User List

**Transaction ID:** `CU00`  
**BMS Map:** `COUSR00` / Map `COUSR0A`

### Purpose

COUSR00C displays a paginated list of all user accounts registered in the security file. It is an admin-only function accessible from the Admin Menu. From this list, administrators can navigate to update or delete individual users.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | Selection code `U` (update) or `D` (delete) against a listed user row |
| VSAM `USRSEC` | User security file (browsed for listing) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Up to 10 users per page: user ID, first name, last name, user type |
| Transfer to `COUSR02C` | On `U` selection |
| Transfer to `COUSR03C` | On `D` selection |

### Processing Flow

1. **Initial display:** Browses `USRSEC` from the beginning and shows the first 10 users.

2. **PF8 / PF7:** Page forward and backward through the user list.

3. **Selection:** User types `U` or `D` next to a user row and presses Enter. The selected user ID is placed in the COMMAREA before transferring to the appropriate maintenance program.

4. **PF3 (Return):** Returns to the Admin Menu (`COADM01C`).

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cousr01c"></a>
## COUSR01C — User Add

**Transaction ID:** `CU01`  
**BMS Map:** `COUSR01` / Map `COUSR1A`

### Purpose

COUSR01C allows administrators to create a new user account in the security file. All fields are required.

### Inputs

| Source | Description |
|--------|-------------|
| Screen | User ID (8 chars), first name (20 chars), last name (20 chars), password (8 chars), user type (`A` = admin, `U` = user) |
| VSAM `USRSEC` | Checked for duplicate user ID before writing |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | Success confirmation or validation error messages |
| VSAM `USRSEC` | New 80-byte user record written |

### Processing Flow

1. **Validation:**
   - All five fields must be non-blank.
   - User ID must not already exist in `USRSEC` (duplicate check).
   - User type must be `A` or `U`.

2. **Write:** A new 80-byte record is assembled (user ID, first name, last name, password, type, filler) and written to `USRSEC` via CICS WRITE.

3. **Success / error feedback:** A confirmation message is shown on success; errors are highlighted in red.

4. **PF4 (Clear):** Clears all input fields.

5. **PF3 (Return):** Returns to the Admin Menu.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cousr02c"></a>
## COUSR02C — User Update

**Transaction ID:** `CU02`  
**BMS Map:** `COUSR02` / Map `COUSR2A`

### Purpose

COUSR02C allows administrators to update an existing user's first name, last name, password, and user type. The user ID itself cannot be changed.

### Inputs

| Source | Description |
|--------|-------------|
| COMMAREA | User ID (passed from COUSR00C) |
| Screen | Editable fields: first name, last name, password, user type |
| VSAM `USRSEC` | Current user record (read for display; read-with-UPDATE-lock for write) |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | User data with editable fields; success or error messages |
| VSAM `USRSEC` | User record rewritten with updated fields |

### Processing Flow

1. **Load current data:** User ID from COMMAREA retrieves the current record from `USRSEC`. Current values are displayed.

2. **User edits fields** and presses PF3 or PF5 to save.

3. **Validation:**
   - First name and last name must not be blank.
   - Password must not be blank.
   - User type must be `A` or `U`.

4. **Change detection:** Old and new values are compared. If nothing changed, a "no changes" message is shown.

5. **Write:** The record is re-read with UPDATE lock, then rewritten with the new values via CICS REWRITE.

6. **PF4 (Clear):** Restores original values.

7. **PF12 (Cancel):** Returns without saving.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cousr03c"></a>
## COUSR03C — User Delete

**Transaction ID:** `CU03`  
**BMS Map:** `COUSR03` / Map `COUSR3A`

### Purpose

COUSR03C allows administrators to delete a user account from the security file. A confirmation step is required before the deletion is executed.

### Inputs

| Source | Description |
|--------|-------------|
| COMMAREA | User ID (passed from COUSR00C) |
| Screen | Confirmation via PF5 key |
| VSAM `USRSEC` | User record to be deleted |

### Outputs

| Destination | Description |
|-------------|-------------|
| Screen | User details for confirmation; success or error message |
| VSAM `USRSEC` | User record deleted |

### Processing Flow

1. **Display:** The user's details are shown in fully protected mode. A message prompts the administrator to press PF5 to confirm deletion or PF3/PF12 to cancel.

2. **PF5 (Confirm Delete):**
   - The record is re-read with an UPDATE lock.
   - CICS DELETE is issued to permanently remove the record.
   - A success message is displayed.

3. **PF3 / PF12 (Cancel):** Returns to the User List without deleting.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

## Batch Programs

---

<a name="cbact01c"></a>
## CBACT01C — Account Master File Report

**Job Step:** Standalone batch utility

### Purpose

CBACT01C is a simple sequential read-and-print program that reads every record from the account master VSAM file and writes formatted account data to multiple output files. It is primarily used for reporting and data extract.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `ACCTFILE` | Account master file; all records are read sequentially |
| JCL DD `PARM` | None required |

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential file `ACCTOUT` | One output line per account record |
| Additional output files | As defined by the JCL DD statements |

### Processing Flow

1. **Open files:** Opens `ACCTFILE` for input and all output files.

2. **Read loop:**
   - Reads one account record at a time from `ACCTFILE` using sequential READ NEXT.
   - For each record, calls `COBDATFT` (the date format assembler module) to convert account dates from internal YYYYMMDD format to display YYYY-MM-DD format.
   - Writes the formatted record to the output file(s).
   - Increments a record count.

3. **End of file:** When all records have been processed, closes all files and writes a summary count to the system log (SYSOUT).

### Date / Parameter Dependencies

- Calls `COBDATFT` with conversion type `'1'` (YYYYMMDD → YYYY-MM-DD) for display formatting.
- No PARM date input.

---

<div style="page-break-before: always"></div>

---

<a name="cbact02c"></a>
## CBACT02C — Card Master File Report

**Job Step:** Standalone batch utility

### Purpose

CBACT02C reads all records from the card master VSAM file and prints them to a report file. It is the card-file equivalent of CBACT01C.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `CARDFILE` | Card master file; all records read sequentially |

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential output file | One line per card record showing card number, account ID, CVV code, embossed name, expiration date, and active status |

### Processing Flow

Identical pattern to CBACT01C:
1. Open files.
2. Read-loop: READ NEXT → format → write.
3. End of file: close files, print record count.

No date conversion calls are made (card dates are stored in display format).

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cbact03c"></a>
## CBACT03C — Card Cross-Reference Report

**Job Step:** Standalone batch utility

### Purpose

CBACT03C reads and prints all records from the card cross-reference VSAM file. The cross-reference file maps card numbers to both account IDs and customer IDs.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `XREFFILE` | Card cross-reference file; all records read sequentially |

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential output file | One line per cross-reference record: card number, customer ID, account ID |

### Processing Flow

Standard open → read-loop → close pattern. No date processing.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cbact04c"></a>
## CBACT04C — Interest Calculation (INTCALC)

**Job Step:** `INTCALC` in the nightly batch cycle

### Purpose

CBACT04C calculates the monthly interest charge for each account. For every account, it looks up the applicable interest rate based on the account's discount group and the transaction category balances for that period, then computes the interest amount and posts it back to the account's balance.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `TCATBAL-FILE` | Transaction category balance file — one record per account/type/category combination, holding the outstanding balance for each spending category |
| VSAM `ACCOUNT-FILE` | Account master — read randomly by account ID to retrieve group ID and update balance |
| VSAM `XREF-FILE` | Cross-reference file — read to map transaction category records back to account IDs |
| VSAM `DIS-GROUP-FILE` | Disclosure group file — holds interest rates keyed by group ID + transaction type + transaction category |
| JCL `PARM` | Optional: an 8-character date string (`CCYYMMDD`) used as the processing date. If not supplied, the current date is used. |

### Outputs

| Destination | Description |
|-------------|-------------|
| VSAM `ACCOUNT-FILE` | Account record rewritten with updated balance after interest is added |
| SYSOUT | Summary counts and exception messages |

### Processing Flow

1. **Receive PARM date:** If the JCL PARM is provided, it is used as the processing date (this allows reprocessing interest for a prior period). The PARM is received through the LINKAGE SECTION as `PARM-DATE`.

2. **Sequential read of TCATBAL-FILE:**
   - Each record contains: account ID, transaction type code, category code, and the category's outstanding balance.
   - For each record, the program reads `XREF-FILE` randomly to confirm the account exists and retrieve its group ID (needed to look up the interest rate).

3. **Interest rate lookup:**
   - Using the account's group ID + transaction type + category code as a composite key, `DIS-GROUP-FILE` is read randomly to obtain `DIS-INT-RATE`.

4. **Interest calculation:**
   ```
   Interest = (Category Balance × Annual Interest Rate) ÷ 1200
   ```
   The divisor 1200 converts an annual rate (stored as a percentage, e.g., 18.00 = 18%) to a monthly rate.

5. **Account balance update:**
   - `ACCOUNT-FILE` is read randomly for that account ID.
   - The computed interest amount is added to the account's current balance.
   - The account record is rewritten.

6. **Fee calculation (STUB):**
   - Paragraph `1400-COMPUTE-FEES` exists in the code but contains no active logic — it is a placeholder for future fee computation.

### Date / Parameter Dependencies

- Accepts an optional `PARM-DATE` in CCYYMMDD format via the JCL PARM field.
- If the PARM is blank or absent, the program uses the current system date.

---

<div style="page-break-before: always"></div>

---

<a name="cbcus01c"></a>
## CBCUS01C — Customer Master File Report

**Job Step:** Standalone batch utility

### Purpose

CBCUS01C reads and prints all customer records from the customer master VSAM file. It is the customer-file equivalent of CBACT01C.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `CUSTFILE` | Customer master file; all records read sequentially |

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential output file | One line per customer: ID, name, address, phone, SSN, date of birth, FICO score |

### Processing Flow

Standard open → read-loop → close. No date conversion calls.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cbexport"></a>
## CBEXPORT — Full Data Export

**Job Step:** Data migration / extract utility

### Purpose

CBEXPORT reads all five core data domains — customers, accounts, card cross-references, transactions, and card records — and writes them to a single, unified export file. Each output record is tagged with a record type identifier (`C`=customer, `A`=account, `X`=cross-reference, `T`=transaction, `D`=card). This file is used for branch data migration or system-to-system data exchange.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `CUSTFILE` | Customer master |
| VSAM `ACCTFILE` | Account master |
| VSAM `XREFFILE` | Card cross-reference |
| VSAM `TRANFILE` | Transaction file |
| VSAM `CARDFILE` | Card master |

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential `EXPFILE` | Mixed-type export file; 500-byte records, one per source entity |

### Processing Flow

1. **Open all files.**

2. **Sequential export of each domain (in order):**
   - For each source file, read sequentially.
   - Populate the `EXPORT-RECORD` structure (defined in copybook `CVEXPORT`) using the appropriate REDEFINES overlay for that record type.
   - Set the record type indicator (`C`/`A`/`X`/`T`/`D`).
   - Write to `EXPFILE`.

3. **Hardcoded metadata:** Each export record includes `BRANCH-ID='0001'` and `REGION-CODE='NORTH'` — these are hardcoded and represent the source branch identity.

4. **Close all files and write totals.**

### Date / Parameter Dependencies

No date processing. No PARM input.

---

<div style="page-break-before: always"></div>

---

<a name="cbimport"></a>
## CBIMPORT — Branch Data Import

**Job Step:** Data migration / load utility

### Purpose

CBIMPORT is the companion to CBEXPORT. It reads a mixed-type export file and routes each record to the appropriate VSAM output file based on the record type code. It is used to load migrated branch data into the application's VSAM files.

### Inputs

| Source | Description |
|--------|-------------|
| Sequential `IMPFILE` | Mixed-type import file produced by CBEXPORT (or equivalent) |

### Outputs

| Destination | Description |
|-------------|-------------|
| VSAM `CUSTFILE` | Customer records (type `C`) |
| VSAM `ACCTFILE` | Account records (type `A`) |
| VSAM `XREFFILE` | Cross-reference records (type `X`) |
| VSAM `TRANFILE` | Transaction records (type `T`) |
| VSAM `CARDFILE` | Card records (type `D`) |
| Sequential `ERROUT` | Records with unrecognized type codes |

### Processing Flow

1. **Open all files.**

2. **Read-loop:** For each import record:
   - Inspect the record type code.
   - Route to the matching output VSAM file via WRITE.
   - If the type code is unrecognized, write to `ERROUT` with a diagnostic message.

3. **Validation (STUB):** Paragraph `3000-VALIDATE-IMPORT` exists but contains no active logic — all records are accepted as-is. Validation against business rules is a future enhancement.

4. **Close all files; write record counts by type.**

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cbstm03a"></a>
## CBSTM03A — Statement Generator (CREASTMT)

**Job Step:** `CREASTMT` — final step in the nightly batch cycle before file reopen

### Purpose

CBSTM03A is the monthly statement generation program. It reads transaction data and account/customer information to produce two parallel output files: a traditional 80-character print-format statement and an HTML-format statement. It is the largest and most complex batch program in the application.

### Inputs

| Source | Description |
|--------|-------------|
| VSAM `TRNXFILE` | Transaction file — browsed sequentially grouped by card number |
| VSAM `XREFFILE` | Cross-reference file — read randomly to map card number → customer/account ID |
| VSAM `CUSTFILE` | Customer master — read randomly to get customer name and address |
| VSAM `ACCTFILE` | Account master — read randomly to get balance, credit limit, and group |

All file I/O is delegated to the subroutine program `CBSTM03B`.

### Outputs

| Destination | Description |
|-------------|-------------|
| Sequential `STMTFILE` | 80-character print-format statement file, one statement section per account |
| Sequential `HTMLFILE` | 100-character HTML-format statement file with HTML tags |

### Processing Flow

1. **Initialization:** Opens all files by calling `CBSTM03B` with operation code `'O'` (Open) for each DD name.

2. **Transaction grouping:** Transactions are read and grouped by card number. The program maintains an in-memory 2-dimensional array capable of holding up to 51 cards × 10 transactions per card simultaneously.

3. **Statement assembly:** For each account group:
   - Customer name and address are retrieved.
   - Account balances and credit limit are retrieved.
   - Each transaction is formatted as a statement line.
   - A summary section (previous balance, payments, new charges, interest, new balance) is assembled.

4. **Dual output:** Each section of the statement is written to both `STMTFILE` (print format, 80 chars) and `HTMLFILE` (with HTML tags, 100 chars).

5. **Subroutine calls:** All file READ, WRITE, OPEN, CLOSE operations are issued via `CALL 'CBSTM03B' USING LK-M03B-AREA` — the main program never directly issues file I/O commands.

### Notable Implementation Characteristics

- **ALTER / GO TO:** The program intentionally uses the obsolete COBOL `ALTER` statement (which dynamically changes the target of a `GO TO` at runtime) for its internal control flow. This is a deliberate use of legacy coding style to exercise modernization tooling.
- **COMP and COMP-3:** Numeric fields use both binary (COMP) and packed-decimal (COMP-3) representations.
- **Pointer arithmetic / TIOT:** The program manipulates a mainframe control block (Task I/O Table) via pointer arithmetic. This is advanced, low-level mainframe code used to iterate over DD name entries at runtime.
- **No structured `PERFORM`:** Control flow relies heavily on `GO TO` rather than structured PERFORM loops.

### Date / Parameter Dependencies

No PARM date. Statements are generated for the current processing period as determined by the data in the files.

---

<div style="page-break-before: always"></div>

---

<a name="cbstm03b"></a>
## CBSTM03B — Statement File I/O Subroutine

**Called by:** CBSTM03A only

### Purpose

CBSTM03B is a file I/O service subroutine exclusively used by CBSTM03A. It provides a single, uniform interface for all file operations (open, close, read, write, key-based read) across all five files used during statement generation. By centralizing file I/O in a subroutine, CBSTM03A is freed from VSAM-specific details.

### Inputs / Outputs

CBSTM03B communicates entirely through a shared LINKAGE SECTION parameter block (`LK-M03B-AREA`):

| Parameter Field | Description |
|----------------|-------------|
| DD Name (8 chars) | Identifies which file to operate on: `TRNXFILE`, `XREFFILE`, `CUSTFILE`, `ACCTFILE` |
| Operation code (1 char) | `O`=Open, `C`=Close, `R`=Read next sequential, `K`=Read by key, `W`=Write, `Z`=Re-initialize |
| Return code (2 chars) | `00`=success, `10`=end-of-file, other = error |
| Key (16 chars) | Record key for keyed reads |
| Key length (4 binary digits) | Byte length of the key |
| Data area (1000 chars) | Input data for writes; populated with retrieved data on reads |

### Processing Flow

1. **Receive call** from CBSTM03A with a populated `LK-M03B-AREA`.
2. **Route by DD name** to the appropriate internal section for that file.
3. **Execute the requested operation** (`OPEN`, `CLOSE`, `READ`, `READ KEY`, `WRITE`).
4. **Set return code** and return control to CBSTM03A.

### Notable Detail

The `Z` operation (re-initialize) clears the data area — this is used between card groups in CBSTM03A to reset the working buffer.

### Date / Parameter Dependencies

None.

---

<div style="page-break-before: always"></div>

---

<a name="cbtrn01c"></a>
## CBTRN01C — Transaction Validation (Read-Only)

**Job Step:** Standalone validation / audit utility

### Purpose

CBTRN01C reads the daily transaction input file, validates each transaction against the account and cross-reference masters, and produces an exception report. It makes **no updates** to any file — it is a pure read-and-validate program used for pre-posting audits or post-mortem analysis.

### Inputs

| Source | Description |
|--------|-------------|
| Sequential `DALYTRAN` | Daily transaction input file (one transaction record per line, 350 bytes, layout from `CVTRA06Y`) |
| VSAM `XREF-FILE` | Cross-reference file — read randomly by card number to get account ID |
| VSAM `ACCOUNT-FILE` | Account master — read randomly by account ID to validate account exists |
| VSAM `CARD-FILE` | Card master — read randomly by card number |
| VSAM `CUSTOMER-FILE` | Customer master — read randomly by customer ID |
| Sequential `TRANSACT` | Transaction file — read to check for duplicate transaction IDs |

### Outputs

| Destination | Description |
|-------------|-------------|
| SYSOUT / report | Exception report listing rejected or suspicious transactions with reason codes |
| Return code | Non-zero if any exceptions found |

### Validation Checks Performed

| Validation | Description |
|------------|-------------|
| Card number exists | Must be found in `XREF-FILE` |
| Account exists | Account linked to the card must be in `ACCOUNT-FILE` |
| Account active | Account status must indicate active |
| Amount range | Transaction amount must be within configured limits |
| Duplicate ID | Transaction ID must not already exist in `TRANSACT` |

### Processing Flow

1. **Open all files.**
2. **Read-loop:** For each daily transaction:
   - Look up card in cross-reference.
   - Validate account, customer, and card status.
   - Check for duplicates.
   - Count accepted vs. rejected.
3. **Write exception report.**
4. **Close all files. Set RETURN-CODE** to 4 if any exceptions were found.

### Date / Parameter Dependencies

No date or PARM processing.

---

<div style="page-break-before: always"></div>

---

<a name="cbtrn02c"></a>
## CBTRN02C — Transaction Posting (POSTTRAN)

**Job Step:** `POSTTRAN` — core step 3 in the nightly batch cycle

### Purpose

CBTRN02C is the transaction posting engine. It reads each pending daily transaction, validates it thoroughly, updates the relevant account balance and the transaction category balance, and either commits the transaction to the master transaction file or routes it to a reject file with a reason code. This is the most financially consequential batch program.

### Inputs

| Source | Description |
|--------|-------------|
| Sequential `DALYTRAN` | Daily transaction input file (350-byte records, `CVTRA06Y` layout) |
| VSAM `XREF-FILE` | Cross-reference — maps card number → account ID |
| VSAM `ACCOUNT-FILE` | Account master — validates account, holds balance and credit limit |
| VSAM `TCATBAL-FILE` | Transaction category balance file — updated with new transaction amounts |

### Outputs

| Destination | Description |
|-------------|-------------|
| VSAM `TRANSACT` | Accepted transactions written here (persistent transaction master) |
| Sequential `DALYREJS` | Rejected transactions — 80-char record with the original data plus a rejection reason code appended as a trailer |
| VSAM `ACCOUNT-FILE` | Account balance updated (current balance + transaction amount) |
| VSAM `TCATBAL-FILE` | Category balance updated for the account + type + category combination |
| RETURN-CODE | Set to `4` if any transactions were rejected |

### Rejection Reason Codes

| Code | Meaning |
|------|---------|
| 100 | Card number not found in cross-reference file |
| 101 | Account not found in account master |
| 102 | Transaction amount would exceed credit limit |
| 103 | Transaction category balance record not found |

### Processing Flow

1. **Open all files.**

2. **Read-loop:** For each daily transaction record:

   a. **Cross-reference check (code 100):** The card number is looked up in `XREF-FILE`. If not found, the transaction is rejected with code 100 and written to `DALYREJS`.

   b. **Account existence check (code 101):** The account ID retrieved from the cross-reference is used to read `ACCOUNT-FILE`. If not found, rejected with code 101.

   c. **Credit limit check (code 102):** `New Balance = Current Balance + Transaction Amount`. If the new balance would exceed the account's credit limit, rejected with code 102. (Note: for payments, the amount is negative, so this check effectively ensures the balance does not drop below the credit limit in reverse — see Cobil00C for more detail.)

   d. **Category balance update (code 103):** A composite key of account ID + transaction type + transaction category is used to read `TCATBAL-FILE`. If the record is not found, rejected with code 103. If found, the category balance is updated: `New Category Balance = Old Balance + Transaction Amount`, and the record is rewritten.

   e. **Account balance update:** If all checks pass, the account's current balance is updated and the account record is rewritten.

   f. **Transaction written to master:** The accepted transaction (in `CVTRA05Y` layout) is written to `TRANSACT`.

3. **End of file:** Close all files. Set `RETURN-CODE = 4` if the rejection count is greater than zero.

### Date / Parameter Dependencies

- Calls `CSUTLDTC` (indirectly, via working storage) to validate transaction timestamps.
- No direct PARM date input — uses dates embedded within each transaction record.

---

<div style="page-break-before: always"></div>

---

<a name="cbtrn03c"></a>
## CBTRN03C — Transaction Detail Report

**Job Step:** Invoked by `TRANREPT` procedure, submitted from `CORPT00C`

### Purpose

CBTRN03C produces a formatted 133-character wide transaction detail report. It reads transactions sequentially, enriches each record with transaction type description and category description from reference files, optionally filters by a date range, and prints a paginated report to SYSOUT.

### Inputs

| Source | Description |
|--------|-------------|
| Sequential `TRANFILE` | Transaction master file (read sequentially) |
| VSAM `XREF-FILE` | Cross-reference — read randomly by card number to get account/customer IDs |
| VSAM `TRANTYPE-FILE` | Transaction type reference (2-char code → 50-char description) |
| VSAM `TRANCATG-FILE` | Transaction category reference (type+category codes → description) |
| JCL `DATEPARM` DD | Optional: start date and end date in `CCYYMMDD` format on a single 16-byte input record |

### Outputs

| Destination | Description |
|-------------|-------------|
| SYSOUT `RPTFILE` | 133-character print file, 20 lines per page with page headers; includes per-account subtotals and a final grand total |

### Report Layout

Each detail line shows: transaction date, transaction ID, type code + description, category code + description, source, amount, merchant name, and card number.

### Processing Flow

1. **Read DATEPARM (optional):**
   - If the `DATEPARM` DD is present and contains a valid 16-byte record, the first 8 characters are the start date and the next 8 characters are the end date (both CCYYMMDD).
   - If not provided, all transactions are reported.

2. **Sequential read of TRANFILE:**
   - For each transaction, check whether the transaction date falls within the start/end date range (if one was specified).
   - If the transaction is in range:
     - Read `TRANTYPE-FILE` randomly by type code to get the type description.
     - Read `TRANCATG-FILE` randomly by type+category key to get the category description.
     - If either lookup fails (`INVALID KEY`), the program **abends** — these reference files are expected to always contain entries for all valid codes.
     - Read `XREF-FILE` randomly by card number to get the account ID.
     - Format a 133-char report line and write it.
   - Maintain running totals per account and a grand total.

3. **Page breaks:** A new page header is printed every 20 detail lines.

4. **Account subtotals:** When the account ID changes (detected by comparing with the prior record's account ID), a subtotal line is printed.

5. **Grand total:** Written at the end of the report.

### Date / Parameter Dependencies

- Date range filter read from the `DATEPARM` DD statement in CCYYMMDD format.
- If `DATEPARM` is not present in the JCL, all transactions are included.

---

<div style="page-break-before: always"></div>

---

<a name="cobswait"></a>
## COBSWAIT — Timed Wait Utility

**Job Step:** Utility — called standalone or as a step in a JCL sequence

### Purpose

COBSWAIT is a batch wrapper for the `MVSWAIT` assembler module. It accepts a delay interval from SYSIN, converts it to binary format, and calls the assembler wait routine to pause execution for the specified duration. It is used in JCL sequences where a time delay between steps is needed (e.g., waiting for a CICS region to quiesce before submitting the next job).

### Inputs

| Source | Description |
|--------|-------------|
| SYSIN | A single 8-character record containing the delay duration (seconds or timer units) |

### Outputs

None. The program simply waits for the specified interval and then returns.

### Processing Flow

1. **ACCEPT** the 8-character delay value from SYSIN into a display field.
2. **MOVE** the value to a binary (COMP) field (`MVSWAIT-TIME`) in the required format for `MVSWAIT`.
3. **CALL 'MVSWAIT'** passing the time field.
4. Return normally.

### Date / Parameter Dependencies

No date dependencies. The delay value is a pure numeric timer interval.

---

<div style="page-break-before: always"></div>

---

## Standalone Utility Programs

---

<a name="csutldtc"></a>
## CSUTLDTC — Date Validation Utility

**Calling interface:** `CALL 'CSUTLDTC' USING date-field, format-field, result-field`

### Purpose

CSUTLDTC is a shared, callable date validation subroutine. It accepts a date string and a format string, validates whether the date represents a valid calendar date, and returns a result message and severity code. It is called by multiple online programs (COACTUPC, COTRN02C, CORPT00C) and inherits the reliability of IBM's Language Environment (LE) date services.

### Inputs (LINKAGE SECTION)

| Parameter | Picture | Description |
|-----------|---------|-------------|
| `LS-DATE` | PIC X(10) | The date string to validate (e.g., `2024-06-15`) |
| `LS-DATE-FORMAT` | PIC X(10) | The format descriptor (e.g., `YYYY-MM-DD`) telling the validator how to interpret the date |
| `LS-RESULT` | PIC X(80) | Output field for the result message (passed by reference) |

### Outputs

| Field | Description |
|-------|-------------|
| `LS-RESULT` | `'Date is valid'` on success; a descriptive error message on failure (e.g., "Invalid month", "Invalid day for month") |
| `RETURN-CODE` | Numeric severity: `0` = valid; positive value = error. Code `2513` = "FC-INSUFFICIENT-DATA" — a special case where the LE service considers the format string to have insufficient data; callers COTRN02C and CORPT00C tolerate this severity and do not treat it as an error. |

### Processing Flow

1. **Receive** the date string and format string from the calling program.
2. **Call** the IBM Language Environment service `CEEDAYS` (converts a date to a Lilian day number — used as the validation mechanism). If `CEEDAYS` succeeds, the date is valid.
3. **Evaluate the feedback code** from `CEEDAYS`:
   - `SEVERITY = 0` → date is valid; set `LS-RESULT = 'Date is valid'`.
   - `SEVERITY = 2513` → insufficient data condition; set severity in RETURN-CODE but callers decide whether to treat as an error.
   - Other non-zero severity → build a descriptive error message and set RETURN-CODE.
4. **Return** to the calling program.

### Why Use CEEDAYS?

`CEEDAYS` is a highly reliable IBM LE service that understands the full Gregorian calendar, including leap years, month lengths, and valid ranges. Using it means CSUTLDTC does not need to reimplement calendar logic — it delegates all calendar math to the operating environment.

### Date / Parameter Dependencies

Not applicable — this program *is* the date validation service.

---

<div style="page-break-before: always"></div>

---

## Assembler Modules

---

<a name="cobdatft"></a>
## COBDATFT — Date Format Converter

**Calling interface:** `CALL 'COBDATFT' USING CODATECN-REC`  
**Source:** `app/asm/COBDATFT.asm`

### Purpose

COBDATFT is an IBM System/370 assembler module that converts dates between two formats:
- **Type 1:** Converts `YYYYMMDD` (8-digit compact) → `YYYY-MM-DD` (10-character display with dashes)
- **Type 2:** Converts `YYYY-MM-DD` (10-character display) → `YYYYMMDD` (8-digit compact)

### Calling Interface

The caller passes a single record `CODATECN-REC` (defined in copybook `CODATECN`):

| Field | Content |
|-------|---------|
| Type indicator | `'1'` for compact→display; `'2'` for display→compact |
| Input date area | The date in the source format |
| Output date area | Populated by COBDATFT with the converted result (overlays the input via REDEFINES) |

### Usage in the Application

CBACT01C calls COBDATFT (type 1) to convert stored YYYYMMDD account dates to YYYY-MM-DD format for report output.

---

<a name="mvswait"></a>
## MVSWAIT — Interval Timer Wait

**Calling interface:** `CALL 'MVSWAIT' USING MVSWAIT-TIME`  
**Source:** `app/asm/MVSWAIT.asm`

### Purpose

MVSWAIT is an IBM System/370 assembler module that suspends execution of the calling program for a specified time interval. It accepts a binary time value and invokes the `ASMWAIT` macro, which issues a mainframe supervisor call (SVC) to sleep the task for the given duration.

### Calling Interface

| Parameter | Type | Description |
|-----------|------|-------------|
| `MVSWAIT-TIME` | PIC 9(8) COMP (binary) | Duration to wait |

### Usage in the Application

`COBSWAIT` (the batch wait utility) calls MVSWAIT after accepting the delay value from SYSIN. This is used in JCL job streams to introduce controlled pauses between steps.

---

<div style="page-break-before: always"></div>

---

## Appendix: Batch Cycle Processing Order

The nightly batch cycle must run in the following sequence to ensure data consistency:

```
1. CLOSEFIL        — Close all CICS files so batch can have exclusive access
2. (Load masters)  — Reload ACCTFILE, CARDFILE, CUSTFILE, XREFFILE if needed
3. CBTRN02C        — POSTTRAN: post daily transactions to accounts
4. CBACT04C        — INTCALC:  calculate and post monthly interest
5. COMBTRAN        — Combine daily and system-generated transactions
6. CBSTM03A        — CREASTMT: generate account statements
7. OPENFIL         — Reopen CICS files for online access
```

Optional / standalone jobs that can run outside this cycle:
- `CBTRN01C` — Pre-posting validation audit (read-only, safe to run any time)
- `CBTRN03C` — Transaction detail report (submitted online via `CORPT00C` / `CORPT00C`)
- `CBACT01C`, `CBACT02C`, `CBACT03C`, `CBCUS01C` — File content reports (read-only)
- `CBEXPORT` / `CBIMPORT` — Data migration tools (run outside normal cycle)

---

## Appendix: VSAM File Summary

| Logical Name | Description | Primary Key | Alternate Index |
|-------------|-------------|-------------|-----------------|
| `ACCTDAT` / `ACCTFILE` | Account master | Account ID (11 digits) | None |
| `CARDDAT` / `CARDFILE` | Card master | Card Number (16 chars) | None |
| `CUSTDAT` / `CUSTFILE` | Customer master | Customer ID (9 digits) | None |
| `TRANSACT` / `TRANFILE` | Transaction master | Transaction ID (16 chars) | None |
| `XREFFILE` / `CCXREF` | Card↔Account↔Customer cross-reference | Card Number (16 chars) | `CXACAIX` (by Account ID), `CARDAIX` (by Account ID on card file) |
| `USRSEC` | User security master | User ID (8 chars) | None |
| `TCATBAL-FILE` | Transaction category balances | Account ID + Type + Category | None |
| `DIS-GROUP-FILE` | Interest rate disclosure groups | Group ID + Type + Category | None |
| `TRANTYPE-FILE` | Transaction type descriptions | Type Code (2 chars) | None |
| `TRANCATG-FILE` | Transaction category descriptions | Type Code + Category Code | None |

---

## Appendix: Key Data Structures

### Account Record (`CVACT01Y` — 300 bytes)
Account ID, active status, current balance (signed decimal), credit limit, cash credit limit, current-cycle credit, current-cycle debit, open date (YYYY-MM-DD), expiration date (YYYY-MM-DD), reissue date (YYYY-MM-DD), address zip, group ID.

### Card Record (`CVACT02Y` — 150 bytes)
Card number (16 chars), account ID (11 digits), CVV code (3 digits), embossed name (50 chars), expiration date (10 chars), active status (1 char).
> Note: The field name `CARD-EXPIRAION-DATE` contains an intentional typo preserved from the original source.

### Customer Record (`CVCUS01Y` — 500 bytes)
Customer ID (9 digits), first/middle/last name, 3-line address, state code, zip, country, two phone numbers in `(aaa)bbb-cccc` format, SSN (9 digits), government-issued ID, date of birth (YYYY-MM-DD with dashes), EFT account ID, primary card holder indicator, FICO credit score (3 digits).
> Note: Two copybooks define customer records: `CVCUS01Y` uses `CUST-DOB-YYYY-MM-DD` (with dashes, 10 chars); `CUSTREC` uses `CUST-DOB-YYYYMMDD` (no dashes, 8 chars). They are used by different programs.

### Transaction Record (`CVTRA05Y` — 350 bytes)
Transaction ID (16 chars), type code (2 chars), category code (4 digits), source (10 chars), description (100 chars), amount (signed decimal), merchant ID (9 digits), merchant name (50 chars), merchant city (50 chars), zip (10 chars), card number (16 chars), original timestamp (26 chars: `YYYY-MM-DD HH:MM:SS.NNNNNN`), processing timestamp (26 chars).

### Cross-Reference Record (`CVACT03Y` — 50 bytes)
Card number (16 chars), customer ID (9 digits), account ID (11 digits).

### User Security Record (`CSUSR01Y` — 80 bytes)
User ID (8 chars), first name (20 chars), last name (20 chars), password (8 chars, plaintext), user type (1 char: `A`=admin, `U`=user).

---

*Document generated from source analysis of CardDemo v1.0 — `app/cbl/`, `app/cpy/`, `app/asm/`*
