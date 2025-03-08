/*
 * Recogniser.java            
 *
 * Wed 26 Feb 2025 14:06:17 AEDT
 */

/* This recogniser accepts a subset of VC defined by the following CFG: 

	program       -> ( func-decl | var-decl )*
	
	// declarations
    func-decl           -> type identifier para-list compound-stmt
    var-decl            -> type init-declarator-list ";"                DONE                
    init-declarator-list-> init-declarator ( "," init-declarator )*     DONE
    init-declarator     -> declarator ( "=" initialiser )?              DONE
    declarator          -> identifier                                   DONE
                        |  identifier "[" INTLITERAL? "]"               DONE
    initialiser         -> expr 
                        |  "{" expr ( "," expr )* "}"

    // primitive types
    type                -> void | boolean | int | float

    // identifiers
    identifier          -> ID                                           DONE

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
    // private boolean identParsed = false;

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
    // program             ->  ( func-decl | var-decl )*
    // eliminates common prefixes using choice operator
    // program             ->  type identifier ( para-list compound-stmt | init-declarator-list ";" )*
    
    public void parseProgram() {
        try {
            while (currentToken.kind != Token.EOF) {
                parseType();
                parseIdent();
                // identParsed = true;
                if (currentToken.kind == Token.LPAREN) {
                    parseFuncDecl();    // para-list compound-stmt
                } else {
                    parseVarDecl();     // init-declarator-list
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
    }

    void parseVarDecl() throws SyntaxError {
        parseInitDeclaratorList();
        match(Token.SEMICOLON);
    }

    void parseInitDeclaratorList() throws SyntaxError {
        parseInitDeclarator();
        while (currentToken.kind == Token.COMMA) {
            accept();
            parseIdent();
            // identParsed = false;
            parseInitDeclarator();
        }
    }

    void parseInitDeclarator() throws SyntaxError {
        parseDeclarator();
        if (currentToken.kind == Token.EQ) {
            accept();
            // identParsed = false;
            parseInitialiser();
        }
    }

    void parseDeclarator() throws SyntaxError {     
        // if (currentToken.kind == Token.ID) {
        //     parseIdent();
        // }  
        // if (!identParsed) {
        //     parseIdent();
        //     identParsed = true;
        // }
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

    // ======================= TYPES ==============================

    boolean typeChecker(int tokenKind) throws SyntaxError {
        if (tokenKind == Token.VOID || tokenKind == Token.BOOLEAN || tokenKind == Token.INT || tokenKind == Token.FLOAT) {
            return true;
        } else {
            return false;
        }
    }

    void parseType() throws SyntaxError {
        if (typeChecker(currentToken.kind)) {
            accept();
        } else {
            syntacticError("Type expected here", "");
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

    // ======================= STATEMENTS ==============================
    void parseCompoundStmt() throws SyntaxError {
        match(Token.LCURLY);
        // var-decl*
        while (typeChecker(currentToken.kind)) {
            parseType();
            parseIdent();
            parseVarDecl();
        }
        // stmt*
        while (currentToken.kind != Token.RCURLY) {
            parseStmt();
        }
        match(Token.RCURLY);
    }

    // Defines a list of statements enclosed within curly braces
    // void parseStmtList() throws SyntaxError {
    //     while (currentToken.kind != Token.RCURLY) 
    //         parseStmt();
    // }

    void parseStmt() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.LCURLY:
                parseCompoundStmt();
                break;
            case Token.IF:
                parseIfStmt();
                break;
            case Token.FOR:
                parseForStmt();
                break;
            case Token.WHILE:
                parseWhileStmt();
                break;
            case Token.BREAK:
                parseBreakStmt();
                break;
            case Token.CONTINUE:
                parseContinueStmt();
                break;
            case Token.RETURN:
                parseReturnStmt();
                break;  
            default:
                parseExprStmt();
                break;
        }
    }

    // Handles if statements
    void parseIfStmt() throws SyntaxError {
        match(Token.IF);
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
        if (currentToken.kind == Token.ELSE) {
            accept();
            parseStmt();
        }
    }

    // Handles for statements
    void parseForStmt() throws SyntaxError {
        match(Token.FOR);
        match(Token.LPAREN);
        if (currentToken.kind != Token.SEMICOLON) {
            parseExpr();
        }
        match(Token.SEMICOLON);
        if (currentToken.kind != Token.SEMICOLON) {
            parseExpr();
        }
        match(Token.SEMICOLON);
        if (currentToken.kind != Token.RPAREN) {
            parseExpr();
        }
        match(Token.RPAREN);
        parseStmt();
    }    

    // Handles while statements
    void parseWhileStmt() throws SyntaxError {
        match(Token.WHILE);
        match(Token.LPAREN);
        parseExpr();
        match(Token.RPAREN);
        parseStmt();
    }

    // Handles break statements
    void parseBreakStmt() throws SyntaxError {
        match(Token.BREAK);
        match(Token.SEMICOLON);
    }

    // Handles continue statements
    void parseContinueStmt() throws SyntaxError {
        match(Token.CONTINUE);
        match(Token.SEMICOLON);
    }

    // Handles return statements
    void parseReturnStmt() throws SyntaxError {
        match(Token.RETURN);
        if (currentToken.kind != Token.SEMICOLON) {
            parseExpr();
        }
        match(Token.SEMICOLON);
    }

    // Handles expression statements, optionally parsing an expression followed by a semicolon
    void parseExprStmt() throws SyntaxError {
        if (currentToken.kind != Token.SEMICOLON) {
            parseExpr();
        }
        match(Token.SEMICOLON);
    }

    // Handles expression statements, optionally parsing an expression followed by a semicolon
    // void parseExprStmt() throws SyntaxError {
    //     if (currentToken.kind == Token.ID
    //             || currentToken.kind == Token.INTLITERAL
    //             || currentToken.kind == Token.MINUS
    //             || currentToken.kind == Token.LPAREN) {
    //         parseExpr();
    //         match(Token.SEMICOLON);
    //     } else {
    //         match(Token.SEMICOLON);
    //     }
    // }

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

    // assignment-expr     -> ( cond-or-expr "=" )* cond-or-expr
    void parseAssignExpr() throws SyntaxError {
        parseCondOrExpr();
        while (currentToken.kind == Token.EQ) {
            acceptOperator();
            parseCondOrExpr();
        }
    }

    // cond-or-expr        -> cond-and-expr
    //                     |  cond-or-expr "||" cond-and-expr
    // Eliminating left recursion
    // cond-or-expr        -> cond-and-expr ( "||" cond-and-expr )*
    void parseCondOrExpr() throws SyntaxError {
        parseCondAndExpr();
        while (currentToken.kind == Token.OROR) {
            acceptOperator();
            parseCondAndExpr();
        }
    }

    // cond-and-expr       -> equality-expr
    //                     |  cond-and-expr "&&" equality-expr
    // Eliminating left recursion
    // cond-and-expr       -> equality-expr ( "&&" equality-expr )*
    void parseCondAndExpr() throws SyntaxError {
        parseEqualityExpr();
        while (currentToken.kind == Token.ANDAND) {
            acceptOperator();
            parseEqualityExpr();
        }
    }

    // equality-expr       -> rel-expr
    //                     |  equality-expr "==" rel-expr
    //                     |  equality-expr "!=" rel-expr
    // Eliminating left recursion
    // equality-expr       -> rel-expr ( "==" rel-expr | "!=" rel-expr )*
    void parseEqualityExpr() throws SyntaxError {
        parseRelExpr();
        while (currentToken.kind == Token.EQEQ || currentToken.kind == Token.NOTEQ) {
            acceptOperator();
            parseRelExpr();
        }
    }

    // rel-expr            -> additive-expr
    //                     |  rel-expr "<" additive-expr
    //                     |  rel-expr "<=" additive-expr
    //                     |  rel-expr ">" additive-expr
    //                     |  rel-expr ">=" additive-expr
    // Eliminating left recursion
    // rel-expr            -> additive-expr ( "<" additive-expr | "<=" additive-expr | ">" additive-expr | ">=" additive-expr )*
    void parseRelExpr() throws SyntaxError {
        parseAdditiveExpr();
        while (currentToken.kind == Token.LT || currentToken.kind == Token.LTEQ || currentToken.kind == Token.GT || currentToken.kind == Token.GTEQ) {
            acceptOperator();
            parseAdditiveExpr();
        }
    }

    // additive-expr       -> multiplicative-expr
    //                     |  additive-expr "+" multiplicative-expr
    //                     |  additive-expr "-" multiplicative-expr
    // Eliminating left recursion
    // additive-expr       -> multiplicative-expr ( "+" multiplicative-expr | "-" multiplicative-expr )*
    void parseAdditiveExpr() throws SyntaxError {
        parseMultiplicativeExpr();
        while (currentToken.kind == Token.PLUS || currentToken.kind == Token.MINUS) {
            acceptOperator();
            parseMultiplicativeExpr();
        }
    }

    // multiplicative-expr -> unary-expr
    //                     |  multiplicative-expr "*" unary-expr
    //                     |  multiplicative-expr "/" unary-expr
    // Eliminating left recursion
    // multiplicative-expr -> unary-expr ( "*" unary-expr | "/" unary-expr )*
    void parseMultiplicativeExpr() throws SyntaxError {
        parseUnaryExpr();
        while (currentToken.kind == Token.MULT || currentToken.kind == Token.DIV) {
            acceptOperator();
            parseUnaryExpr();
        }
    }

    // unary-expr          -> "+" unary-expr
    //                     |  "-" unary-expr
    //                     |  "!" unary-expr
    //                     |  primary-expr
    void parseUnaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.PLUS:
                acceptOperator();
                parseUnaryExpr();
                break;
            case Token.MINUS:
                acceptOperator();
                parseUnaryExpr();
                break;
            case Token.NOT:
                acceptOperator();
                parseUnaryExpr();
                break;
            default:
                parsePrimaryExpr();
                break;
        }
    }

    // primary-expr        -> identifier arg-list?
    //                     | identifier "[" expr "]"
    //                     | "(" expr ")"
    //                     | INTLITERAL
    //                     | FLOATLITERAL
    //                     | BOOLLITERAL
    //                     | STRINGLITERAL
    void parsePrimaryExpr() throws SyntaxError {
        switch (currentToken.kind) {
            case Token.ID:
                parseIdent();
                if (currentToken.kind == Token.LPAREN) {
                    parseArgList();
                } else if (currentToken.kind == Token.LBRACKET) {
                    accept();
                    parseExpr();
                    match(Token.RBRACKET);
                }
                break;
            case Token.LPAREN:
                accept();
                parseExpr();
                match(Token.RPAREN);
                break;
            case Token.INTLITERAL:
                parseIntLiteral();
                break;
            case Token.FLOATLITERAL:
                parseFloatLiteral();
                break;
            case Token.BOOLEANLITERAL:
                parseBooleanLiteral();
                break;
            case Token.STRINGLITERAL:
                parseStringLiteral();   
                break;
            default:
                break;
        }
    }

    // ========================== PARAMETERS ========================
    void parseParaList() throws SyntaxError {
        match(Token.LPAREN);
        if (typeChecker(currentToken.kind)) {
            parseProperParaList();
        }
        match(Token.RPAREN);
    }

    void parseProperParaList() throws SyntaxError {
        parseParaDecl();
        while (currentToken.kind == Token.COMMA) {
            accept();
            parseParaDecl();
        }
    }

    void parseParaDecl() throws SyntaxError {
        if (typeChecker(currentToken.kind)) {
            parseType();
            parseIdent();
            parseDeclarator();
        }
    }

    void parseArgList() throws SyntaxError {
        match(Token.LPAREN);
        if (currentToken.kind != Token.RPAREN) {
            parseProperArgList();
        }
        match(Token.RPAREN);
    }

    void parseProperArgList() throws SyntaxError {
        parseArg();
        while (currentToken.kind == Token.COMMA) {
            accept();
            parseArg();
        }
    }

    void parseArg() throws SyntaxError {
        parseExpr();
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

    void parseStringLiteral() throws SyntaxError {
        if (currentToken.kind == Token.STRINGLITERAL) {
            accept();
        } else {
            syntacticError("string literal expected here", "");
        }
    }

}