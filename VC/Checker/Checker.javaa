/*
 * Checker.java
 *
 * This VC compiler pass is responsible for performing semantic analysis 
 * on the abstract syntax tree (AST) of a VC program. It checks for scope and 
 * type rules, decorates the AST with type information, and links identifiers 
 * to their declarations.
 *
 * Sun 09 Mar 2025 08:44:27 AEDT
 *
 */

package VC.Checker;

import VC.ASTs.*;
import VC.Scanner.SourcePosition;
import VC.ErrorReporter;
import VC.StdEnvironment;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.Optional;

public final class Checker implements Visitor {
    private boolean mainFunctionDeclared = false; // Flag to check if main function is present
    private int loopCounter = 0;

    // Enum for error messages
    private enum ErrorMessage {
        MISSING_MAIN("*0: main function is missing"),

        // Defined occurrences of identifiers (global, local, and parameters)
        MAIN_RETURN_TYPE_NOT_INT("*1: return type of main is not int"),
        IDENTIFIER_REDECLARED("*2: identifier redeclared"),
        IDENTIFIER_DECLARED_VOID("*3: identifier declared void"),
        IDENTIFIER_DECLARED_VOID_ARRAY("*4: identifier declared void[]"),


        // applied occurrences of identifiers
        IDENTIFIER_UNDECLARED("*5: identifier undeclared"),
	
        // assignments
        INCOMPATIBLE_TYPE_FOR_ASSIGNMENT("*6: incompatible type for ="),
        INVALID_LVALUE_IN_ASSIGNMENT("*7: invalid lvalue in assignment"),


        // types for expressions 
        INCOMPATIBLE_TYPE_FOR_RETURN("*8: incompatible type for return"),
        INCOMPATIBLE_TYPE_FOR_BINARY_OPERATOR("*9: incompatible type for this binary operator"),
        INCOMPATIBLE_TYPE_FOR_UNARY_OPERATOR("*10: incompatible type for this unary operator"),


	// scalars
        ARRAY_FUNCTION_AS_SCALAR("*11: attempt to use an array/function as a scalar"),

	// arrays
        SCALAR_FUNCTION_AS_ARRAY("*12: attempt to use a scalar/function as an array"), 
        WRONG_TYPE_FOR_ARRAY_INITIALISER("*13: wrong type for element in array initialiser"),
        INVALID_INITIALISER_ARRAY_FOR_SCALAR("*14: invalid initialiser: array initialiser for scalar"),
        INVALID_INITIALISER_SCALAR_FOR_ARRAY("*15: invalid initialiser: scalar initialiser for array"),
        EXCESS_ELEMENTS_IN_ARRAY_INITIALISER("*16: excess elements in array initialiser"),
        ARRAY_SUBSCRIPT_NOT_INTEGER("*17: array subscript is not an integer"),
        ARRAY_SIZE_MISSING("*18: array size missing"), // e.g. int a[];

	// functions
        SCALAR_ARRAY_AS_FUNCTION("*19: attempt to reference a scalar/array as a function"),

        // conditional expressions in if, for and while
        IF_CONDITIONAL_NOT_BOOLEAN("*20: if conditional is not boolean"),
        FOR_CONDITIONAL_NOT_BOOLEAN("*21: for conditional is not boolean"),
        WHILE_CONDITIONAL_NOT_BOOLEAN("*22: while conditional is not boolean"),

        // break and continue
        BREAK_NOT_IN_LOOP("*23: break must be in a while/for"),
        CONTINUE_NOT_IN_LOOP("*24: continue must be in a while/for"),

	// parameters
        TOO_MANY_ACTUAL_PARAMETERS("*25: too many actual parameters"),
        TOO_FEW_ACTUAL_PARAMETERS("*26: too few actual parameters"),
        WRONG_TYPE_FOR_ACTUAL_PARAMETER("*27: wrong type for actual parameter"),

        // reserved for errors that I may have missed (J. Xue)
        MISC_1("*28: misc 1"),
        MISC_2("*29: misc 2"),


        // the following two checks are optional 
        STATEMENTS_NOT_REACHED("*30: statement(s) not reached"),
        MISSING_RETURN_STATEMENT("*31: missing return statement");

        private final String message;

        ErrorMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private final SymbolTable idTable;
    private static final SourcePosition dummyPos = new SourcePosition();
    private final ErrorReporter reporter;

    public Checker(ErrorReporter reporter) {
        this.reporter = Objects.requireNonNull(reporter, "ErrorReporter must not be null");
        this.idTable = new SymbolTable();
        establishStdEnvironment();
    }

    /* Auxiliary Methods */

     /* 
      * Declares a variable in the symbol table and checks for redeclaration errors.
      */

    private void declareVariable(Ident ident, Decl decl) {
        idTable.retrieveOneLevel(ident.spelling).ifPresent(entry -> 
            reporter.reportError(ErrorMessage.IDENTIFIER_REDECLARED.getMessage(), ident.spelling, ident.position)
        );
        idTable.insert(ident.spelling, decl);
        ident.visit(this, null);
    }

    // Your other auxilary methods

    public void check(AST ast) {
        ast.visit(this, null);
    }

    public int countElementsOfArrayInitialiser(List ast) {
        if (ast.isEmptyArrayExprList()) {
            return 0;
        } else {
            return 1 + countElementsOfArrayInitialiser(((ArrayExprList) ast).EL);
        }
    }

    public int countArgs(List argList) {
        if (argList.isEmptyArgList()) {
            return 0;
        } else {
            return 1 + countArgs(((ArgList) argList).AL);
        }
    }

    public int countParas(List paraList) {
        if (paraList.isEmptyParaList()) {
            return 0;
        } else {
            return 1 + countParas(((ParaList) paraList).PL);
        }
    }

    
    // Programs

    @Override
    public Object visitProgram(Program ast, Object o) {
        ast.FL.visit(this, null);
        if (!mainFunctionDeclared) {
            reporter.reportError(ErrorMessage.MISSING_MAIN.getMessage(), "", ast.position);
            return StdEnvironment.errorType;
        }
        return null;
    }

    // Statements

    @Override
    public Object visitCompoundStmt(CompoundStmt ast, Object o) {
        idTable.openScope();
        ((FuncDecl) o).PL.visit(this, null);
	// Your code goes here

        ast.DL.visit(this, o); // Visit the declaration list
        ast.SL.visit(this, o); // Visit the statement list
        idTable.closeScope();
        return null;
    }

    @Override
    public Object visitStmtList(StmtList ast, Object o) {
        ast.S.visit(this, o);
        if (ast.S instanceof ReturnStmt && ast.SL instanceof StmtList) {
            reporter.reportError(ErrorMessage.STATEMENTS_NOT_REACHED.getMessage(), "", ast.SL.position);
        }
        ast.SL.visit(this, o);
        return null;
    }

    @Override
    public Object visitExprStmt(ExprStmt ast, Object o) {
        ast.E.visit(this, o);
        return null;
    }

    @Override
    public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
        return null;
    }

    @Override
    public Object visitAssignExpr(AssignExpr ast, Object o) {
        Expr lhs = (Expr) ast.E1;
        Expr rhs = (Expr) ast.E2;
        Type tAST1 = (Type) ast.E1.visit(this, o);
        Type tAST2 = (Type) ast.E2.visit(this, o);
        if (tAST1.assignable(tAST2)) {
            ast.type = tAST1;

        } else {
            reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_ASSIGNMENT.getMessage(), "", ast.position);
            ast.type = StdEnvironment.errorType;
            return StdEnvironment.errorType;
        }
        if (lhs instanceof VarExpr) {
            Decl identDecl = (Decl) ((Ident) ((SimpleVar) ((VarExpr) lhs).V).I).visit(this, o);
            if (identDecl == null) {
                reporter.reportError(ErrorMessage.IDENTIFIER_UNDECLARED.getMessage(), ((Ident) ((SimpleVar) ((VarExpr) lhs).V).I).spelling, ((VarExpr) lhs).V.position);
                ast.type = StdEnvironment.errorType;
                return StdEnvironment.errorType;
            } else {
                if (identDecl instanceof FuncDecl) {
                    reporter.reportError(ErrorMessage.INVALID_LVALUE_IN_ASSIGNMENT.getMessage(), ((Ident) ((SimpleVar) ((VarExpr) lhs).V).I).spelling, ((VarExpr) lhs).V.position);
                    ast.type = StdEnvironment.errorType;
                    return StdEnvironment.errorType;
                } else if (identDecl.T.isArrayType()) {
                    reporter.reportError(ErrorMessage.INVALID_LVALUE_IN_ASSIGNMENT.getMessage(), ((Ident) ((SimpleVar) ((VarExpr) lhs).V).I).spelling, ((VarExpr) lhs).V.position);
                    ast.type = StdEnvironment.errorType;
                    return StdEnvironment.errorType;
                }
            }
        } else if (lhs instanceof ArrayExpr) {
            Decl identDecl = (Decl) ((Ident) ((SimpleVar) ((ArrayExpr) lhs).V).I).visit(this, o);
            if (identDecl == null) {
                reporter.reportError(ErrorMessage.IDENTIFIER_UNDECLARED.getMessage(), ((Ident) ((SimpleVar) ((ArrayExpr) lhs).V).I).spelling, ((ArrayExpr) lhs).V.position);
                ast.type = StdEnvironment.errorType;
                return StdEnvironment.errorType;
            } else {
                if (identDecl instanceof FuncDecl) {
                    reporter.reportError(ErrorMessage.INVALID_LVALUE_IN_ASSIGNMENT.getMessage(), ((Ident) ((SimpleVar) ((ArrayExpr) lhs).V).I).spelling, ((ArrayExpr) lhs).V.position);
                    ast.type = StdEnvironment.errorType;
                    return StdEnvironment.errorType;
                }
            }
        } else {
            reporter.reportError(ErrorMessage.INVALID_LVALUE_IN_ASSIGNMENT.getMessage(), "", ast.position);
            ast.type = StdEnvironment.errorType;
            return StdEnvironment.errorType;
        }
        return ast.type;
    }

    @Override
    public Object visitSimpleVar(SimpleVar ast, Object o) {
        Decl identDecl = (Decl) ast.I.visit(this, o);
        

        if (identDecl == null) {
            reporter.reportError(ErrorMessage.IDENTIFIER_UNDECLARED.getMessage(), ast.I.spelling, ast.I.position);
            return StdEnvironment.errorType;
        } else {
            ast.type = (Type) identDecl.T;
            if (identDecl instanceof FuncDecl) {
                reporter.reportError(ErrorMessage.ARRAY_FUNCTION_AS_SCALAR.getMessage(), ast.I.spelling, ast.I.position);
            } else {
                if (o instanceof ArrayExpr && !(ast.type.isArrayType())) {
                    reporter.reportError(ErrorMessage.SCALAR_FUNCTION_AS_ARRAY.getMessage(), ast.I.spelling, ast.I.position);
                } else if (o instanceof VarExpr && ast.type.isArrayType() && !(((Expr)o).parent instanceof Arg)) {
                    reporter.reportError(ErrorMessage.ARRAY_FUNCTION_AS_SCALAR.getMessage(), ast.I.spelling, ast.I.position);
                }
            }
        }
        return ast.type;
    }


    @Override
    public Object visitEmptyStmt(EmptyStmt ast, Object o) {
        return null;
    }

    @Override
    public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
        return null;
    }

    @Override
    public Object visitReturnStmt(ReturnStmt ast, Object o) {
        Expr returnExpr = (Expr) ast.E;
        String funcName = ((FuncDecl) o).I.spelling;
        if (returnExpr.isEmptyExpr()) {
            if (o instanceof FuncDecl) {
                FuncDecl funcDecl = (FuncDecl) o;
                if (!(funcDecl.T.isVoidType())) {
                    reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_RETURN.getMessage(), "", ast.position);
                }
            }
        } else {
            Type returnType = (Type) returnExpr.visit(this, o);
            if (o instanceof FuncDecl) {
                FuncDecl funcDecl = (FuncDecl) o;
                if (!(funcDecl.T.isVoidType()) && !(funcDecl.T.assignable(returnType))) {
                    reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_RETURN.getMessage(), "", ast.position);
                }
            }
        }
        return null;
    }

    @Override
    public Object visitIfStmt(IfStmt ast, Object o) {
        Type condType = (Type) ast.E.visit(this, o);
        if (!condType.isBooleanType()) {
            reporter.reportError(ErrorMessage.IF_CONDITIONAL_NOT_BOOLEAN.getMessage(), "", ast.position);
        }
        ast.S1.visit(this, o);
        ast.S2.visit(this, o);
        return null;
    }

    @Override
    public Object visitWhileStmt(WhileStmt ast, Object o) {
        
        Type condType = (Type) ast.E.visit(this, o);
        if (!condType.isBooleanType()) {
            reporter.reportError(ErrorMessage.WHILE_CONDITIONAL_NOT_BOOLEAN.getMessage(), "", ast.position);
        }
        loopCounter++;
        ast.S.visit(this, o);
        loopCounter--;
        return null;
    }

    @Override
    public Object visitForStmt(ForStmt ast, Object o) {
        ast.E1.visit(this, o);
        ast.E3.visit(this, o);
        Type condType2 = (Type) ast.E2.visit(this, o);
        if (!ast.E2.isEmptyExpr() && !condType2.isBooleanType()) {
            reporter.reportError(ErrorMessage.FOR_CONDITIONAL_NOT_BOOLEAN.getMessage(), "", ast.position);

        }
        loopCounter++;
        ast.S.visit(this, o);
        loopCounter--;
        return null;
    }

    @Override
    public Object visitBreakStmt(BreakStmt ast, Object o) {
        if (loopCounter <= 0) {
            reporter.reportError(ErrorMessage.BREAK_NOT_IN_LOOP.getMessage(), "", ast.position);
        }
        return null;
    }

    @Override
    public Object visitContinueStmt(ContinueStmt ast, Object o) {
        if (loopCounter == 0) {
            reporter.reportError(ErrorMessage.CONTINUE_NOT_IN_LOOP.getMessage(), "", ast.position);
            return StdEnvironment.errorType;
        }
        return null;
    }

    // Expressions
    @Override
    public Object visitUnaryExpr(UnaryExpr ast, Object o ) {
        Type tAST = (Type) ast.E.visit(this, o);
        String opString = ((Operator) ast.O).spelling;

        if (opString.equals("!")) {
            if (!tAST.isBooleanType()) {
                reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_UNARY_OPERATOR.getMessage(), opString, ast.position);
                ast.type = StdEnvironment.errorType;
            } else {
                ast.type = StdEnvironment.booleanType;
            }
        } else if (opString.equals("+") || opString.equals("-")) {
            if (tAST.isIntType()) {
                ast.type = StdEnvironment.intType;
            } else if (tAST.isFloatType()) {
                ast.type = StdEnvironment.floatType;
            } else {
                reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_UNARY_OPERATOR.getMessage(), opString, ast.position);
                ast.type = StdEnvironment.errorType;
            }
        }
        return ast.type;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr ast, Object o) {
        Type tAST1 = (Type) ast.E1.visit(this, o);
        String opString = ((Operator) ast.O).spelling;
        Type tAST2 = (Type) ast.E2.visit(this, o);
        switch (opString) {
            case "+", "-", "*", "/" -> {
                if (tAST1.isIntType() && tAST2.isIntType()) {
                    ast.type = StdEnvironment.intType;
                } else if (tAST1.isFloatType() && tAST2.isFloatType()) {
                    ast.type = StdEnvironment.floatType;
                } else if (tAST1.isIntType() && tAST2.isFloatType()) {
                    Operator op = new Operator("i2f", dummyPos);
                    UnaryExpr unaryNode = new UnaryExpr(op, ast.E1, dummyPos);
                    ast.E1 = unaryNode;
                    ast.type = StdEnvironment.floatType;
                } else if (tAST1.isFloatType() && tAST2.isIntType()) {
                    Operator op = new Operator("i2f", dummyPos);
                    UnaryExpr unaryNode = new UnaryExpr(op, ast, dummyPos);
                    ast.E2 = unaryNode;
                    ast.type = StdEnvironment.floatType;
                } else {
                    if (!tAST1.isErrorType() && !tAST2.isErrorType()) {
                        reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_BINARY_OPERATOR.getMessage(), opString, ast.position);
                    }
                    ast.type = StdEnvironment.errorType;
                }
            }
            case "<", "<=", ">", ">=" -> {
                if (tAST1.isIntType() && tAST2.isIntType()) {
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isFloatType() && tAST2.isFloatType()) {
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isIntType() && tAST2.isFloatType()) {
                    Operator op = new Operator("i2f", dummyPos);
                    UnaryExpr unaryNode = new UnaryExpr(op, ast.E1, dummyPos);
                    ast.E1 = unaryNode;
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isFloatType() && tAST2.isIntType()) {
                    Operator op = new Operator("i2f", dummyPos);
                    UnaryExpr unaryNode = new UnaryExpr(op, ast, dummyPos);
                    ast.E2 = unaryNode;
                    ast.type = StdEnvironment.booleanType;
                } else {
                    if (!tAST1.isErrorType() && !tAST2.isErrorType()){
                        reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_BINARY_OPERATOR.getMessage(), opString, ast.position);
                    }
                    ast.type = StdEnvironment.errorType;
                }
            }
            case "==", "!=" -> {
                if (tAST1.isIntType() && tAST2.isIntType()) {
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isFloatType() && tAST2.isFloatType()) {
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isStringType() && tAST2.isStringType()) {
                    ast.type = StdEnvironment.booleanType;
                } else if (tAST1.isBooleanType() && tAST2.isBooleanType()) {
                    ast.type = StdEnvironment.booleanType;
                } else {
                    if (!tAST1.isErrorType() && !tAST2.isErrorType()) {
                        reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_BINARY_OPERATOR.getMessage(), opString, ast.position);
                    }
                    ast.type = StdEnvironment.errorType;
                }
            }
            case "&&", "||" -> {
                if (tAST1.isBooleanType() && tAST2.isBooleanType()) {
                    ast.type = StdEnvironment.booleanType;
                } else {
                    if (!tAST1.isErrorType() && !tAST2.isErrorType()) {
                        reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_BINARY_OPERATOR.getMessage(), opString, ast.position);
                    }
                    ast.type = StdEnvironment.errorType;
                }
            }
        }
        return ast.type;
    }

    @Override
    public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
        // Need to check if left child of var dec node is array type
        Decl decl = (Decl) o;
        Type typeDecl = (Type) decl.T;
       
        if (!typeDecl.isArrayType()) {
            reporter.reportError(ErrorMessage.INVALID_INITIALISER_ARRAY_FOR_SCALAR.getMessage(), "", ast.position);
            return StdEnvironment.errorType;
        } else {
            ast.IL.visit(this,o);
            int numElements = countElementsOfArrayInitialiser(ast.IL);
            Integer arraySize = Integer.parseInt(((IntExpr) ((ArrayType)typeDecl).E).IL.spelling);
            // if (numElements > arraySize) {
            //     reporter.reportError(ErrorMessage.EXCESS_ELEMENTS_IN_ARRAY_INITIALISER.getMessage(), "", ast.position);
            // } 
        }

        
        return null;
    }

    @Override
    public Object visitArrayExprList(ArrayExprList ast, Object o) {
        Decl decl = (Decl) o;
        Type typeDecl = (Type) decl.T;
        Type eAST = (Type) ast.E.visit(this, o); // Type of expression in the array
       
        Type arrType = ((ArrayType) typeDecl).T;
        if(!arrType.assignable(eAST)){
            reporter.reportError(ErrorMessage.WRONG_TYPE_FOR_ARRAY_INITIALISER.getMessage(), "", ast.position);
        }
    
           
        ast.EL.visit(this, o);
        
        return null;
    }

    @Override
    public Object visitArrayExpr(ArrayExpr ast, Object o) {
        // Need to check if type of var child is of array type that had been declared
        Type varType = (Type) ast.V.visit(this, ast);
        Type indexType = (Type) ast.E.visit(this, null);
        if (!(indexType.isIntType())) {
            reporter.reportError(ErrorMessage.ARRAY_SUBSCRIPT_NOT_INTEGER.getMessage(), null, dummyPos);
            // return StdEnvironment.errorType;
        }
        // Also check if index is <= size of declared array

        return ast.type;
    }

    @Override 
    public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
        return null;
    }

    @Override
    public Object visitCallExpr(CallExpr ast, Object o) {
        Ident cIdent = (Ident) ast.I;
        List cArgList = (List) ast.AL;
        Decl funcDecl = (Decl) ast.I.visit(this, o); // pointer to the function declaration
        Type callType = null;        

        if (funcDecl == null) {
            reporter.reportError(ErrorMessage.IDENTIFIER_UNDECLARED.getMessage(), ast.I.spelling, ast.I.position);
            return StdEnvironment.errorType;
        } else if (!funcDecl.isFuncDecl()) {
            reporter.reportError(ErrorMessage.SCALAR_ARRAY_AS_FUNCTION.getMessage(), ast.I.spelling, ast.I.position);
            return StdEnvironment.errorType;
        } 

        // Count the number of arguments
        int argCount = countArgs(cArgList);
        int paraCount = countParas(((FuncDecl) funcDecl).PL);

        // funcDecl is a function declaration 
        List funcParaList = (List)((FuncDecl) funcDecl).PL;
        while (!funcParaList.isEmptyParaList() && !cArgList.isEmptyArgList()) {
            Type funcParaType = (Type) ((ParaList)funcParaList).P.T.visit(this, null);
            Type argType = (Type) ((ArgList) cArgList).A.E.visit(this, null);

            if (!funcParaType.isArrayType() && !argType.isArrayType()) {
                if (!funcParaType.assignable(argType)) {
                    reporter.reportError(ErrorMessage.WRONG_TYPE_FOR_ACTUAL_PARAMETER.getMessage(), cIdent.spelling, ast.position);
                    callType = StdEnvironment.errorType;
                }
                //do potential int float cast here
                if (funcParaType.isFloatType() && argType.isIntType()) {
                    UnaryExpr unaryNode = new UnaryExpr(new Operator("i2f", dummyPos), ((ArgList) cArgList).A, dummyPos);
                    ((ArgList) cArgList).A.E = unaryNode;
                }
            } else if (funcParaType.isArrayType() && !argType.isArrayType()) {
                reporter.reportError(ErrorMessage.WRONG_TYPE_FOR_ACTUAL_PARAMETER.getMessage(), cIdent.spelling, ast.position);
                callType = StdEnvironment.errorType;
            } else if (!funcParaType.isArrayType() && argType.isArrayType()) {
                reporter.reportError(ErrorMessage.WRONG_TYPE_FOR_ACTUAL_PARAMETER.getMessage(), cIdent.spelling, ast.position);
                callType = StdEnvironment.errorType;
            } else {
                Type funcParaArrayType = (Type) ((ArrayType)funcParaType).T.visit(this, null);
                Type argArrayType = (Type) ((ArrayType)argType).T.visit(this, null);
                if (!funcParaArrayType.assignable(argArrayType)) {
                    reporter.reportError(ErrorMessage.WRONG_TYPE_FOR_ACTUAL_PARAMETER.getMessage(), cIdent.spelling, ast.position);
                    callType = StdEnvironment.errorType;
                }
            }
            funcParaList = ((ParaList) funcParaList).PL;
            cArgList = ((ArgList) cArgList).AL;
        }

        if (argCount > paraCount) {
            reporter.reportError(ErrorMessage.TOO_MANY_ACTUAL_PARAMETERS.getMessage(), cIdent.spelling, ast.position);
            callType = StdEnvironment.errorType;
        } else if (argCount < paraCount) {
            reporter.reportError(ErrorMessage.TOO_FEW_ACTUAL_PARAMETERS.getMessage(), cIdent.spelling, ast.position);
            callType = StdEnvironment.errorType;
        }

        
        Type funcReturnType = (Type) ((FuncDecl) funcDecl).T.visit(this, null);
        return callType != null ? callType : funcReturnType;
    }

    @Override
    public Object visitEmptyExpr(EmptyExpr ast, Object o) {
        ast.type = (ast.parent instanceof ReturnStmt) ? StdEnvironment.voidType : StdEnvironment.errorType;
        return ast.type;
    }

    @Override
    public Object visitBooleanExpr(BooleanExpr ast, Object o) {
        ast.type = StdEnvironment.booleanType;
        return ast.type;
    }

    @Override
    public Object visitIntExpr(IntExpr ast, Object o) {
        ast.type = StdEnvironment.intType;
        return ast.type;
    }

    @Override
    public Object visitFloatExpr(FloatExpr ast, Object o) {
        ast.type = StdEnvironment.floatType;
        return ast.type;
    }

    @Override
    public Object visitVarExpr(VarExpr ast, Object o) {
        ast.type = (Type) ast.V.visit(this, ast);
        return ast.type;
    }

    @Override
    public Object visitStringExpr(StringExpr ast, Object o) {
        ast.type = StdEnvironment.stringType;
        return ast.type;
    }

    // Declarations

    @Override
    public Object visitFuncDecl(FuncDecl ast, Object o) {
        declareVariable(ast.I, ast);
        idTable.insert(ast.I.spelling, ast); // Insert the function declaration into the symbol table

        // Your code goes here

        // HINT: Pass ast as the 2nd argument so that the formal parameters
        // of the function can be extracted when the function body is visited.
	//
        Type funcReturnType = (Type) ast.T.visit(this, o);
        Ident funcIdent = ast.I;
        String funcName = funcIdent.spelling;
        List funcParaList = (List) ast.PL;
        if (funcName.equals("main")) {
            if (mainFunctionDeclared) {
                reporter.reportError(ErrorMessage.IDENTIFIER_REDECLARED.getMessage(), funcName, funcIdent.position);
                
            } else {
                mainFunctionDeclared = true;
                if (!funcReturnType.isIntType()) {
                    reporter.reportError(ErrorMessage.MAIN_RETURN_TYPE_NOT_INT.getMessage(), funcName, funcIdent.position);
                }
                
            }
        }

        ast.S.visit(this, ast); // Visit the function body

        return null;
    }

    @Override
    public Object visitDeclList(DeclList ast, Object o) {
        ast.D.visit(this, null);
        ast.DL.visit(this, null);
        return null;
    }

    @Override
    public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
        return null;
    }

    @Override
    public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);

	// Fill the rest
        if (ast.T.isVoidType()) {
            reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID.getMessage(), ast.I.spelling, ast.I.position);
        } else if (ast.T.isArrayType()) { // e.g. int a[];
            ArrayType arrayType = (ArrayType) ast.T.visit(this, o);
            // Check if the type of the array is void (e.g. void a[])
            if (arrayType.T.isVoidType()) {
                reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID_ARRAY.getMessage(), ast.I.spelling, ast.I.position);
            } else {
                // Check if array contains elements, it should by of type int (e.g. x["3131"];)
                if (!(arrayType.E.isEmptyExpr())) {
                    if (!(arrayType.E instanceof IntExpr)) {
                        reporter.reportError(ErrorMessage.ARRAY_SUBSCRIPT_NOT_INTEGER.getMessage(), ast.I.spelling, ast.I.position);
                    } else {
                        // Check if the array initialiser has more elements than the array size
                        // e.g. int a[3] = {1, 2, 3, 4};
                       
                        if ((ast.E) instanceof ArrayInitExpr) {
                            int numElements = countElementsOfArrayInitialiser(((ArrayInitExpr) ast.E).IL);
                            Integer arraySize = Integer.parseInt(((IntExpr) (arrayType).E).IL.spelling);
                            if (numElements > arraySize) {
                                reporter.reportError(ErrorMessage.EXCESS_ELEMENTS_IN_ARRAY_INITIALISER.getMessage(), ast.I.spelling, ast.I.position);
                            }
                            ast.E.visit(this, ast);
                        }
                    }
                } else { // arrayType.E.isEmptyExpr() (e.g. int a[]...)
                    if (ast.E.isEmptyExpr()) { // e.g. int a[] = ;
                        reporter.reportError(ErrorMessage.ARRAY_SIZE_MISSING.getMessage(), ast.I.spelling, ast.I.position);
                    } else if (!(ast.E instanceof ArrayInitExpr)) { // e.g. int a[] = 1;
                        reporter.reportError(ErrorMessage.INVALID_INITIALISER_SCALAR_FOR_ARRAY.getMessage(), ast.I.spelling, ast.I.position);
                    }
                    ast.E.visit(this, ast);
                }
            }
        } else {
            // Check if the initialiser is an array when the type is not an array
            // e.g. int a = {1, 2, 3};
            if (ast.E instanceof ArrayExpr) {
                reporter.reportError(ErrorMessage.INVALID_INITIALISER_ARRAY_FOR_SCALAR.getMessage(), ast.I.spelling, ast.I.position);
            }
            // Check if type of initialiser is not the same as the type of the variable
            // e.g. int a = 1.0;
            Type initialiserType = (Type) ast.E.visit(this, ast);
            if (initialiserType != null && ast.T != null && !(ast.T.assignable(initialiserType))) {
                reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_ASSIGNMENT.getMessage(), ast.I.spelling, ast.I.position);
            }
        }
        return null;
    }

    @Override
    public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {
        declareVariable(ast.I, ast);
	
	// Fill the rest
        if (ast.T.isVoidType()) {
            reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID.getMessage(), ast.I.spelling, ast.I.position);
        } else if (ast.T.isArrayType()) { // e.g. 
            ArrayType arrayType = (ArrayType) ast.T.visit(this, o);
            // Check if the type of the array is void (e.g. void a[])  
            if (arrayType.T.isVoidType()) {
                reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID_ARRAY.getMessage(), ast.I.spelling, ast.I.position);
            } else {
                // Check if array contains elements, it should by of type int (e.g. x["3131"];)
                if (!arrayType.E.isEmptyExpr()) {
                    if (!(arrayType.E instanceof IntExpr)) {
                        reporter.reportError(ErrorMessage.ARRAY_SUBSCRIPT_NOT_INTEGER.getMessage(), ast.I.spelling, ast.I.position);
                    } else {
                        // Check if the array initialiser has more elements than the array size
                        // e.g. int a[3] = {1, 2, 3, 4};
                        // reporter.reportError(ErrorMessage.EXCESS_ELEMENTS_IN_ARRAY_INITIALISER.getMessage(), ast.I.spelling, ast.I.position);
                        if ((ast.E) instanceof ArrayInitExpr) {
                            int numElements = countElementsOfArrayInitialiser(((ArrayInitExpr) ast.E).IL);
                            Integer arraySize = Integer.parseInt(((IntExpr) (arrayType).E).IL.spelling);
                            if (numElements > arraySize) {
                                reporter.reportError(ErrorMessage.EXCESS_ELEMENTS_IN_ARRAY_INITIALISER.getMessage(), ast.I.spelling, ast.I.position);
                            }
                            ast.E.visit(this, ast);
                        }
                    }
                }
            }
        } else {
            // Check if the initialiser is an array when the type is not an array
            // e.g. int a = {1, 2, 3};
            if (ast.E instanceof ArrayExpr) {
                reporter.reportError(ErrorMessage.INVALID_INITIALISER_ARRAY_FOR_SCALAR.getMessage(), ast.I.spelling, ast.I.position);
            } 
            // Check if type of initialiser is not the same as the type of the variable
            // e.g. int a = 1.0;
            Type initialiserType = (Type) ast.E.visit(this, ast);
            if (initialiserType != null && ast.T != null && !(ast.T.assignable(initialiserType))) {
                reporter.reportError(ErrorMessage.INCOMPATIBLE_TYPE_FOR_ASSIGNMENT.getMessage(), ast.I.spelling, ast.I.position);
            }

        }
        return null;
    }

    // Parameters

    @Override
    public Object visitParaList(ParaList ast, Object o) {
        ast.P.visit(this, null);
        ast.PL.visit(this, null);
        return null;
    }

    @Override
    public Object visitParaDecl(ParaDecl ast, Object o) {
        declareVariable(ast.I, ast);

        if (ast.T.isVoidType()) {
            reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID.getMessage(), ast.I.spelling, ast.I.position);
        } else if (ast.T.isArrayType()) {
            if (((ArrayType) ast.T).T.isVoidType()) {
                reporter.reportError(ErrorMessage.IDENTIFIER_DECLARED_VOID_ARRAY.getMessage(), ast.I.spelling, ast.I.position);
            }
        }


        return null;
    }

    @Override
    public Object visitEmptyParaList(EmptyParaList ast, Object o) {
        return null;
    }

    // Arguments
   
    // Your visitor methods for arguments go here
    @Override
    public Object visitArgList(ArgList ast, Object o) {
        ast.A.visit(this, o);
        ast.AL.visit(this, o);
        return null;
    }

    @Override 
    public Object visitArg(Arg ast, Object o) {
        Type argType = (Type) ast.E.visit(this, o);
        ast.type = argType;
        return ast.type;
    }

    @Override
    public Object visitEmptyArgList(EmptyArgList ast, Object o) {
        return null;
    }

    // Types

    @Override
    public Object visitErrorType(ErrorType ast, Object o) {
        return StdEnvironment.errorType;
    }

    @Override
    public Object visitBooleanType(BooleanType ast, Object o) {
        return StdEnvironment.booleanType;
    }

    @Override
    public Object visitIntType(IntType ast, Object o) {
        return StdEnvironment.intType;
    }

    @Override
    public Object visitFloatType(FloatType ast, Object o) {
        return StdEnvironment.floatType;
    }

    @Override
    public Object visitStringType(StringType ast, Object o) {
        return StdEnvironment.stringType;
    }

    @Override
    public Object visitVoidType(VoidType ast, Object o) {
        return StdEnvironment.voidType;
    }

    @Override
    public Object visitArrayType(ArrayType ast, Object o) {
        return ast;
    }

    // Literals, Identifiers and Operators

    @Override
    public Object visitIdent(Ident I, Object o) {
        Optional<IdEntry> binding = idTable.retrieve(I.spelling);
        binding.ifPresent(entry -> I.decl = entry.attr); // Link the identifier to its declaration
        return binding.map(entry -> entry.attr).orElse(null);
    }

    @Override
    public Object visitBooleanLiteral(BooleanLiteral SL, Object o) {
        return StdEnvironment.booleanType;
    }

    @Override
    public Object visitIntLiteral(IntLiteral IL, Object o) {
        return StdEnvironment.intType;
    }

    @Override
    public Object visitFloatLiteral(FloatLiteral IL, Object o) {
        return StdEnvironment.floatType;
    }

    @Override
    public Object visitStringLiteral(StringLiteral IL, Object o) {
        return StdEnvironment.stringType;
    }

    @Override
    public Object visitOperator(Operator O, Object o) {
        return null;
    }

    // variable names

    // Creates a small AST to represent the "declaration" of each built-in
    // function, and enters it in the symbol table.

    private FuncDecl declareStdFunc(Type resultType, String id, VC.ASTs.List pl) {
        var binding = new FuncDecl(resultType, new Ident(id, dummyPos), pl,
                new EmptyStmt(dummyPos), dummyPos);
        idTable.insert(id, binding);
        return binding;
    }

    // Creates small ASTs to represent "declarations" of all
    // build-in functions.
    // Inserts these "declarations" into the symbol table.

    private final static Ident dummyI = new Ident("x", dummyPos);

    private void establishStdEnvironment() {
        // Define four primitive types
        // errorType is assigned to ill-typed expressions

        StdEnvironment.booleanType = new BooleanType(dummyPos);
        StdEnvironment.intType = new IntType(dummyPos);
        StdEnvironment.floatType = new FloatType(dummyPos);
        StdEnvironment.stringType = new StringType(dummyPos);
        StdEnvironment.voidType = new VoidType(dummyPos);
        StdEnvironment.errorType = new ErrorType(dummyPos);

        // enter into the declarations for built-in functions into the table

        StdEnvironment.getIntDecl = declareStdFunc(StdEnvironment.intType,
                "getInt", new EmptyParaList(dummyPos));
        StdEnvironment.putIntDecl = declareStdFunc(StdEnvironment.voidType,
                "putInt", new ParaList(
                        new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putIntLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putIntLn", new ParaList(
                        new ParaDecl(StdEnvironment.intType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.getFloatDecl = declareStdFunc(StdEnvironment.floatType,
                "getFloat", new EmptyParaList(dummyPos));
        StdEnvironment.putFloatDecl = declareStdFunc(StdEnvironment.voidType,
                "putFloat", new ParaList(
                        new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putFloatLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putFloatLn", new ParaList(
                        new ParaDecl(StdEnvironment.floatType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolDecl = declareStdFunc(StdEnvironment.voidType,
                "putBool", new ParaList(
                        new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));
        StdEnvironment.putBoolLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putBoolLn", new ParaList(
                        new ParaDecl(StdEnvironment.booleanType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putStringLn", new ParaList(
                        new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putStringDecl = declareStdFunc(StdEnvironment.voidType,
                "putString", new ParaList(
                        new ParaDecl(StdEnvironment.stringType, dummyI, dummyPos),
                        new EmptyParaList(dummyPos), dummyPos));

        StdEnvironment.putLnDecl = declareStdFunc(StdEnvironment.voidType,
                "putLn", new EmptyParaList(dummyPos));
    }
}
