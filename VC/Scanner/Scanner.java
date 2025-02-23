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
    private int lineNum;
    private int colNum;

    // =========================================================

    public Scanner(SourceFile source, ErrorReporter reporter) {
        sourceFile = source;
        errorReporter = reporter;
        debug = false;

        // Initiaise currentChar for the starter code. 
        // Change it if necessary for your full implementation
        currentChar = sourceFile.getNextChar();

        // Initialise your counters for counting line and column numbers here
        lineNum = 1;
        colNum = 1;
    }

    public void enableDebugging() {
        debug = true;
    }

    // accept gets the next character from the source program.
    private void accept() {

     	currentChar = sourceFile.getNextChar();

  	// You may save the lexeme of the current token incrementally here


  	// You may also increment your line and column counters here
        if (currentChar == '\n') {
            lineNum++;
            colNum = 0;
        } else {
            colNum++;
        }
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
            
            // Handle keywords
            case 'b':
                accept();
                // boolean
                if (currentChar == 'o' && inspectChar(1) == 'o' && inspectChar(2) == 'l' && inspectChar(3) == 'e' && inspectChar(4) == 'a' && inspectChar(5) == 'n') {
                    if (keywordChecker(6) == false) {
                        return Token.ID;
                    } else {
                        return Token.BOOLEAN;
                    }
                // break
                } else if (currentChar == 'r' && inspectChar(1) == 'e' && inspectChar(2) == 'a' && inspectChar(3) == 'k') {
                    if (keywordChecker(4) == false) {
                        return Token.ID;
                    } else {
                        return Token.BREAK;
                    }
                } else {
                    return Token.ID;
                }
            case 'c':
                accept();
                // continue
                if (currentChar == 'o' && inspectChar(1) == 'n' && inspectChar(2) == 't' && inspectChar(3) == 'i' && inspectChar(4) == 'n' && inspectChar(5) == 'u' && inspectChar(6) == 'e') {
                    if (keywordChecker(7) == false) {
                        return Token.ID;
                    } else {
                        return Token.CONTINUE;
                    }
                } else {
                    return Token.ID;
                }
            case 'e':
                accept();
                // else
                if (currentChar == 'l' && inspectChar(1) == 's' && inspectChar(2) == 'e') {
                    if (keywordChecker(3)) {
                        return Token.ID;
                    } else {
                        return Token.ELSE;
                    }
                } else {
                    return Token.ID;
                }
            case 'f':
                accept();
                // float
                if (currentChar == 'l' && inspectChar(1) == 'o' && inspectChar(2) == 'a' && inspectChar(3) == 't') {
                    if (keywordChecker(4) == false) {
                        return Token.ID;
                    } else {
                        return Token.FLOAT;
                    }
                // for
                } else if (currentChar == 'o' && inspectChar(1) == 'r') {
                    if (keywordChecker(2) == false) {
                        return Token.ID;
                    } else {
                        return Token.FOR;
                    }
                } else {
                    return Token.ID;
                }
            case 'i':
                accept();
                // int
                if (currentChar == 'n' && inspectChar(1) == 't') {
                    if (keywordChecker(2) == false) {
                        return Token.ID;
                    } else {
                        return Token.INT;
                    }
                // if
                } else if (currentChar == 'f') {
                    if (keywordChecker(1) == false) {
                        return Token.ID;
                    } else {
                        return Token.IF;
                    }
                } else {
                    return Token.ID;
                }
            case 'r':
                accept();
                // return
                if (currentChar == 'e' && inspectChar(1) == 't' && inspectChar(2) == 'u' && inspectChar(3) == 'r' && inspectChar(4) == 'n') {
                    if (keywordChecker(5) == false) {
                        return Token.ID;
                    } else {
                        return Token.RETURN;
                    }
                } else {
                    return Token.ID;
                }
            case 'v':
                accept();
                // if void
                if (currentChar == 'o' && inspectChar(1) == 'i' && inspectChar(2) == 'd') {
                    if (keywordChecker(3) == false) {
                        return Token.ID;
                    } else {
                        return Token.VOID;
                    }
                } else {
                    return Token.ID;
                }
            case 'w':
                accept();
                // if while
                if (currentChar == 'h' && inspectChar(1) == 'i' && inspectChar(2) == 'l' && inspectChar(3) == 'e') {
                    if (keywordChecker(4) == false) {
                        return Token.ID;
                    } else {
                        return Token.WHILE;
                    }
                } else {
                    return Token.ID;
                }

            case '.':
       	    //  Handle floats (by calling auxiliary functions)
            
		
	    // ...
            case SourceFile.eof:
                currentSpelling.append(Token.spell(Token.EOF));
                return Token.EOF;

            default:
                break;
        }

        // Handle identifiers and numeric literals
        // ...

        accept();
        return Token.ERROR;
    }

    private boolean keywordChecker(int k) {
        boolean isKeyword = false;
        if (Character.isLetter(inspectChar(k)) || Character.isDigit(inspectChar(k)) || inspectChar(k) == '_') {
            while (Character.isLetter(inspectChar(k)) || Character.isDigit(inspectChar(k)) || inspectChar(k) == '_') {
                accept();
                k++;
            }
            return isKeyword;
        } else {
            for (int i = 0; i < k; i++) {
                accept();
            }
            return isKeyword = true;
        }
    }

    private void skipSpaceAndComments() {
        // Checker to go through all possible cases
        boolean checker = false;
        while (checker == false) {

            // whitespace case
            if (currentChar == ' ' || currentChar == '\n' || currentChar == '\t') {
                accept();
            }
            // If comment like this //
            else if (currentChar == '/' && inspectChar(1) == '/') {
                accept();
                accept();
                while (currentChar != '\n' || currentChar != '$') {
                    accept();
                }
            }
            // If comment like this /* */
            else if (currentChar == '/' && inspectChar(1) == '*') {
                accept();
                accept();   
                while ((currentChar != '*' && inspectChar(1) != '/') || currentChar != '$') {
                    accept();
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
        sourcePos.lineStart = lineNum;
        sourcePos.charStart = colNum;
	
        kind = nextToken();

        token = new Token(kind, currentSpelling.toString(), sourcePos);

   	// * do not remove these three lines below (for debugging purposes)
        if (debug) {
            System.out.println(token);
        }
        return token;
    }
}
