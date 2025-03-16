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
    Type tAST;
    Ident iAST;
    // typechecker
    if (typeChecker(currentToken.kind)) {
      tAST = parseType();
      iAST = parseIdent();
      if (currentToken.kind == Token.LPAREN) {
        dAST = parseFuncDecl(tAST, iAST);
      } else {
        dAST = parseGlobalVar(tAST, iAST);
      }
    }
    
    // Recursive call to parseDecList
    if (typeChecker(currentToken.kind)) {
      dlAST = parseDecList();
      finish(decListPos);
      dlAST = new DeclList(dAST, dlAST, decListPos);
    } else {
      finish(decListPos);
      dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), decListPos);
    }
    
    // if (currentToken.kind == Token.VOID) {
    //   dlAST = parseFuncDeclList();
    //   finish(funcPos);
    //   dlAST = new DeclList(dAST, dlAST, funcPos);
    // } else {
    //   finish(funcPos);
    //   dlAST = new DeclList(dAST, new EmptyDeclList(dummyPos), funcPos);
    // }
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

  Decl parseGlobalVar(Type tAST, Ident iAST) throws SyntaxError{
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
      gvType = parseArrayType(tAST);
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
      gvAST = new GlobalVarDecl(tAST, iAST, gvExpr, gvPos);
      // currentToken should be an identifier so go back to calling parseDecList
      return gvAST; 
    } 

    // Should reach end of last global variable here
    match(Token.SEMICOLON);
    finish(gvPos);
    gvAST = new GlobalVarDecl(gvType, gvIdent, gvExpr, gvPos);
    return gvAST;
    // switch (currentToken.kind) {
    //   // int x;
    //   case Token.SEMICOLON: // identifier ";"
    //     accept();
    //     finish(gvPos);
    //     gvExpr = new EmptyExpr(dummyPos);
    //     gvAST = new GlobalVarDecl(tAST, gvIdent, gvExpr, gvPos);
    //     break;
    //   // int x[5];
    //   case Token.LBRACKET: // identifier "[" INTLITERAL? "]"

    //     gvType = parseArrayType(tAST);
    //     finish(gvPos);
    //     gvAST = new GlobalVarDecl(gvType, gvIdent, gvExpr, gvPos);
    //     break;
    //   // int x = 5;
    //   case Token.EQ: // identifier ( "=" initialiser )?
    //     Operator eqOp = acceptOperator();
    //     gvExpr = parseInitialiser();
    //     finish(gvPos);
    //     gvAST = new GlobalVarDecl(tAST, gvIdent, gvExpr, gvPos);
    //     break;

    //   default:
    //     break;
    // }

    // finish(gvPos);
    // return gvAST;
  }

  Decl parseLocalVar(Type tAST, Ident iAST) throws SyntaxError {
    Decl lvAST = null;
    

    lvAST = new LocalVarDecl(tAST, iAST, null, previousTokenPosition);
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
  Type parseArrayType(Type tAST) throws SyntaxError {
    // identifier "[" INTLITERAL? "]"
    Type atAST = null; 
    Expr iAST = null;

    SourcePosition arrayPos = new SourcePosition();
    start(arrayPos);

    match(Token.LBRACKET);
    if (currentToken.kind == Token.INTLITERAL) {
      SourcePosition intPos = new SourcePosition();
      start(intPos);
      IntLiteral ilAST = parseIntLiteral();
      finish(intPos);
      iAST = new IntExpr(ilAST, intPos);
    }
    match(Token.RBRACKET);
    
    finish(arrayPos);
    atAST = new ArrayType(tAST, iAST, arrayPos);
    return atAST;
  }
  

// ======================= STATEMENTS ==============================

  Stmt parseCompoundStmt() throws SyntaxError {
    Stmt cAST = null; 

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    match(Token.LCURLY);

    // Insert code here to build a DeclList node for variable declarations
    List slAST = parseStmtList();
    match(Token.RCURLY);
    finish(stmtPos);

    /* In the subset of the VC grammar, no variable declarations are
     * allowed. Therefore, a block is empty iff it has no statements.
     */
    if (slAST instanceof EmptyStmtList) 
      cAST = new EmptyCompStmt(stmtPos);
    else
      cAST = new CompoundStmt(new EmptyDeclList(dummyPos), slAST, stmtPos);
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

    sAST = parseExprStmt();

    return sAST;
  }

  Stmt parseExprStmt() throws SyntaxError {
    Stmt sAST = null;

    SourcePosition stmtPos = new SourcePosition();
    start(stmtPos);

    if (currentToken.kind == Token.ID
        || currentToken.kind == Token.INTLITERAL
        || currentToken.kind == Token.LPAREN) {
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
    if (currentToken.kind != Token.RCURLY) {
      plAST = parseParaList();
      finish(paraListPos);
      plAST = new ParaList(pAST, plAST, paraListPos);
    } else {
      finish(paraListPos);
      plAST = new ParaList(pAST, new EmptyParaList(dummyPos), paraListPos);
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
      pType = parseArrayType(tAST);
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
    match(Token.RPAREN);
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
      accept();
      finish(argListPos);
      alAST = new ArgList(aAST, new EmptyArgList(dummyPos), argListPos);
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
      accept();
      exprAST = parseExpr();
      while (currentToken.kind == Token.COMMA) {
        accept();
        parseExpr();
      }
      match(Token.RCURLY);
    } else { // expr
      exprAST = parseExpr();
    }
    return exprAST;
  }

  Expr parseExpr() throws SyntaxError {
    Expr exprAST = null;
    exprAST = parseAssignExpr();
    return exprAST;
  }

  Expr parseAssignExpr() throws SyntaxError {
    Expr exprAST = null;
    Expr e1AST = null;
    Expr e2AST = null;

    SourcePosition assignPos = new SourcePosition();
    start(assignPos);
    e1AST = parseCondOrExpr();
    
    if (currentToken.kind == Token.EQ) {
      Operator opAST = acceptOperator();
      e2AST = parseAssignExpr();
      finish(assignPos);
      exprAST = new AssignExpr(e1AST, e2AST, assignPos);
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
        if (currentToken.kind == Token.LPAREN) {
          // "(" proper-arg-list? ")"
          // accept();
          accept();
          List alAST = parseArgList(); 
          // finish(primPos);
          // exprAST = new CallExpr(iAST, aplAST, primPos);
        } else if (currentToken.kind == Token.LBRACKET) {
          accept();
          Expr eAST = parseExpr();
          match(Token.RBRACKET);
          finish(primPos);
          // exprAST = new ArrayExpr(iAST, eAST, primPos);
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

