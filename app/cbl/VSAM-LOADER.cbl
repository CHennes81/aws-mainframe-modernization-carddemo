      ******************************************************************
      * Program     : VSAM-LOADER.CBL                                  *
      * Function    : Load ASCII flat sequential files into GnuCOBOL   *
      *               BDB indexed files for use by CBACT04C.           *
      * Inputs      : TCATBAL-FLAT, XREF-FLAT, ACCT-FLAT,             *
      *               DISCGRP-FLAT  (LINE SEQUENTIAL)                  *
      * Outputs     : TCATBALF, XREFFILE, ACCTFILE, DISCGRP (INDEXED) *
      * FD defs     : Exact copies of CBACT04C FD definitions for      *
      *               byte-level record compatibility.                  *
      ******************************************************************
       IDENTIFICATION DIVISION.
       PROGRAM-ID.    VSAM-LOADER.
       AUTHOR.        CardDemo Migration.
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
      *--- Sequential (flat ASCII) inputs ----------------------------*
           SELECT TCATBAL-IN   ASSIGN TO TCATBAL_FLAT
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS  IS WS-TCIN-STAT.

           SELECT XREF-IN      ASSIGN TO XREF_FLAT
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS  IS WS-XRIN-STAT.

           SELECT ACCT-IN      ASSIGN TO ACCT_FLAT
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS  IS WS-ACIN-STAT.

           SELECT DISCGRP-IN   ASSIGN TO DISCGRP_FLAT
                  ORGANIZATION IS LINE SEQUENTIAL
                  FILE STATUS  IS WS-DGIN-STAT.

      *--- Indexed outputs (key structures matching CBACT04C) --------*
           SELECT TCATBAL-FILE ASSIGN TO TCATBALF
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS RANDOM
                  RECORD KEY   IS FD-TRAN-CAT-KEY
                  FILE STATUS  IS WS-TCOUT-STAT.

           SELECT XREF-FILE    ASSIGN TO XREFFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS RANDOM
                  RECORD KEY   IS FD-XREF-CARD-NUM
                  ALTERNATE RECORD KEY IS FD-XREF-ACCT-ID
                         WITH DUPLICATES
                  FILE STATUS  IS WS-XROUT-STAT.

           SELECT ACCOUNT-FILE ASSIGN TO ACCTFILE
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS RANDOM
                  RECORD KEY   IS FD-ACCT-ID
                  FILE STATUS  IS WS-ACOUT-STAT.

           SELECT DISCGRP-FILE ASSIGN TO DISCGRP
                  ORGANIZATION IS INDEXED
                  ACCESS MODE  IS RANDOM
                  RECORD KEY   IS FD-DISCGRP-KEY
                  FILE STATUS  IS WS-DGOUT-STAT.

      *
       DATA DIVISION.
       FILE SECTION.
      *--- Input FDs: flat X(n) records, matched to output lengths ---*
       FD  TCATBAL-IN
           RECORD CONTAINS 50 CHARACTERS.
       01  FD-TCATBAL-IN-REC              PIC X(50).

       FD  XREF-IN
           RECORD CONTAINS 50 CHARACTERS.
       01  FD-XREF-IN-REC                 PIC X(50).

       FD  ACCT-IN
           RECORD CONTAINS 300 CHARACTERS.
       01  FD-ACCT-IN-REC                 PIC X(300).

       FD  DISCGRP-IN
           RECORD CONTAINS 50 CHARACTERS.
       01  FD-DISCGRP-IN-REC              PIC X(50).

      *--- Output FDs: verbatim copies of CBACT04C FILE SECTION ------*
       FD  TCATBAL-FILE.
       01  FD-TRAN-CAT-BAL-RECORD.
           05 FD-TRAN-CAT-KEY.
              10 FD-TRANCAT-ACCT-ID       PIC 9(11).
              10 FD-TRANCAT-TYPE-CD       PIC X(02).
              10 FD-TRANCAT-CD            PIC 9(04).
           05 FD-FD-TRAN-CAT-DATA         PIC X(33).

       FD  XREF-FILE.
       01  FD-XREFFILE-REC.
           05 FD-XREF-CARD-NUM            PIC X(16).
           05 FD-XREF-CUST-NUM            PIC 9(09).
           05 FD-XREF-ACCT-ID             PIC 9(11).
           05 FD-XREF-FILLER              PIC X(14).

       FD  DISCGRP-FILE.
       01  FD-DISCGRP-REC.
           05 FD-DISCGRP-KEY.
              10 FD-DIS-ACCT-GROUP-ID     PIC X(10).
              10 FD-DIS-TRAN-TYPE-CD      PIC X(02).
              10 FD-DIS-TRAN-CAT-CD       PIC 9(04).
           05 FD-DISCGRP-DATA             PIC X(34).

       FD  ACCOUNT-FILE.
       01  FD-ACCTFILE-REC.
           05 FD-ACCT-ID                  PIC 9(11).
           05 FD-ACCT-DATA                PIC X(289).

      *
       WORKING-STORAGE SECTION.
       01  WS-FILE-STATUS.
           05 WS-TCIN-STAT                PIC XX VALUE SPACES.
           05 WS-XRIN-STAT                PIC XX VALUE SPACES.
           05 WS-ACIN-STAT                PIC XX VALUE SPACES.
           05 WS-DGIN-STAT                PIC XX VALUE SPACES.
           05 WS-TCOUT-STAT               PIC XX VALUE SPACES.
           05 WS-XROUT-STAT               PIC XX VALUE SPACES.
           05 WS-ACOUT-STAT               PIC XX VALUE SPACES.
           05 WS-DGOUT-STAT               PIC XX VALUE SPACES.

       01  WS-EOF-FLAGS.
           05 WS-TCATBAL-EOF              PIC X VALUE 'N'.
           05 WS-XREF-EOF                 PIC X VALUE 'N'.
           05 WS-ACCT-EOF                 PIC X VALUE 'N'.
           05 WS-DISCGRP-EOF              PIC X VALUE 'N'.

       01  WS-COUNTS.
           05 WS-TCATBAL-CNT              PIC 9(07) VALUE 0.
           05 WS-XREF-CNT                 PIC 9(07) VALUE 0.
           05 WS-ACCT-CNT                 PIC 9(07) VALUE 0.
           05 WS-DISCGRP-CNT              PIC 9(07) VALUE 0.
           05 WS-TCATBAL-DUP              PIC 9(07) VALUE 0.
           05 WS-XREF-DUP                 PIC 9(07) VALUE 0.
           05 WS-ACCT-DUP                 PIC 9(07) VALUE 0.
           05 WS-DISCGRP-DUP              PIC 9(07) VALUE 0.

      *
       PROCEDURE DIVISION.
       0000-MAIN.
           DISPLAY 'VSAM-LOADER: START OF EXECUTION'.
           PERFORM 1000-LOAD-TCATBAL.
           PERFORM 2000-LOAD-XREF.
           PERFORM 3000-LOAD-ACCT.
           PERFORM 4000-LOAD-DISCGRP.
           DISPLAY 'VSAM-LOADER: END OF EXECUTION'.
           STOP RUN.

      *---------------------------------------------------------------*
       1000-LOAD-TCATBAL.
           DISPLAY 'LOADING TCATBALF FROM TCATBAL-FLAT'.
           OPEN INPUT TCATBAL-IN.
           IF WS-TCIN-STAT NOT = '00'
               DISPLAY 'ERROR OPENING INPUT TCATBAL-FLAT: '
                       WS-TCIN-STAT
               STOP RUN
           END-IF.
           OPEN OUTPUT TCATBAL-FILE.
           IF WS-TCOUT-STAT NOT = '00'
               DISPLAY 'ERROR OPENING OUTPUT TCATBALF: '
                       WS-TCOUT-STAT
               STOP RUN
           END-IF.
           PERFORM UNTIL WS-TCATBAL-EOF = 'Y'
               READ TCATBAL-IN INTO FD-TRAN-CAT-BAL-RECORD
               EVALUATE WS-TCIN-STAT
                   WHEN '10'
                       MOVE 'Y' TO WS-TCATBAL-EOF
                   WHEN '00'
                       WRITE FD-TRAN-CAT-BAL-RECORD
                       EVALUATE WS-TCOUT-STAT
                           WHEN '00'
                               ADD 1 TO WS-TCATBAL-CNT
                           WHEN '22'
                               ADD 1 TO WS-TCATBAL-DUP
                               DISPLAY 'WARN: DUP KEY TCATBALF '
                                       FD-TRAN-CAT-KEY
                           WHEN OTHER
                               DISPLAY 'ERROR WRITING TCATBALF: '
                                       WS-TCOUT-STAT
                               STOP RUN
                       END-EVALUATE
                   WHEN OTHER
                       DISPLAY 'ERROR READING TCATBAL-FLAT: '
                               WS-TCIN-STAT
                       STOP RUN
               END-EVALUATE
           END-PERFORM.
           CLOSE TCATBAL-IN
                 TCATBAL-FILE.
           DISPLAY 'TCATBALF  LOADED: ' WS-TCATBAL-CNT
                   '  DUPLICATES SKIPPED: ' WS-TCATBAL-DUP.
           EXIT.

      *---------------------------------------------------------------*
       2000-LOAD-XREF.
           DISPLAY 'LOADING XREFFILE FROM XREF-FLAT'.
           OPEN INPUT XREF-IN.
           IF WS-XRIN-STAT NOT = '00'
               DISPLAY 'ERROR OPENING INPUT XREF-FLAT: '
                       WS-XRIN-STAT
               STOP RUN
           END-IF.
           OPEN OUTPUT XREF-FILE.
           IF WS-XROUT-STAT NOT = '00'
               DISPLAY 'ERROR OPENING OUTPUT XREFFILE: '
                       WS-XROUT-STAT
               STOP RUN
           END-IF.
           PERFORM UNTIL WS-XREF-EOF = 'Y'
               READ XREF-IN INTO FD-XREFFILE-REC
               EVALUATE WS-XRIN-STAT
                   WHEN '10'
                       MOVE 'Y' TO WS-XREF-EOF
                   WHEN '00'
                       WRITE FD-XREFFILE-REC
                       EVALUATE WS-XROUT-STAT
                           WHEN '00'
                               ADD 1 TO WS-XREF-CNT
                           WHEN '22'
                               ADD 1 TO WS-XREF-DUP
                               DISPLAY 'WARN: DUP KEY XREFFILE '
                                       FD-XREF-CARD-NUM
                           WHEN OTHER
                               DISPLAY 'ERROR WRITING XREFFILE: '
                                       WS-XROUT-STAT
                               STOP RUN
                       END-EVALUATE
                   WHEN OTHER
                       DISPLAY 'ERROR READING XREF-FLAT: '
                               WS-XRIN-STAT
                       STOP RUN
               END-EVALUATE
           END-PERFORM.
           CLOSE XREF-IN
                 XREF-FILE.
           DISPLAY 'XREFFILE  LOADED: ' WS-XREF-CNT
                   '  DUPLICATES SKIPPED: ' WS-XREF-DUP.
           EXIT.

      *---------------------------------------------------------------*
       3000-LOAD-ACCT.
           DISPLAY 'LOADING ACCTFILE FROM ACCT-FLAT'.
           OPEN INPUT ACCT-IN.
           IF WS-ACIN-STAT NOT = '00'
               DISPLAY 'ERROR OPENING INPUT ACCT-FLAT: '
                       WS-ACIN-STAT
               STOP RUN
           END-IF.
           OPEN OUTPUT ACCOUNT-FILE.
           IF WS-ACOUT-STAT NOT = '00'
               DISPLAY 'ERROR OPENING OUTPUT ACCTFILE: '
                       WS-ACOUT-STAT
               STOP RUN
           END-IF.
           PERFORM UNTIL WS-ACCT-EOF = 'Y'
               READ ACCT-IN INTO FD-ACCTFILE-REC
               EVALUATE WS-ACIN-STAT
                   WHEN '10'
                       MOVE 'Y' TO WS-ACCT-EOF
                   WHEN '00'
                       WRITE FD-ACCTFILE-REC
                       EVALUATE WS-ACOUT-STAT
                           WHEN '00'
                               ADD 1 TO WS-ACCT-CNT
                           WHEN '22'
                               ADD 1 TO WS-ACCT-DUP
                               DISPLAY 'WARN: DUP KEY ACCTFILE '
                                       FD-ACCT-ID
                           WHEN OTHER
                               DISPLAY 'ERROR WRITING ACCTFILE: '
                                       WS-ACOUT-STAT
                               STOP RUN
                       END-EVALUATE
                   WHEN OTHER
                       DISPLAY 'ERROR READING ACCT-FLAT: '
                               WS-ACIN-STAT
                       STOP RUN
               END-EVALUATE
           END-PERFORM.
           CLOSE ACCT-IN
                 ACCOUNT-FILE.
           DISPLAY 'ACCTFILE  LOADED: ' WS-ACCT-CNT
                   '  DUPLICATES SKIPPED: ' WS-ACCT-DUP.
           EXIT.

      *---------------------------------------------------------------*
       4000-LOAD-DISCGRP.
           DISPLAY 'LOADING DISCGRP FROM DISCGRP-FLAT'.
           OPEN INPUT DISCGRP-IN.
           IF WS-DGIN-STAT NOT = '00'
               DISPLAY 'ERROR OPENING INPUT DISCGRP-FLAT: '
                       WS-DGIN-STAT
               STOP RUN
           END-IF.
           OPEN OUTPUT DISCGRP-FILE.
           IF WS-DGOUT-STAT NOT = '00'
               DISPLAY 'ERROR OPENING OUTPUT DISCGRP: '
                       WS-DGOUT-STAT
               STOP RUN
           END-IF.
           PERFORM UNTIL WS-DISCGRP-EOF = 'Y'
               READ DISCGRP-IN INTO FD-DISCGRP-REC
               EVALUATE WS-DGIN-STAT
                   WHEN '10'
                       MOVE 'Y' TO WS-DISCGRP-EOF
                   WHEN '00'
                       WRITE FD-DISCGRP-REC
                       EVALUATE WS-DGOUT-STAT
                           WHEN '00'
                               ADD 1 TO WS-DISCGRP-CNT
                           WHEN '22'
                               ADD 1 TO WS-DISCGRP-DUP
                               DISPLAY 'WARN: DUP KEY DISCGRP  '
                                       FD-DISCGRP-KEY
                           WHEN OTHER
                               DISPLAY 'ERROR WRITING DISCGRP: '
                                       WS-DGOUT-STAT
                               STOP RUN
                       END-EVALUATE
                   WHEN OTHER
                       DISPLAY 'ERROR READING DISCGRP-FLAT: '
                               WS-DGIN-STAT
                       STOP RUN
               END-EVALUATE
           END-PERFORM.
           CLOSE DISCGRP-IN
                 DISCGRP-FILE.
           DISPLAY 'DISCGRP   LOADED: ' WS-DISCGRP-CNT
                   '  DUPLICATES SKIPPED: ' WS-DISCGRP-DUP.
           EXIT.
      *
