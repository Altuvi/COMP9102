/*
 * Recogniser.java            
 *
 * Wed 26 Feb 2025 14:06:17 AEDT
 */

/* This recogniser accepts a subset of VC defined by the following CFG: 

	program       -> ( func-decl | var-decl )*
	
	// declarations
    func-decl           -> type identifier para-list compound-stmt
    var-decl            -> type init-declarator-list ";"
    init-declarator-list-> init-declarator ( "," init-declarator )*
    init-declarator     -> declarator ( "=" initialiser )? 
    declarator          -> identifier 
                        |  identifier "[" INTLITERAL? "]"
    initialiser         -> expr 
                        |  "{" expr ( "," expr )* "}"

    // primitive types
    type                -> void | boolean | int | float

    // identifiers
    identifier          -> ID 

    // statements 
    compound-stmt       -> "{" var-decl* stmt* "}" 
    stmt                -> compound-stmt
                        |  if-stmt 
                        |  for-stmt
                        |  while-stmt 
                        |  break-stmt
                        |  continue-stmt
                        |  return-stmt
                        |  expr-stmt
    if-stmt             -> if "(" expr ")" stmt ( else stmt )?
    for-stmt            -> for "(" expr? ";" expr? ";" expr? ")" stmt
    while-stmt          -> while "(" expr ")" stmt
    break-stmt          -> break ";"
    continue-stmt       -> continue ";"
    return-stmt         -> return expr? ";"
    expr-stmt           -> expr? ";"


    // expressions 
    expr                -> assignment-expr
    assignment-expr     -> ( cond-or-expr "=" )* cond-or-expr
    cond-or-expr        -> cond-and-expr 
                        |  cond-or-expr "||" cond-and-expr
    cond-and-expr       -> equality-expr 
                        |  cond-and-expr "&&" equality-expr
    equality-expr       -> rel-expr
                        |  equality-expr "==" rel-expr
                        |  equality-expr "!=" rel-expr
    rel-expr            -> additive-expr
                        |  rel-expr "<" additive-expr
                        |  rel-expr "<=" additive-expr
                        |  rel-expr ">" additive-expr
                        |  rel-expr ">=" additive-expr
    additive-expr       -> multiplicative-expr
                        |  additive-expr "+" multiplicative-expr
                        |  additive-expr "-" multiplicative-expr
    multiplicative-expr -> unary-expr
                        |  multiplicative-expr "*" unary-expr
                        |  multiplicative-expr "/" unary-expr
    unary-expr          -> "+" unary-expr
                        |  "-" unary-expr
                        |  "!" unary-expr
                        |  primary-expr

    primary-expr        -> identifier arg-list?
                        | identifier "[" expr "]"
                        | "(" expr ")"
                        | INTLITERAL
                        | FLOATLITERAL
                        | BOOLLITERAL
                        | STRINGLITERAL

    // parameters
    para-list           -> "(" proper-para-list? ")"
    proper-para-list    -> para-decl ( "," para-decl )*
    para-decl           -> type declarator
    arg-list            -> "(" proper-arg-list? ")"
    proper-arg-list     -> arg ( "," arg )*
    arg                 -> expr
 
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
    private boolean typeChecker(int tokenKind) throws SyntaxError {
        if (tokenKind == Token.VOID || tokenKind == Token.BOOLEAN || tokenKind == Token.INT || tokenKind == Token.FLOAT) {
            return true;
        } else {
            return false;
            // syntacticError("Type expected here", "");
        }
    }

    public void parseProgram() {
        try {
            while (typeChecker(currentToken.kind)) {
                accept();
                parseIdent();
                if (currentToken.kind == Token.LPAREN) {
                    parseFuncDecl();
                } else {
                    parseVarDecl();
                }
            }
            
            if (currentToken.kind != Token.EOF) {
                syntacticError("\"%\" wrong result type for a function", currentToken.spelling);
            }
        } catch (SyntaxError s) { }
    }

    // ========================== DECLARATIONS ========================
    void parseFuncDecl() throws SyntaxError {
        parseParaList();
        parseCompoundStmt();
        // match(Token.LPAREN);
        // if (typeChecker(currentToken.kind)) {
        //     parseProperParaList();
        // }
        // match(Token.RPAREN);
    }

    void parseVarDecl() throws SyntaxError {
        parseInitDeclaratorList();
        match(Token.SEMICOLON);
    }

    void parseInitDeclaratorList() throws SyntaxError {
        parseInitDeclarator();
        while (currentToken.kind == Token.COMMA) {
            accept();
            parseInitDeclarator();
        }
    }

    void parseInitDeclarator() throws SyntaxError {
        parseDeclarator();
        if (currentToken.kind == Token.EQ) {
            accept();
            parseInitialiser();
        }
    }

    void parseDeclarator() throws SyntaxError {
        parseIdent();
        if (currentToken.kind == Token.LBRACKET) {
            accept();
            if (currentToken.kind == Token.INTLITERAL) {
                parseIntLiteral();
            }
            match(Token.RBRACKET);
        }
    }

    void parseInitialiser() throws SyntaxError {
        if (currentToken.kind == Token.LCURLY) {
            accept();
            parseExpr();
            while (currentToken.kind == Token.COMMA) {
                accept();
                parseExpr();
            }
            match(Token.RCURLY);
        } else {
            parseExpr();
        }
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

    void parseWhileStmt() throws SyntaxError {
        match(Token.WHILE);
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
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

