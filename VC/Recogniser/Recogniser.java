/*
 * Recogniser.java            
 *
 * Wed 26 Feb 2025 14:06:17 AEDT
 */

/* This recogniser accepts a subset of VC defined by the following CFG: 

	program       -> func-decl
	
	// declaration
	
	func-decl     -> void identifier "(" ")" compound-stmt
	
	identifier    -> ID
	
	// statements 
	compound-stmt -> "{" stmt* "}" 
	stmt          -> continue-stmt
	    	      |  expr-stmt
	continue-stmt -> continue ";"
	expr-stmt     -> expr? ";"
	
	// expressions 
	expr                -> assignment-expr
	assignment-expr     -> additive-expr
	additive-expr       -> multiplicative-expr
	                    |  additive-expr "+" multiplicative-expr
	multiplicative-expr -> unary-expr
		            |  multiplicative-expr "*" unary-expr
	unary-expr          -> "-" unary-expr
			    |  primary-expr
	
	primary-expr        -> identifier
	 		    |  INTLITERAL
			    | "(" expr ")"
 
It serves as a good starting point for implementing your own VC recogniser. 
You can modify the existing parsing methods (if necessary) and add any missing ones 
to build a complete recogniser for VC.

Alternatively, you are free to disregard the starter code entirely and develop 
your own solution, as long as it adheres to the same public interface.

*/

package VC.Recogniser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;
import VC.ErrorReporter;

public class Recogniser {

    private Scanner scanner;
    private ErrorReporter errorReporter;
    private Token currentToken;

    public Recogniser(Scanner lexer, ErrorReporter reporter) {
        scanner = lexer;
        errorReporter = reporter;
        currentToken = scanner.getToken();
    }

    // match checks to see if the current token matches tokenExpected.
    // If so, fetches the next token.
    // If not, reports a syntactic error.
    void match(int tokenExpected) throws SyntaxError {
        if (currentToken.kind == tokenExpected) {
            currentToken = scanner.getToken();
        } else {
            syntacticError("\"%\" expected here", Token.spell(tokenExpected));
        }
    }

    // accepts the current token and fetches the next
    void accept() {
        currentToken = scanner.getToken();
    }

    // Handles syntactic errors and reports them via the error reporter.
    void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
        SourcePosition pos = currentToken.position;
        errorReporter.reportError(messageTemplate, tokenQuoted, pos);
        throw new SyntaxError();
    }

    // ========================== PROGRAMS ========================
    public void parseProgram() {
        try {
            parseFuncDecl();
            if (currentToken.kind != Token.EOF) {
                syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
            }
        } catch (SyntaxError s) { }
    }

    // ========================== DECLARATIONS ========================
    void parseFuncDecl() throws SyntaxError {
        match(Token.VOID);
        parseIdent();
        match(Token.LPAREN);
        match(Token.RPAREN);
        parseCompoundStmt();
    }

    // ======================= STATEMENTS ==============================
    void parseCompoundStmt() throws SyntaxError {
        match(Token.LCURLY);
        parseStmtList();
        match(Token.RCURLY);
    }

    // Defines a list of statements enclosed within curly braces
    void parseStmtList() throws SyntaxError {
        while (currentToken.kind != Token.RCURLY) 
            parseStmt();
    }

    void parseStmt() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.CONTINUE:
                parseContinueStmt();
                break;
            default:
                parseExprStmt();
                break;
        }
    }

    // Handles continue statements
    void parseContinueStmt() throws SyntaxError {
        match(Token.CONTINUE);
        match(Token.SEMICOLON);
    }

    // Handles expression statements, optionally parsing an expression followed by a semicolon
    void parseExprStmt() throws SyntaxError {
        if (currentToken.kind == Token.ID
                || currentToken.kind == Token.INTLITERAL
                || currentToken.kind == Token.MINUS
                || currentToken.kind == Token.LPAREN) {
            parseExpr();
            match(Token.SEMICOLON);
        } else {
            match(Token.SEMICOLON);
        }
    }

    // ======================= IDENTIFIERS ======================
    // Calls parseIdent rather than match(Token.ID). In future assignments, 
    // an Identifier node will be constructed in this method.
    void parseIdent() throws SyntaxError {
        if (currentToken.kind == Token.ID) {
            accept();
        } else {
            syntacticError("identifier expected here", "");
        }
    }

    // ======================= OPERATORS ======================
    // Calls acceptOperator rather than accept(). In future assignments, 
    // an Operator Node will be constructed in this method.
    void acceptOperator() throws SyntaxError {
        currentToken = scanner.getToken();
    }

    // ======================= EXPRESSIONS ======================
    void parseExpr() throws SyntaxError {
        parseAssignExpr();
    }

    void parseAssignExpr() throws SyntaxError {
        parseAdditiveExpr();
    }

    void parseAdditiveExpr() throws SyntaxError {
        parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS) {
            acceptOperator();
            parseMultiplicativeExpr();
        }
    }

    void parseMultiplicativeExpr() throws SyntaxError {
        parseUnaryExpr();
        while (currentToken.kind == Token.MULT) {
            acceptOperator();
            parseUnaryExpr();
        }
    }

    void parseUnaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.MINUS:
                acceptOperator();
                parseUnaryExpr();
                break;
            default:
                parsePrimaryExpr();
                break;
        }
    }

    void parsePrimaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.ID:
                parseIdent();
                break;
            case Token.LPAREN:
                accept();
                parseExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                parseIntLiteral();
                break;
            default:
                syntacticError("illegal primary expression", currentToken.spelling);
        }
    }

    // ========================== LITERALS ========================
    // Calls these methods rather than accept(). In future assignments, 
    // literal AST nodes will be constructed inside these methods.
    void parseIntLiteral() throws SyntaxError {
        if (currentToken.kind == Token.INTLITERAL) {
            accept();
        } else {
            syntacticError("integer literal expected here", "");
        }
    }

    void parseFloatLiteral() throws SyntaxError {
        if (currentToken.kind == Token.FLOATLITERAL) {
            accept();
        } else {
            syntacticError("float literal expected here", "");
        }
    }

    void parseBooleanLiteral() throws SyntaxError {
        if (currentToken.kind == Token.BOOLEANLITERAL) {
            accept();
        } else {
            syntacticError("boolean literal expected here", "");
        }
    }

}

