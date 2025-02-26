/*
 * Scanner.java                        
 *
 * Sun 09 Feb 2025 13:31:52 AEDT
 *
 * The starter code here is provided as a high-level guide for implementation.
 *
 * You may completely disregard the starter code and develop your own solution, 
 * provided that it maintains the same public interface.
 *
 */

package VC.Scanner;

import javax.print.FlavorException;

import VC.ErrorReporter;

public final class Scanner {

    private SourceFile sourceFile;
    private ErrorReporter errorReporter;
    private boolean debug;

    private StringBuilder currentSpelling;
    private char currentChar;
    private SourcePosition sourcePos;
    private int lineCounter;
    private int colCounter;

    // =========================================================

    public Scanner(SourceFile source, ErrorReporter reporter) {
        sourceFile = source;
        errorReporter = reporter;
        debug = false;

        // Initiaise currentChar for the starter code. 
        // Change it if necessary for your full implementation
        currentChar = sourceFile.getNextChar();

        // Initialise your counters for counting line and column numbers here
        lineCounter = 1;
        colCounter = 1;
    }

    public void enableDebugging() {
        debug = true;
    }

    // accept gets the next character from the source program.
    private void accept() {

    // You may save the lexeme of the current token incrementally here
        currentSpelling.append(currentChar);

  	// You may also increment your line and column counters here
        if (currentChar == '\n') {
            lineCounter++;
            colCounter = 1;
        } else if (currentChar == '\t') {
            colCounter = ((colCounter + 7) / 8) * 8 + 1;
            // if (colCounter % 8 == 0) { // if colCounter is multiple of 8
			// 	colCounter++; // increment colCounter by 1
			// } else {
			// 	// e.g. colCounter = 3 --> new colCounter = 3 + (9 - (3 % 8)) = 9
			// 	colCounter += 9 - (colCounter % 8); // 
			// }
        } else {
            colCounter++;
        }
     	currentChar = sourceFile.getNextChar();
    }

    private void acceptSpaceComment() {
        if (currentChar == '\n') {
            lineCounter++;
            colCounter = 1;
        } else if (currentChar == '\t') {
            colCounter = ((colCounter + 7) / 8) * 8 + 1;
            // if (colCounter % 8 == 0) { // if colCounter is multiple of 8
			// 	colCounter++; // increment colCounter by 1
			// } else {
			// 	// e.g. colCounter = 3 --> new colCounter = 3 + (9 - (3 % 8)) = 9
			// 	colCounter += 9 - (colCounter % 8);
			// }
        } else {
            colCounter++;
        }
        currentChar = sourceFile.getNextChar();
    }


    // inspectChar returns the n-th character after currentChar in the input stream. 
    // If there are fewer than nthChar characters between currentChar 
    // and the end of file marker, SourceFile.eof is returned.
    // 
    // Both currentChar and the current position in the input stream
    // are *not* changed. Therefore, a subsequent call to accept()
    // will always return the next char after currentChar.

    // That is, inspectChar does not change 

    private char inspectChar(int nthChar) {
        return sourceFile.inspectChar(nthChar);
    }

    private int nextToken() {
        // Tokens: separators, operators, literals, identifiers, and keywords
        switch (currentChar) {
            // Handle separators
            case '(':
                accept();
                return Token.LPAREN;
            case ')':
                accept();
                return Token.RPAREN;
            case '[':
                accept();
                return Token.LBRACKET;
            case ']':
                accept();
                return Token.RBRACKET;
            case '{':
                accept();
                return Token.LCURLY;
            case '}':
                accept();
                return Token.RCURLY;
            case ';':
                accept();
                return Token.SEMICOLON;
            case ',':
                accept();
                return Token.COMMA;

            // Handle operators
            case '+':
                accept();
                return Token.PLUS;
            case '-':
                accept();
                return Token.MINUS;
            case '*':
                accept();
                return Token.MULT;
            case '/':
                accept();
                return Token.DIV;
            case '!':
                accept();
                if (currentChar == '=') {
                    accept();
                    return Token.NOTEQ;
                } else {
                    return Token.NOT;
                }
            case '=':
                accept();
                if (currentChar == '=') {
                    accept();
                    return Token.EQEQ;
                } else {
                    return Token.EQ;
                }
            case '<':
                accept();
                if (currentChar == '=') {
                    accept();
                    return Token.LTEQ;
                } else {
                    return Token.LT;
                }
            case '>':
                accept();
                if (currentChar == '=') {
                    accept();
                    return Token.GTEQ;
                } else {
                    return Token.GT;
                }
            case '&':
                accept();
                if (currentChar == '&') {
                    accept();
                    return Token.ANDAND;
                } else {
                    return Token.ERROR;
                }
            case '|':
                accept();
                if (currentChar == '|') {
                    accept();
                    return Token.OROR;
                } else {
                    return Token.ERROR;
                }

            case '.':
       	    //  Handle floats (by calling auxiliary functions)
                accept();
                // .digit+ exponent?
                if (Character.isDigit(currentChar)) {
                    while (Character.isDigit(currentChar)) {
                        accept();
                    }
                    // .digit+ exponent
                    if (currentChar == 'e' || currentChar == 'E') {
                        return isExponent();
                    } else {
                        return Token.FLOATLITERAL; // e.g. .121
                    }
                } else {
                    return Token.ERROR; // e.g. .abc, .e12
                }
		
	    // ...
            case SourceFile.eof:
                accept();
                currentSpelling.append(Token.spell(Token.EOF));
                return Token.EOF;

            default:
                break;
        }

        // Handle identifiers and numeric literals
        // Handle identifiers (Token.java converts identifiers into keywords if identifier is keyword)
        if (Character.isLetter(currentChar) || currentChar == '_') {
            if (isBooleanLiteral()) {
                return Token.BOOLEANLITERAL;
            } else {
                accept();
                while (Character.isLetter(currentChar) || currentChar == '_' || Character.isLetter(currentChar)) {
                    accept();
                }
                return Token.ID;
            }
            
        }

        // Handle numeric literals (integers and floats)
        // Case 1: digit+ fraction exponent?
        // Case 2: digit+.
        // Case 3: digit+.?exponent
        if (Character.isDigit(currentChar)) {
            accept();
            while (Character.isDigit(currentChar)) {
                accept();
            }
            if (currentChar == '.') {
                accept();
                // Case 1a: digit+ .digit+ exponent
                if (Character.isDigit(currentChar)) {
                    // digit+ .digit+ e.g. 1.23
                    while (Character.isDigit(currentChar)) {
                        accept();
                    }
                    // digit .digit+ exponent e.g. 1.23E
                    if (currentChar == 'e' || currentChar == 'E') {
                        return isExponent();
                    // else 
                    } else {
                        return Token.FLOATLITERAL;
                    }
                } else if (currentChar == 'e' || currentChar == 'E') {
                    return isExponent();
                } else {
                    return Token.FLOATLITERAL; // e.g. '1.' in '1. etft' is a float literal
                }
            // Case 3b: digit+ exponent
            } else if (currentChar == 'e' || currentChar == 'E') {
                // digitE(+|-)digit+
                return isExponent();
            } else {
                return Token.INTLITERAL;
            }
        }

        // Handle string literals and escape characters
        if (currentChar == '"') {
            int lStart = lineCounter;
            int cStart = colCounter;
            SourcePosition errorPos = new SourcePosition();
            accept();
            while (currentChar != '"' && currentChar != '\n' && currentChar != sourceFile.eof) {
                // Handle escape characters
                if (currentChar == '\\') {
                    // accept();
                    if (isEscapeCharacter()) {
                        accept();
                    } else {    // illegal escape character
                        
                        // report error
                        errorPos.lineStart = lStart;
                        errorPos.charStart = cStart;
                        errorPos.lineFinish = lineCounter;
                        errorPos.charFinish = colCounter;
                        errorReporter.reportError("illegal escape character", "ERROR", errorPos);
                        // continue rest of remaining input
                        accept();
                    }
                } else {
                    // Handle other characters
                    accept();   
                }
            }
            if (currentChar == '"') {
                // terminated string
                accept();
                return Token.STRINGLITERAL;

            } else if (currentChar == '\n' || currentChar == sourceFile.eof) {
                // unterminated string
                errorPos.lineStart = lStart;
                errorPos.charStart = cStart;
                errorPos.lineFinish = lStart;
                errorPos.charFinish = cStart;
                errorReporter.reportError("unterminated string", "ERROR", errorPos);
                // return string token that could be made
            }

            // Handle if unterminated string
        }

        accept();
        return Token.ERROR;
    }

    private int isExponent() {
        // digitE(+|-)digit+ e.g. 121E+12 OR .E+121
        if ((inspectChar(1) == '+' || inspectChar(1) == '-') && Character.isDigit(inspectChar(2))) {
            accept(); // e
            accept(); // +/-
            accept(); // digit
            while (Character.isDigit(currentChar)) {
                accept(); // accept rest of digits if any
            }
            return Token.FLOATLITERAL;
        // digitEdigit+ e.g. 121E121 OR .E121
        } else if (Character.isDigit(inspectChar(1))){
            accept(); // e
            accept(); // digit
            while (Character.isDigit(currentChar)) {
                accept(); // accept rest of digits if any
            }
            return Token.FLOATLITERAL;
        // if next character after E is not a digit or a +/- then E should be an ID
        } else if (currentChar == '.') { 
            // accept();
            return Token.FLOATLITERAL; // '1.' in '1.ef' is a float
            // return Token.ERROR; // e.g. 121ef or 121e_
        } else {
            return Token.INTLITERAL; // '123' in '123ef' is an int
        }
    }

    private boolean isBooleanLiteral() {
        if (currentChar == 't' && inspectChar(1) == 'r' && inspectChar(2) == 'u' && inspectChar(3) == 'e') {
            accept();
            accept();
            accept();
            accept();
            return true;
        } else if (currentChar == 'f' && inspectChar(1) == 'a' && inspectChar(2) == 'l' && inspectChar(3) == 's' && inspectChar(4) == 'e') {
            accept();
            accept();
            accept();
            accept();
            accept();
            return true;
        } else {
            return false;
        }
    }

    private boolean isEscapeCharacter() {
        switch (currentChar) {
            case 'b':   // backspace
                currentChar = '\b';
                return true;
            case 'f':   // formfeed
                currentChar = '\f';
                return true;
            case 'n':   // new line
                currentChar = '\n';
                return true;
            case 'r':   // carriage return
                currentChar = '\r';
                return true;
            case 't':   // horizontal tab
                currentChar = '\t';
                return true;
            case '\'':  // single quote
                currentChar = '\'';
                return true;
            case '\"':  // double quote
                currentChar = '\"';
                return true;
            case '\\':  // backslash
                currentChar = '\\';
                return true;
            default:
                return false;
        }
    }

    private void skipSpaceAndComments() {
        // Checker to go through all possible cases
        boolean checker = false;
        while (checker == false) {
            // whitespace case
            if (currentChar == ' ' || currentChar == '\n' || currentChar == '\t') {
                acceptSpaceComment();
            }
            // If comment like this //
            else if (currentChar == '/' && inspectChar(1) == '/') {
                acceptSpaceComment();
                acceptSpaceComment();
                while (currentChar != '\n' && currentChar != SourceFile.eof) {
                    acceptSpaceComment();
                }
            }
            // If comment like this /* */
            else if (currentChar == '/' && inspectChar(1) == '*') {
                int lStart = lineCounter;
                int cStart = colCounter;
                acceptSpaceComment();
                acceptSpaceComment();   
                // System.out.println(currentChar);
                while (!(currentChar == '*' && inspectChar(1) == '/') && currentChar != SourceFile.eof) {
                    acceptSpaceComment();
                }
                if (currentChar == '*' && inspectChar(1) == '/') {
                    acceptSpaceComment();
                    acceptSpaceComment();
                // reached eof --> unterminated
                } else {
                    SourcePosition errorPos = new SourcePosition();
                    errorPos.lineStart = lStart;
                    errorPos.lineFinish = errorPos.lineStart;
                    errorPos.charStart = cStart;
                    errorPos.charFinish = errorPos.charStart;
                    errorReporter.reportError("unterminated comment", "ERROR", errorPos);
                    // checker = true;
                }
            } else {
                checker = true;
            }
        }
    }

    public Token getToken() {
        Token token;
        int kind;

        // Skip white space and comments
        skipSpaceAndComments();

        currentSpelling = new StringBuilder();

        sourcePos = new SourcePosition();

        // You need to record the position of the current token somehow
        sourcePos.lineStart = lineCounter;
        sourcePos.lineFinish = sourcePos.lineStart;
        sourcePos.charStart = colCounter;
	
        kind = nextToken();

        if (colCounter - 1 > 0) {
            sourcePos.charFinish = colCounter - 1;
        }
        
        token = new Token(kind, currentSpelling.toString(), sourcePos);

   	// * do not remove these three lines below (for debugging purposes)
        if (debug) {
            System.out.println(token);
        }
        return token;
    }
}
