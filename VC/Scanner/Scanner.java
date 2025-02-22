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
            // boolean
            case 'b':
                ///


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
