/*
 * Parser.java 
 *
 * Thu 06 Mar 2025 12:58:29 AEDT
 *
 * PLEASE COMPARE Recogniser.java PROVIDED IN ASSIGNMENT 2 AND Parser.java
 * PROVIDED BELOW TO UNDERSTAND HOW THE FORMER IS MODIFIED TO OBTAIN THE LATTER.
 *
 * This parser for a subset of the VC language is intended to 
 *  demonstrate how to create the AST nodes, including (among others): 
 *  (1) a list (of statements)
 *  (2) a function
 *  (3) a statement (which is an expression statement), 
 *  (4) a unary expression
 *  (5) a binary expression
 *  (6) terminals (identifiers, integer literals and operators)
 *
 * In addition, it also demonstrates how to use the two methods start 
 * and finish to determine the position information for the start and 
 * end of a construct (known as a phrase) corresponding an AST node.
 *
 * NOTE THAT THE POSITION INFORMATION WILL NOT BE MARKED. HOWEVER, IT CAN BE
 * USEFUL TO DEBUG YOUR IMPLEMENTATION.
 *
 * Note that what is provided below is an implementation for a subset of VC
 * given below rather than VC itself. It provides a good starting point for you
 * to implement a parser for VC yourself, by modifying the parsing methods
 * provided (whenever necessary).
 *
 *
 * Alternatively, you are free to disregard the starter code entirely and 
 * develop your own solution, as long as it adheres to the same public 
 * interface.


program       -> func-decl
func-decl     -> type identifier "(" ")" compound-stmt
type          -> void
identifier    -> ID
// statements
compound-stmt -> "{" stmt* "}" 
stmt          -> expr-stmt
expr-stmt     -> expr? ";"
// expressions 
expr                -> additive-expr
additive-expr       -> multiplicative-expr
                    |  additive-expr "+" multiplicative-expr
                    |  additive-expr "-" multiplicative-expr
multiplicative-expr -> unary-expr
	            |  multiplicative-expr "*" unary-expr
	            |  multiplicative-expr "/" unary-expr
unary-expr          -> "-" unary-expr
		    |  primary-expr

primary-expr        -> identifier
 		    |  INTLITERAL
		    | "(" expr ")"
 */

package VC.Parser;

import VC.Scanner.Scanner;
import VC.Scanner.SourcePosition;
import VC.Scanner.Token;

import java.lang.reflect.Array;
import java.rmi.server.ExportException;

import VC.ErrorReporter;
import VC.ASTs.*;

public class Parser {

  private Scanner scanner;
  private ErrorReporter errorReporter;
  private Token currentToken;
  private SourcePosition previousTokenPosition;
  private SourcePosition dummyPos = new SourcePosition();

  public Parser (Scanner lexer, ErrorReporter reporter) {
    scanner = lexer;
    errorReporter = reporter;

    previousTokenPosition = new SourcePosition();

    currentToken = scanner.getToken();
  }

// match checks to see f the current token matches tokenExpected.
// If so, fetches the next token.
// If not, reports a syntactic error.

  void match(int tokenExpected) throws SyntaxError {
    if (currentToken.kind == tokenExpected) {
      previousTokenPosition = currentToken.position;
      currentToken = scanner.getToken();
    } else {
      syntacticError("\"%\" expected here", Token.spell(tokenExpected));
    }
  }

  void accept() {
    previousTokenPosition = currentToken.position;
    currentToken = scanner.getToken();
  }

  void syntacticError(String messageTemplate, String tokenQuoted) throws SyntaxError {
    SourcePosition pos = currentToken.position;
    errorReporter.reportError(messageTemplate, tokenQuoted, pos);
    throw(new SyntaxError());
  }

// start records the position of the start of a phrase.
// This is defined to be the position of the first
// character of the first token of the phrase.

  void start(SourcePosition position) {
    position.lineStart = currentToken.position.lineStart;
    position.charStart = currentToken.position.charStart;
  }

// finish records the position of the end of a phrase.
// This is defined to be the position of the last
// character of the last token of the phrase.

  void finish(SourcePosition position) {
    position.lineFinish = previousTokenPosition.lineFinish;
    position.charFinish = previousTokenPosition.charFinish;
  }

  void finishCurrent(SourcePosition position) {
    position.lineFinish = currentToken.position.lineFinish;
    position.charFinish = currentToken.position.charFinish;
  }

  void copyStart(SourcePosition from, SourcePosition to) {
    to.lineStart = from.lineStart;
    to.charStart = from.charStart;
  }

  boolean typeChecker(int tokenKind) throws SyntaxError {
    if (tokenKind == Token.VOID || tokenKind == Token.BOOLEAN || tokenKind == Token.INT || tokenKind == Token.FLOAT) {
        return true;
    } else {
        return false;
    }
}

// ========================== PROGRAMS ========================

  public Program parseProgram() {

    Program programAST = null;
    
    SourcePosition programPos = new SourcePosition();
    start(programPos);

    try {
      List dlAST = parseDecList();
      finish(programPos);
      programAST = new Program(dlAST, programPos); 
      if (currentToken.kind != Token.EOF) {
        syntacticError("\"%\" unknown type", currentToken.spelling);
      }
    }
    catch (SyntaxError s) { return null; }
    return programAST;
  }

// ========================== DECLARATIONS ========================

  List parseDecList() throws SyntaxError {
    List dlAST = null;
    Decl dAST = null;

    // SourcePosition funcPos = new SourcePosition();
    // start(funcPos);
    SourcePosition decListPos = new SourcePosition();
    start(decListPos);
    Type tAST = null; 
    Ident iAST = null;
    // typechecker
    if (typeChecker(currentToken.kind)) {
      tAST = parseType();
      iAST = parseIdent();
      if (currentToken.kind == Token.LPAREN) {
        dAST = parseFuncDecl(tAST, iAST);
      } else {
        dAST = parseGlobalVar(tAST, iAST, dlAST);
      }
    } else if (currentToken.kind == Token.ID) {
      iAST = parseIdent();
      dAST = parseGlobalVar(tAST, iAST, dlAST);
    }
    // } else {
    //   // syntacticError("Type expected here", "");
    // }


    // if (dAST == null) 
    //   dlAST = new EmptyDeclList(dummyPos);

    // Recursive call to parseDecList
    if (typeChecker(currentToken.kind) || currentToken.kind == Token.ID) {
      dlAST = parseDecList();
      finish(decListPos);
      dlAST = new DeclList(dAST, dlAST, decListPos);
    // } else if (currentToken.kind == Token.ID) {
    //   dlAST = null;
    //   iAST = parseIdent();
    //   dAST = parseGlobalVar(tAST, iAST, dlAST);
    //   finish(decListPos);
    //   dlAST = new DeclList(dAST, dlAST, decListPos);
    } else {
      if (dAST != null) {
        finish(decListPos);
        dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), decListPos);
      } else {
        dlAST = new EmptyDeclList(dummyPos);
      }
    }
    
    if (dlAST == null) 
      dlAST = new EmptyDeclList(dummyPos);

    return dlAST;
  }

  // List parseGlobalDecList() throws SyntaxError {
  //   // current token should be an identifier after the first one on the same line
  //   List dlAST = null;
  //   Decl dAST = null;

  // }

  List parseLocalDecList() throws SyntaxError {
    List dlAST = null;
    Decl dAST = null;

    SourcePosition decListPos = new SourcePosition();
    start(decListPos);
    Type tAST = null;
    Ident iAST = null;
    // typechecker
    if (typeChecker(currentToken.kind)) {
      tAST = parseType();
      iAST = parseIdent();
      dAST = parseLocalVar(tAST, iAST);
    } else if (currentToken.kind == Token.ID) {
      iAST = parseIdent();
      dAST = parseLocalVar(tAST, iAST);
    }
    
    // Recursive call to parseDecList
    if (typeChecker(currentToken.kind) || currentToken.kind == Token.ID) {
      dlAST = parseLocalDecList();
      finish(decListPos);
      dlAST = new DeclList(dAST, dlAST, decListPos);
    } else {
      if (dAST != null) {
        finish(decListPos);
        dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), decListPos);
      } else {
        dlAST = new EmptyDeclList(dummyPos);
      }
    }
    
    if (dlAST == null) 
      dlAST = new EmptyDeclList(dummyPos);

    return dlAST;
  }

  Decl parseFuncDecl(Type tAST, Ident iAST) throws SyntaxError {
    
    Decl fAST = null; 
    
    SourcePosition funcPos = new SourcePosition();
    start(funcPos);

    // current token should be "("
    match(Token.LPAREN);
    List fplAST = parseParaList();
    Stmt cAST = parseCompoundStmt();
    finish(funcPos);
    fAST = new FuncDecl(tAST, iAST, fplAST, cAST, funcPos);
    return fAST;
  }

  Decl parseGlobalVar(Type tAST, Ident iAST, List dlAST) throws SyntaxError{
    // currentToken.kind should point to an expression if there is one
    
    Decl gvAST = null;
    Type gvType = null;
    Ident gvIdent = iAST;
    Expr gvExpr = null;

    SourcePosition gvPos = new SourcePosition();
    start(gvPos);

    // Parse expression
    // if "[" INTLITERAL? "]"
    if (currentToken.kind == Token.LBRACKET) {
      gvType = parseArrayType();
    } else {
      gvType = tAST;
    }

    // if ( "=" initialiser )?
    if (currentToken.kind == Token.EQ) {
      Operator eqOp = acceptOperator();
      gvExpr = parseInitialiser();
    } else {
      gvExpr = new EmptyExpr(dummyPos);
    }

    // ( "," init-declarator )* 
    // ==> end of global variable declaration so create the node
    // ==> make new DecList node with next global variable declaration as the child
    if (currentToken.kind == Token.COMMA) {
      accept(); 
      finish(gvPos);
       gvAST = new GlobalVarDecl(gvType, gvIdent, gvExpr, gvPos);
      // currentToken should be an identifier so go back to calling parseDecList
      
      return gvAST; 
    } 

    // Should reach end of last global variable here
    match(Token.SEMICOLON);
    finish(gvPos);
    gvAST = new GlobalVarDecl(gvType, gvIdent, gvExpr, gvPos);
    return gvAST;
  }

  Decl parseLocalVar(Type tAST, Ident iAST) throws SyntaxError {
    Decl lvAST = null;
    Ident lvIdent = iAST;
    Type lvType = null;
    Expr lvExpr = null;

    SourcePosition lvPos = new SourcePosition();
    start(lvPos);

    // Parse expression
    // if "[" INTLITERAL? "]"
    if (currentToken.kind == Token.LBRACKET) {
      lvType = parseArrayType();
    } else {
      lvType = tAST;
    }

    // if ( "=" initialiser )?
    if (currentToken.kind == Token.EQ) {
      Operator eqOp = acceptOperator();
      lvExpr = parseInitialiser();
    } else {
      lvExpr = new EmptyExpr(dummyPos);
    }

    // ( "," init-declarator )*
    // ==> end of local variable declaration so create the node
    // ==> make new DecList node with next local variable declaration as the child
    if (currentToken.kind == Token.COMMA) {
      accept(); 
      finish(lvPos);
      lvAST = new LocalVarDecl(lvType, lvIdent, lvExpr, lvPos);
      // currentToken should be an identifier so go back to calling parseDecList
      return lvAST; 
    }

    // Should reach end of last local variable here
    match(Token.SEMICOLON);
    finish(lvPos);
    lvAST = new LocalVarDecl(lvType, lvIdent, lvExpr, lvPos);
    return lvAST;
  }

//  ======================== TYPES ==========================

  Type parseType() throws SyntaxError {
    Type typeAST = null;
    Expr exprAST = null;
    int tokenType = currentToken.kind;

    SourcePosition typePos = new SourcePosition();
    start(typePos);

    // match(Token.VOID);
    match(currentToken.kind);

    finish(typePos);
    // typeAST = new VoidType(typePos);

    switch (tokenType) {
      case Token.VOID:
        typeAST = new VoidType(typePos);
        break;
      case Token.BOOLEAN:
        typeAST = new BooleanType(typePos);
        break;
      case Token.INT:
        typeAST = new IntType(typePos);
        break;
      case Token.FLOAT:
        typeAST = new FloatType(typePos);
        break;
      // case Token.LBRACKET:
      //   match(Token.LBRACKET);
      //   // Parse ArrayType to create ArrayType node
      //   parseType();
      //   typeAST = new ArrayType(typeAST, exprAST, typePos);
      default:
        syntacticError("Type expected here", "");
        break;
    }

    return typeAST;
  }

  // for global/local variable declaration
  Type parseArrayType() throws SyntaxError {
    // identifier "[" INTLITERAL? "]"
    Type atAST = null; 
    Expr iAST = null;
    Type tAST = new IntType(dummyPos);

    SourcePosition arrayPos = new SourcePosition();
    start(arrayPos);

    match(Token.LBRACKET);
    if (currentToken.kind == Token.INTLITERAL) {
      SourcePosition intPos = new SourcePosition();
      start(intPos);
      IntLiteral ilAST = parseIntLiteral();
      finish(intPos);
      iAST = new IntExpr(ilAST, intPos);
    } else {
      iAST = new EmptyExpr(dummyPos);
    }
    match(Token.RBRACKET);
    
    finish(arrayPos);
    atAST = new ArrayType(tAST, iAST, arrayPos);
    return atAST;
  }
  

// ======================= STATEMENTS ==============================

  Stmt parseCompoundStmt() throws SyntaxError {
    Stmt cAST = null; 

    SourcePosition lvPos = new SourcePosition();
    start(lvPos);
    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    match(Token.LCURLY);

    // local-declarations
    List dlAST = parseLocalDecList();
    finish(lvPos);
    
    // stmt*
    // Insert code here to build a DeclList node for variable declarations
    List slAST = parseStmtList();
    match(Token.RCURLY);
    finish(stmtPos);

    /* In the subset of the VC grammar, no variable declarations are
     * allowed. Therefore, a block is empty iff it has no statements.
     */
    if (dlAST instanceof EmptyDeclList && slAST instanceof EmptyStmtList) 
      cAST = new EmptyCompStmt(stmtPos);
    else
      cAST = new CompoundStmt(dlAST, slAST, stmtPos);
    return cAST;
  }

  

  List parseStmtList() throws SyntaxError {
    List slAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind != Token.RCURLY) {
      Stmt sAST = parseStmt();
      {
        if (currentToken.kind != Token.RCURLY) {
          slAST = parseStmtList();
          finish(stmtPos);
          slAST = new StmtList(sAST, slAST, stmtPos);
        } else {
          finish(stmtPos);
          slAST = new StmtList(sAST, new EmptyStmtList(dummyPos), stmtPos);
        }
      }
    }
    else
      slAST = new EmptyStmtList(dummyPos);
    
    return slAST;
  }

  Stmt parseStmt() throws SyntaxError {
    Stmt sAST = null;

    switch (currentToken.kind) {
      case Token.LCURLY:
        sAST = parseCompoundStmt();
        break;
      case Token.IF:
        sAST = parseIfStmt();
        break;
      case Token.FOR:
        sAST = parseForStmt();
        break;
      case Token.WHILE:
        sAST = parseWhileStmt();
        break;
      case Token.BREAK:
        sAST = parseBreakStmt();
        break;
      case Token.CONTINUE:
        sAST = parseContinueStmt();
        break;
      case Token.RETURN:
        sAST = parseReturnStmt();
        break;
      default:
        sAST = parseExprStmt();
        break;
    }

    return sAST;
  }

  Stmt parseIfStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr eAST = null;
    Stmt s1AST = null;
    Stmt s2AST = null;

    SourcePosition ifPos = new SourcePosition();
    start(ifPos);

    match(Token.IF);
    match(Token.LPAREN);
    eAST = parseExpr();
    match(Token.RPAREN);
    s1AST = parseStmt();
    if (currentToken.kind == Token.ELSE) {
      match(Token.ELSE);
      s2AST = parseStmt();
    }

    finish(ifPos);
    if (s2AST == null) {
      sAST = new IfStmt(eAST, s1AST, ifPos);
    } else {
      sAST = new IfStmt(eAST, s1AST, s2AST, ifPos);
    }
    return sAST;
  }

  Stmt parseForStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr e1AST = null;
    Expr e2AST = null;
    Expr e3AST = null;
    Stmt s1AST = null;

    SourcePosition forPos = new SourcePosition();
    start(forPos);

    match(Token.FOR);
    match(Token.LPAREN);
    if (currentToken.kind != Token.SEMICOLON) {
      e1AST = parseExpr();
    }
    match(Token.SEMICOLON);
    if (currentToken.kind != Token.SEMICOLON) {
      e2AST = parseExpr();
    }
    match(Token.SEMICOLON);
    if (currentToken.kind != Token.RPAREN) {
      e3AST = parseExpr();
    }
    match(Token.RPAREN);
    s1AST = parseStmt();

    finish(forPos);
    sAST = new ForStmt(e1AST, e2AST, e3AST, s1AST, forPos);
    return sAST;
  }

  Stmt parseWhileStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr eAST = null;
    Stmt s1AST = null;

    SourcePosition whilePos = new SourcePosition();
    start(whilePos);

    match(Token.WHILE);
    match(Token.LPAREN);
    eAST = parseExpr();
    match(Token.RPAREN);
    s1AST = parseStmt();

    finish(whilePos);
    sAST = new WhileStmt(eAST, s1AST, whilePos);
    return sAST;
  }

  Stmt parseBreakStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition breakPos = new SourcePosition();
    start(breakPos);

    match(Token.BREAK);
    match(Token.SEMICOLON);

    finish(breakPos);
    sAST = new BreakStmt(breakPos);
    return sAST;
  }

  Stmt parseContinueStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition continuePos = new SourcePosition();
    start(continuePos);

    match(Token.CONTINUE);
    match(Token.SEMICOLON);

    finish(continuePos);
    sAST = new ContinueStmt(continuePos);
    return sAST;
  }

  Stmt parseReturnStmt() throws SyntaxError {
    Stmt sAST = null;
    Expr eAST = null;

    SourcePosition returnPos = new SourcePosition();
    start(returnPos);

    match(Token.RETURN);
    if (currentToken.kind != Token.SEMICOLON) {
      eAST = parseExpr();
    }
    match(Token.SEMICOLON);

    finish(returnPos);
    sAST = new ReturnStmt(eAST, returnPos);
    return sAST;
  }

  Stmt parseExprStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL
        || currentToken.kind == Token.LPAREN
        || currentToken.kind == Token.FLOATLITERAL
        || currentToken.kind == Token.BOOLEANLITERAL
        || currentToken.kind == Token.STRINGLITERAL
        // Include unary operators?
        || currentToken.kind == Token.PLUS
        || currentToken.kind == Token.MINUS
        || currentToken.kind == Token.NOT) {
        Expr eAST = parseExpr();
        match(Token.SEMICOLON);
        finish(stmtPos);
        sAST = new ExprStmt(eAST, stmtPos);
    } else {
      match(Token.SEMICOLON);
      finish(stmtPos);
      sAST = new ExprStmt(new EmptyExpr(dummyPos), stmtPos);
    }
    return sAST;
  }


// ======================= PARAMETERS =======================

  List parseParaList() throws SyntaxError {
    List plAST = null;
    Decl pAST = null;
    Type tAST = null;
    Ident iAST = null;

    SourcePosition paraListPos = new SourcePosition();
    start(paraListPos);

    // match(Token.LPAREN);
    // proper-para-list?
    
    
    if (typeChecker(currentToken.kind)) {
      // type declarator
      // => type identifier
      // | type identifier "[ INTLITERAL? ]" 
      tAST = parseType();
      iAST = parseIdent();
      pAST = parseParaDecl(tAST, iAST);
    }

    // Recursive call to parseParaList
    // If current token is not beginning of a compound statement
    if (currentToken.kind != Token.RPAREN) {
      plAST = parseParaList();
      finish(paraListPos);
      plAST = new ParaList((ParaDecl) pAST, plAST, paraListPos);
    } else {
      // Should expect beginning of compount statement
      match(Token.RPAREN);
      finish(paraListPos);
      // if pAST is null, create an empty ParaList node
      if (pAST != null) {
        plAST = new ParaList((ParaDecl) pAST, new EmptyParaList(dummyPos), paraListPos);
      } else {
      plAST = new EmptyParaList(paraListPos);
      }
    }
    
    // match(Token.RPAREN);
    if (plAST == null)
      plAST = new EmptyParaList(paraListPos);

    return plAST;
  }

  Decl parseParaDecl(Type tAST, Ident iAST) throws SyntaxError {
    Decl pAST = null;
    Type pType = null;
    Ident pIdent = iAST;

    SourcePosition paraPos = new SourcePosition();
    start(paraPos);

    // type identifier
    // | type identifier "[ INTLITERAL? ]" 
    if (currentToken.kind == Token.LBRACKET) {
      pType = parseArrayType();
    } else {
      pType = tAST;
    }

    // ( "," proper-para-list )*
    // ==> end of parameter declaration so create the node
    // ==> make new ParaList node with next parameter declaration as the child
    if (currentToken.kind == Token.COMMA) {
      accept(); 
      finish(paraPos);
      pAST = new ParaDecl(tAST, iAST, paraPos);
      // currentToken should be the identifier of the next parameter so go back to calling parseDecList
      return pAST; 
    }

    // Should reach end of last parameter here
    // match(Token.RPAREN);
    finish(paraPos);
    pAST = new ParaDecl(pType, pIdent, paraPos);
    
    return pAST;
  }

  List parseArgList() throws SyntaxError {
    List alAST = null;
    Arg aAST = null;
    // Expr eAST = null;

    SourcePosition argListPos = new SourcePosition();
    start(argListPos);

    // arg-list            -> "(" proper-arg-list? ")"
    if (currentToken.kind != Token.RPAREN) {
      // eAST = parseExpr();
      aAST = parseArg();
    }

    // Recursive call to parseArgList
    if (currentToken.kind != Token.RPAREN) {
      alAST = parseArgList();
      finish(argListPos);
      alAST = new ArgList(aAST, alAST, argListPos);
    } else {
      match(Token.RPAREN);
      finish(argListPos);
      if (aAST != null) {
        alAST = new ArgList(aAST, new EmptyArgList(dummyPos), argListPos);
      } else {
        alAST = new EmptyArgList(dummyPos);
      }
    }

    if (alAST == null)
      alAST = new EmptyArgList(dummyPos);

    return alAST;
  }

  Arg parseArg() throws SyntaxError {
    Arg aAST = null;
    Expr argExpr = parseExpr();

    SourcePosition argPos = new SourcePosition();
    start(argPos);

    // arg ( "," arg )*
    if (currentToken.kind == Token.COMMA) {
      accept();
      finish(argPos);
      aAST = new Arg(argExpr, argPos);
      return aAST;
    }

    // match(Token.RPAREN);
    // finish(argPos);
    // currentToken should be ')' so return the Arg node
    finishCurrent(argPos);
    aAST = new Arg(argExpr, argPos);
    return aAST;
  }


// ======================= EXPRESSIONS ======================

  Expr parseInitialiser() throws SyntaxError {
    Expr exprAST = null;
    // "{" expr ( "," expr )* "}"
    if (currentToken.kind == Token.LCURLY) {
      // exprAST is an ArrayInitExpr
      // ArrayInitExpr aiAST = null;
      SourcePosition aiePos = new SourcePosition();
      start(aiePos);
      accept(); // accept the "{"
      
      // currentToken should be an expression in the array
      // expr ( "," expr )*
      // List aelAST = null;
      // SourcePosition aelPos = new SourcePosition();
      // start(aelPos);
      
      List aelAST = parseArrayExprList();
      match(Token.RCURLY);
      finish(aiePos);
      exprAST = new ArrayInitExpr(aelAST, aiePos);

    } else { // expr
      exprAST = parseExpr();
    }
    return exprAST;
  }

  List parseArrayExprList() throws SyntaxError {
    List aelAST = null;
    // Expr exprAST = null;
    // Expr e1AST = null;
    // Expr e2AST = null;

    SourcePosition aelPos = new SourcePosition();
    start(aelPos);
    
    if (currentToken.kind != Token.RCURLY) {
      Expr eAST = parseExpr();
      if (currentToken.kind != Token.RCURLY) {
        // currentToken should be a comma
        match(Token.COMMA);
        // currentToken should be an expression in the array
        aelAST = parseArrayExprList();
        finish(aelPos);
        aelAST = new ArrayExprList(eAST, aelAST, aelPos);
      } else {
        // currentToken should be a right curly bracket
        finish(aelPos);
        aelAST = new ArrayExprList(eAST, new EmptyArrayExprList(dummyPos), aelPos);
      }
    } else {
      aelAST = new EmptyArrayExprList(dummyPos);
    }
    return aelAST;
  }

  Expr parseExpr() throws SyntaxError {
    Expr exprAST = null;
    exprAST = parseAssignExpr();
    return exprAST;
  }

  Expr parseAssignExpr() throws SyntaxError {
    Expr exprAST = null;
    Expr e1AST = null;
    // Expr e2AST = null;

    SourcePosition assignPos = new SourcePosition();
    start(assignPos);
    exprAST = parseCondOrExpr();
    
    if (currentToken.kind == Token.EQ) {
      Operator opAST = acceptOperator();
      e1AST = parseAssignExpr();
      finish(assignPos);
      exprAST = new AssignExpr(exprAST, e1AST, assignPos);
      // exprAST = new AssignExpr(exprAST, opAST, e2AST, assignPos);
    }
    return exprAST;
  }

  Expr parseCondOrExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition condOrPos = new SourcePosition();
    start(condOrPos);

    exprAST = parseCondAndExpr();
    while (currentToken.kind == Token.OROR) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseCondAndExpr();
      SourcePosition condOr2Pos = new SourcePosition();
      copyStart(condOrPos, condOr2Pos);
      finish(condOrPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, condOr2Pos);
    }
    return exprAST;
  }

  Expr parseCondAndExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition condAndPos = new SourcePosition();
    start(condAndPos);

    exprAST = parseEqualityExpr();
    while (currentToken.kind == Token.ANDAND) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseEqualityExpr();
      SourcePosition condAnd2Pos = new SourcePosition();
      copyStart(condAndPos, condAnd2Pos);
      finish(condAndPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, condAnd2Pos);
    }
    return exprAST;
  }

  Expr parseEqualityExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition eqPos = new SourcePosition();
    start(eqPos);

    exprAST = parseRelExpr();
    while (currentToken.kind == Token.EQEQ
           || currentToken.kind == Token.NOTEQ) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseRelExpr();
      SourcePosition eq2Pos = new SourcePosition();
      copyStart(eqPos, eq2Pos);
      finish(eqPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, eq2Pos);
    }
    return exprAST;
  }

  Expr parseRelExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition relPos = new SourcePosition();
    start(relPos);

    exprAST = parseAdditiveExpr();
    while (currentToken.kind == Token.LT
           || currentToken.kind == Token.LTEQ
           || currentToken.kind == Token.GT
           || currentToken.kind == Token.GTEQ) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseAdditiveExpr();
      SourcePosition rel2Pos = new SourcePosition();
      copyStart(relPos, rel2Pos);
      finish(relPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, rel2Pos);
    }
    return exprAST;
  }

  Expr parseAdditiveExpr() throws SyntaxError {
    Expr exprAST = null;

    SourcePosition addStartPos = new SourcePosition();
    start(addStartPos);

    exprAST = parseMultiplicativeExpr();
    while (currentToken.kind == Token.PLUS
           || currentToken.kind == Token.MINUS) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseMultiplicativeExpr();

      SourcePosition addPos = new SourcePosition();
      copyStart(addStartPos, addPos);
      finish(addPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, addPos);
    }
    return exprAST;
  }

  Expr parseMultiplicativeExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition multStartPos = new SourcePosition();
    start(multStartPos);

    exprAST = parseUnaryExpr();
    while (currentToken.kind == Token.MULT
           || currentToken.kind == Token.DIV) {
      Operator opAST = acceptOperator();
      Expr e2AST = parseUnaryExpr();
      SourcePosition multPos = new SourcePosition();
      copyStart(multStartPos, multPos);
      finish(multPos);
      exprAST = new BinaryExpr(exprAST, opAST, e2AST, multPos);
    }
    return exprAST;
  }

  Expr parseUnaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition unaryPos = new SourcePosition();
    start(unaryPos);

    switch (currentToken.kind) {
      case Token.PLUS, Token.MINUS, Token.NOT:
        {
          Operator opAST = acceptOperator();
          Expr e2AST = parseUnaryExpr();
          finish(unaryPos);
          exprAST = new UnaryExpr(opAST, e2AST, unaryPos);
        }
        break;

      default:
        exprAST = parsePrimaryExpr();
        break;
       
    }
    return exprAST;
  }

  Expr parsePrimaryExpr() throws SyntaxError {

    Expr exprAST = null;

    SourcePosition primPos = new SourcePosition();
    start(primPos);

    switch (currentToken.kind) {

      case Token.ID:
        Ident iAST = parseIdent();
        // Function call
        if (currentToken.kind == Token.LPAREN) {
          // "(" proper-arg-list? ")"
          // accept();
          accept();
          List alAST = parseArgList(); 
          finish(primPos);
          exprAST = new CallExpr(iAST, alAST, primPos);
        // Array expression
        } else if (currentToken.kind == Token.LBRACKET) {
          accept();
          Var simVAST = new SimpleVar(iAST, primPos);
          Expr eAST = parseExpr();
          match(Token.RBRACKET);
          finish(primPos);
          exprAST = new ArrayExpr(simVAST, eAST, primPos);
        // Variable
        } else {
          finish(primPos);
          Var simVAST = new SimpleVar(iAST, primPos);
          exprAST = new VarExpr(simVAST, primPos);
        }
        break;

      case Token.LPAREN:
        {
          accept();
          exprAST = parseExpr();
	        match(Token.RPAREN);
        }
        break;

      case Token.INTLITERAL:
        IntLiteral ilAST = parseIntLiteral();
        finish(primPos);
        exprAST = new IntExpr(ilAST, primPos);
        break;

      case Token.FLOATLITERAL:
        FloatLiteral flAST = parseFloatLiteral();
        finish(primPos);
        exprAST = new FloatExpr(flAST, primPos);
        break;

      case Token.BOOLEANLITERAL:
        BooleanLiteral blAST = parseBooleanLiteral();
        finish(primPos);
        exprAST = new BooleanExpr(blAST, primPos);
        break;

      case Token.STRINGLITERAL:
        StringLiteral slAST = parseStringLiteral();
        finish(primPos);
        exprAST = new StringExpr(slAST, primPos);
        break;

      default:
        syntacticError("illegal primary expression", currentToken.spelling);
       
    }
    return exprAST;
  }

  // Expr parseIntExpr() {
  //   Expr iAST = null;
  //   IntLiteral iLit = null;
  //   iAST =
  //   return iAST()
  // }

// ========================== ID, OPERATOR and LITERALS ========================

  Ident parseIdent() throws SyntaxError {

    Ident I = null; 

    if (currentToken.kind == Token.ID) {
      previousTokenPosition = currentToken.position;
      String spelling = currentToken.spelling;
      I = new Ident(spelling, previousTokenPosition);
      currentToken = scanner.getToken();
    } else 
      syntacticError("identifier expected here", "");
    return I;
  }

// acceptOperator parses an operator, and constructs a leaf AST for it

  Operator acceptOperator() throws SyntaxError {
    Operator O = null;

    previousTokenPosition = currentToken.position;
    String spelling = currentToken.spelling;
    O = new Operator(spelling, previousTokenPosition);
    currentToken = scanner.getToken();
    return O;
  }


  IntLiteral parseIntLiteral() throws SyntaxError {
    IntLiteral IL = null;

    if (currentToken.kind == Token.INTLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      IL = new IntLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("integer literal expected here", "");
    return IL;
  }

  FloatLiteral parseFloatLiteral() throws SyntaxError {
    FloatLiteral FL = null;

    if (currentToken.kind == Token.FLOATLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      FL = new FloatLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("float literal expected here", "");
    return FL;
  }

  BooleanLiteral parseBooleanLiteral() throws SyntaxError {
    BooleanLiteral BL = null;

    if (currentToken.kind == Token.BOOLEANLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      BL = new BooleanLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("boolean literal expected here", "");
    return BL;
  }

  StringLiteral parseStringLiteral() throws SyntaxError {
    StringLiteral SL = null;

    if (currentToken.kind == Token.STRINGLITERAL) {
      String spelling = currentToken.spelling;
      accept();
      SL = new StringLiteral(spelling, previousTokenPosition);
    } else 
      syntacticError("string literal expected here", "");
    return SL;
  }

}

