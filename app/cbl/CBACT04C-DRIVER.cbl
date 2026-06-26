******************************************************************
      * CBACT04C-DRIVER.CBL
      * Driver program to invoke CBACT04C with JCL PARM equivalent.
      * On the mainframe, PARM-DATE is passed via JCL PARM= statement.
      * This driver replicates that behavior for GnuCOBOL execution.
      * Change PARM-DATE value to set the processing date for the run.
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID. CBACT04C-DRIVER.
       DATA DIVISION.
       WORKING-STORAGE SECTION.
       01 EXTERNAL-PARMS.
          05 PARM-LENGTH     PIC S9(04) COMP VALUE 10.
          05 PARM-DATE       PIC X(10)  VALUE '2024-06-15'.
       PROCEDURE DIVISION.
           CALL 'CBACT04C' USING EXTERNAL-PARMS
           STOP RUN.
           