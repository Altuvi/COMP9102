/*
*** Emitter.java 
*
* Sun 30 Mar 2025 14:56:56 AEDT
*
* A new frame object is created for every function just before the
* function is being translated in visitFuncDecl.
*
* All the information about the translation of a function should be
* placed in this Frame object and passed across the AST nodes as the
* 2nd argument of every visitor method in Emitter.java.
*
*/

package VC.CodeGen;

import VC.ASTs.*;
import VC.ErrorReporter;
import VC.StdEnvironment;

public final class Emitter implements Visitor {

private ErrorReporter errorReporter;
private String inputFilename;
private String classname;
private String outputFilename;
private int currentArrayIndex;

public Emitter(String inputFilename, ErrorReporter reporter) {
    this.inputFilename = inputFilename;
    errorReporter = reporter;
    
    int i = inputFilename.lastIndexOf('.');
    if (i > 0)
        classname = inputFilename.substring(0, i);
    else
        classname = inputFilename;
    
}

// ast must be a Program node

public final void gen(AST ast) {
    ast.visit(this, null); 
    JVM.dump(classname + ".j");
}
    
// Programs
public Object visitProgram(Program ast, Object o) {

    /* This method works for scalar variables only. You need to add code
     * to handle all array-related declarations and initialisations.
     */ 

    // Generates the default constructor initialiser 
    emit(JVM.CLASS, "public", classname);
    emit(JVM.SUPER, "java/lang/Object");

    emit("");

    // Three subpasses:

    // (1) Generate .field definition statements since
    //     these are required to appear before method definitions.
    //
    // This can also be done using a separate visitor.
    List list = ast.FL;
    while (!list.isEmpty()) {
    	DeclList dlAST = (DeclList) list;
    	if (dlAST.D instanceof GlobalVarDecl) {
            GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
            if (vAST.T.isArrayType()) {
                Type arrayType = ((ArrayType)vAST.T).T; // get element type
                emit(JVM.STATIC_FIELD, vAST.I.spelling, "[" + VCtoJavaType(arrayType));
            } else {
                emit(JVM.STATIC_FIELD, vAST.I.spelling, VCtoJavaType(vAST.T));
            }
            
        }
    	list = dlAST.DL;
    }

    emit("");

    // (2) Generate <clinit> for global variables (assumed to be static)
    //
    // This can also be done using a separate visitor.

    emit("; standard class static initializer ");
    emit(JVM.METHOD_START, "static <clinit>()V");
    emit("");

    // create a Frame for <clinit>

    Frame frame = new Frame(false);

    list = ast.FL;
    while (!list.isEmpty()) {
    	DeclList dlAST = (DeclList) list;
    	if (dlAST.D instanceof GlobalVarDecl) {
            GlobalVarDecl vAST = (GlobalVarDecl) dlAST.D;
            // Check type of global variable
            if (vAST.T.isArrayType()) {
                Type arrayType = ((ArrayType)vAST.T).T; // get element type
                IntLiteral arraySize = ((IntExpr)((ArrayType) vAST.T).E).IL;
                String arraySizeStr = arraySize.spelling;
                emitICONST(Integer.parseInt(arraySizeStr)); // Push array size
                frame.push();

                if (arrayType.isIntType() || arrayType.isBooleanType()) {
                    emit(JVM.NEWARRAY, "int");
                } else if (arrayType.isFloatType()) {
                    emit(JVM.NEWARRAY, "float");
                }

                if (!vAST.E.isEmptyExpr()) {
                    //do init of array
                    vAST.E.visit(this, frame);
                }
                 
                emitPUTSTATIC("["+VCtoJavaType(arrayType), vAST.I.spelling);
                frame.pop();

            } else { 
                //doing scalar
                if (!vAST.E.isEmptyExpr()) {
                    vAST.E.visit(this, frame);
                } else {
                    if (vAST.T.equals(StdEnvironment.floatType))
                        emit(JVM.FCONST_0);
                    else
                        emit(JVM.ICONST_0);
    
                     frame.push();
                }
    
                emitPUTSTATIC(VCtoJavaType(vAST.T), vAST.I.spelling); 
                frame.pop();
                }
            }
            
        list = dlAST.DL;
    }

    emit("");
    emit("; set limits used by this method");

    emit(JVM.LIMIT, "locals", frame.getNewIndex());
    emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());
    
    emit(JVM.RETURN);
    emit(JVM.METHOD_END, "method");

    emit("");

    // (3) Generate Java bytecode for the VC program

    emit("; standard constructor initializer ");
    emit(JVM.METHOD_START, "public <init>()V");
    emit(JVM.LIMIT, "stack 1");
    emit(JVM.LIMIT, "locals 1");
    emit(JVM.ALOAD_0);
    emit(JVM.INVOKESPECIAL, "java/lang/Object/<init>()V");
    emit(JVM.RETURN);
    emit(JVM.METHOD_END, "method");

    return ast.FL.visit(this, o);
}

// Statements

public Object visitStmtList(StmtList ast, Object o) {
    ast.S.visit(this, o);
    ast.SL.visit(this, o);
    return null;
}

public Object visitCompoundStmt(CompoundStmt ast, Object o) {
    Frame frame = (Frame) o; 

    String scopeStart = frame.getNewLabel();
    String scopeEnd = frame.getNewLabel();
    frame.scopeStart.push(scopeStart);
    frame.scopeEnd.push(scopeEnd);

    emit(scopeStart + ":");
    if (ast.parent instanceof FuncDecl) {
    	if (((FuncDecl) ast.parent).I.spelling.equals("main")) {
            emit(JVM.VAR, "0 is argv [Ljava/lang/String; from " + (String) frame.scopeStart.peek() + " to " +  (String) frame.scopeEnd.peek());
            emit(JVM.VAR, "1 is vc$ L" + classname + "; from " + (String) frame.scopeStart.peek() + " to " +  (String) frame.scopeEnd.peek());
            // Generate code for the initialiser vc$ = new classname();
            emit(JVM.NEW, classname);
            emit(JVM.DUP);
            frame.push(2);
            emit("invokenonvirtual", classname + "/<init>()V");
            frame.pop();
            emit(JVM.ASTORE_1);
            frame.pop();
    	} else {
            emit(JVM.VAR, "0 is this L" + classname + "; from " + (String) frame.scopeStart.peek() + " to " +  (String) frame.scopeEnd.peek());
        ((FuncDecl) ast.parent).PL.visit(this, o);
        }
    }

    ast.DL.visit(this, o);
    ast.SL.visit(this, o);
    emit(scopeEnd + ":");

    frame.scopeStart.pop();
    frame.scopeEnd.pop();
    return null;
}

public Object visitReturnStmt(ReturnStmt ast, Object o) {
    Frame frame = (Frame)o;

/*
int main() { return 0; } must be interpretted as 
public static void main(String[] args) { return ; }
Therefore, "return expr", if present in the main of a VC program
must be translated into a RETURN rather than IRETURN instruction.
*/

    if (frame.isMain())  {
        emit(JVM.RETURN);
        return null;
    }

/*  Your other code goes here for handling return <Expr>. */ 
    if (ast.E.isEmptyExpr()) {
        emit(JVM.RETURN);
    } else { // Return has an expression
        ast.E.visit(this, o);

        Type returnType = ast.E.type;

        if (returnType.isIntType() || returnType.isBooleanType()) {
            emit(JVM.IRETURN);
        } else if (returnType.isFloatType()) {
            emit(JVM.FRETURN);
        }
        frame.pop();
    }
    return null;
}

public Object visitExprStmt(ExprStmt ast, Object o) {
    Frame frame = (Frame) o;
    ast.E.visit(this, o);
    if (!ast.E.type.isVoidType()) {
        emit(JVM.POP);
        frame.pop();
    }
    return null;
}

public Object visitIfStmt(IfStmt ast, Object o) {
    Frame frame = (Frame) o;
    String elseLabel = frame.getNewLabel();
    String endLabel = frame.getNewLabel();

    // Evaluate condition
    ast.E.visit(this, frame);

    // If condition is false (0), jump to else part
    emit(JVM.IFEQ, elseLabel);
    frame.pop();

    // "Then" part
    ast.S1.visit(this, o);
    emit(JVM.GOTO, endLabel);

    // "Else" part
    emit(elseLabel + ":");
    if (ast.S2.isEmptyStmt()) {
        // do nothing
    } else {
        ast.S2.visit(this, o);
    }

    // End of if statement
    emit(endLabel + ":");
    return null;
}

public Object visitWhileStmt(WhileStmt ast, Object o) {
    Frame frame = (Frame) o;
    String loopStartLabel = frame.getNewLabel();
    String loopEndLabel = frame.getNewLabel();

    // Push labels for break/continue
    frame.conStack.push(loopStartLabel); // Continue goes back to condition
    frame.brkStack.push(loopEndLabel); // Break goes to end of loop

    emit(loopStartLabel + ":");

    ast.E.visit(this, frame); // Evaluate condition (leaves 0 or 1)
    emit(JVM.IFEQ, loopEndLabel); // If false, exit loop
    frame.pop(); // Pop condition value

    ast.S.visit(this, o); //  Loop body
    emit(JVM.GOTO, loopStartLabel); // Go back to condition

    emit(loopEndLabel + ":"); // End of loop
    // Pop labels for break/continue
    if (!frame.conStack.isEmpty()) frame.conStack.pop();
    if (!frame.brkStack.isEmpty()) frame.brkStack.pop();

    return null;
}

public Object visitBreakStmt(BreakStmt ast, Object o) {
    Frame frame  = (Frame) o;  
    String loopEndlabel = frame.brkStack.peek();
    emit(JVM.GOTO, loopEndlabel);
    return null;
}

public Object visitContinueStmt(ContinueStmt ast, Object o) {
    Frame frame  = (Frame) o;  
    String loopStartLabel = frame.conStack.peek();
    emit(JVM.GOTO, loopStartLabel);
    return null;
}

public Object visitForStmt(ForStmt ast, Object o) {
    Frame frame = (Frame) o;

    // Labels
    String conditionLabel = frame.getNewLabel(); // Start of condition check
    String updateLabel = frame.getNewLabel(); // Start of update expression
    String endLabel = frame.getNewLabel(); // End of loop

    // Push labels for break/continue
    frame.conStack.push(updateLabel); // Continue goes back to condition
    frame.brkStack.push(endLabel); // Break goes to end of loop

    // Initialization
    if (!ast.E1.isEmptyExpr()) {
        ast.E1.visit(this, o);
        // If E1 produced a value (e.g., assignment result), and it's not used, pop it
        if (!ast.E1.type.isVoidType()) {
            emit(JVM.POP);
            frame.pop();
        }
    }

    // Condition check
    emit(conditionLabel + ":");
    if (!ast.E2.isEmptyExpr()) {
        ast.E2.visit(this, o); // Evaluate condition => 0 or 1
        emit(JVM.IFEQ, endLabel); // If false, jump to end
        frame.pop(); // Pop condition value
    }

    // Loop body
    ast.S.visit(this, frame); // Execute loop body

    // Update expression
    emit(updateLabel + ":");
    if (!ast.E3.isEmptyExpr()) {
        ast.E3.visit(this, frame); // Evaluate update expression
        if (!ast.E3.type.isVoidType()) {
            emit(JVM.POP);
            frame.pop();
        }
    }

    emit(JVM.GOTO, conditionLabel); // Go back to condition check

    // Loop End
    emit(endLabel + ":");
    // Pop labels for break/continue
    if (!frame.conStack.isEmpty()) frame.conStack.pop();
    if (!frame.brkStack.isEmpty()) frame.brkStack.pop();

    return null;
}

public Object visitEmptyStmtList(EmptyStmtList ast, Object o) {
    return null;
}

public Object visitEmptyCompStmt(EmptyCompStmt ast, Object o) {
    return null;
}

public Object visitEmptyStmt(EmptyStmt ast, Object o) {
    return null;
}

// Expressions

public Object visitAssignExpr(AssignExpr ast, Object o) {
    Frame frame  = (Frame) o;
    
    // Evaluate the right-hand side expression first
    ast.E2.visit(this, o); // Leaves value on stack

    // 

}

public Object visitCallExpr(CallExpr ast, Object o) {
    Frame frame = (Frame) o;
    String fname = ast.I.spelling;

    if (fname.equals("getInt")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/getInt()I");
    	frame.push();
    } else if (fname.equals("putInt")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putInt(I)V");
    	frame.pop();
    } else if (fname.equals("putIntLn")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putIntLn(I)V");
    	frame.pop();
    } else if (fname.equals("getFloat")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/getFloat()F");
    	frame.push();
    } else if (fname.equals("putFloat")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putFloat(F)V");
    	frame.pop();
    } else if (fname.equals("putFloatLn")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putFloatLn(F)V");
    	frame.pop();
    } else if (fname.equals("putBool")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putBool(Z)V");
    	frame.pop();
    } else if (fname.equals("putBoolLn")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putBoolLn(Z)V");
    	frame.pop();
    } else if (fname.equals("putString")) {
    	ast.AL.visit(this, o);
    	emit(JVM.INVOKESTATIC, "VC/lang/System/putString(Ljava/lang/String;)V");
    	frame.pop();
    } else if (fname.equals("putStringLn")) {
    	ast.AL.visit(this, o);
    	emit(JVM.INVOKESTATIC, "VC/lang/System/putStringLn(Ljava/lang/String;)V");
    	frame.pop();
    } else if (fname.equals("putLn")) {
    	ast.AL.visit(this, o); // push args (if any) into the op stack
    	emit("invokestatic VC/lang/System/putLn()V");
    } else { // programmer-defined functions

    	FuncDecl fAST = (FuncDecl) ast.I.decl;

    	// all functions except main are assumed to be instance methods
    	if (frame.isMain()) 
            emit("aload_1"); // vc.funcname(...)
    	else
            emit("aload_0"); // this.funcname(...)

        frame.push();

    	ast.AL.visit(this, o);
    
    	String retType = VCtoJavaType(fAST.T);
    
    	// The types of the parameters of the called function are not
    	// directly available in the FuncDecl node but can be gathered
    	// by traversing its field PL.

    	StringBuffer argsTypes = new StringBuffer("");
    	List fpl = fAST.PL;
    	while (! fpl.isEmpty()) {
            if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
        	argsTypes.append("Z");         
            else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
        	argsTypes.append("I");         
            else
        	argsTypes.append("F");         

            fpl = ((ParaList) fpl).PL;
     	}
    
    	emit("invokevirtual", classname + "/" + fname + "(" + argsTypes + ")" + retType);
    	frame.pop(argsTypes.length() + 1);

    	if (! retType.equals("V"))
            frame.push();
    }
    return null;
}

public Object visitUnaryExpr(UnaryExpr ast, Object o) {
    Frame frame = (Frame) o;
    String op = ast.O.spelling;

    ast.E.visit(this, o); // Evaluate expression, leave value on stack

    if (op.equals("+")) {
        // Unary plus is a no-op for numeric types
    } else if (op.equals("-")) {
        if (ast.E.type.isIntType()) {
            emit(JVM.INEG);
        } else if (ast.E.type.isFloatType()) {
            emit(JVM.FNEG);
        }
    } else if (op.equals("!")) {
        // Logical NOT for boolean type (int 0 or 1)
        String trueLabel = frame.getNewLabel();
        String endLabel = frame.getNewLabel();

        // If expression value is not 0 (true), jump to trueLabel
        emit(JVM.IFNE, trueLabel);
        // Value is 0 (false), so push 1 (true)
        emit(JVM.ICONST_1);
        emit(JVM.GOTO, endLabel);

        emit(trueLabel + ":"); 
        // Value is 1 (true), so push 0 (false)
        emit(JVM.ICONST_0);
        emit(endLabel + ":");

    }
    return null;
}

public Object visitBinaryExpr(BinaryExpr ast, Object o) {
    Frame frame = (Frame) o;
    String op = ast.O.spelling;
    Type type1 = ast.E1.type;
    Type type2 = ast.E2.type;
    Type resultType = ast.type;

    // Handle && and || operators
    if (op.equals("&&")) {
        String falseLabel = frame.getNewLabel();
        String endLabel = frame.getNewLabel();

        ast.E1.visit(this, o); // Evaluate LHS
        emit(JVM.IFEQ, falseLabel); // If LHS is false (0), jump to falseLabel
        frame.pop(); // Pop LHS value (it was true, IFEQ didn't consume it)

        ast.E2.visit(this, o); // Evaluate RHS
        emit(JVM.IFEQ, falseLabel); // If RHS is false (0), jump to falseLabel
        frame.pop(); // Pop RHS value (it was true, IFEQ didn't consume it)

        // Both LHS and RHS are true (1)
        emit(JVM.ICONST_1); // Push 1 (true)
        emit(JVM.GOTO, endLabel); // Jump to endLabel

        emit(falseLabel + ":"); // falseLabel
        emit(JVM.ICONST_0); // Push 0 (false)

        emit(endLabel + ":"); // endLabel
        frame.push(); // Push result of && operation
        return null;

    } else if (op.equals("||")) {
        String trueLabel = frame.getNewLabel();
        String endLabel = frame.getNewLabel();

        ast.E1.visit(this, o); // Evaluate LHS
        emit(JVM.IFNE, trueLabel); // If LHS is true (1), jump to trueLabel
        frame.pop(); // Pop LHS value (it was false, IFEQ didn't consume it)

        ast.E2.visit(this, o); // Evaluate RHS
        emit(JVM.IFNE, trueLabel); // If RHS is true (1), jump to trueLabel
        frame.pop(); // Pop RHS value (it was false, IFEQ didn't consume it)

        // Both LHS and RHS are false (0)
        emit(JVM.ICONST_0); // Push 0 (false)
        emit(JVM.GOTO, endLabel); // Jump to endLabel

        emit(trueLabel + ":"); // trueLabel
        emit(JVM.ICONST_1); // Push 1 (true)

        emit(endLabel + ":"); // endLabel
        frame.push(); // Push result of || operation
        return null;
    }

    // Handle other binary operators
    ast.E1.visit(this, o); // push value 1
    ast.E2.visit(this, o); // push value 2

    if (resultType.isIntType()) {
        switch (op) {
            case "i+": emit(JVM.IADD); break;
            case "i-": emit(JVM.ISUB); break;
            case "i*": emit(JVM.IMUL); break;
            case "i/": emit(JVM.IDIV); break;
            // Comparision yields boolean (0 or 1)
            case "i==": emitIF_ICMPCOND("i==", frame); return null;
            case "i!=": emitIF_ICMPCOND("i!=", frame); return null;
            case "i<": emitIF_ICMPCOND("i<", frame); return null;
            case "i<=": emitIF_ICMPCOND("i<=", frame); return null;
            case "i>": emitIF_ICMPCOND("i>", frame); return null;
            case "i>=": emitIF_ICMPCOND("i>=", frame); return null;
        }
        frame.pop(); 
    } else if (resultType.isFloatType()) {
        switch (op) {
            case "f+": emit(JVM.FADD); break;
            case "f-": emit(JVM.FSUB); break;
            case "f*": emit(JVM.FMUL); break;
            case "f/": emit(JVM.FDIV); break;
            // Comparision yields boolean (0 or 1)
            case "f==": emitFCMP("f==", frame); return null;
            case "f!=": emitFCMP("f!=", frame); return null;
            case "f<": emitFCMP("f<", frame); return null;
            case "f<=": emitFCMP("f<=", frame); return null;
            case "f>": emitFCMP("f>", frame); return null;
            case "f>=": emitFCMP("f>=", frame); return null;
        }
        frame.pop();
    } else if (resultType.isBooleanType()) {
        // ==, != for booleans treated as integers
        switch (op) {
            case "i==": emitIF_ICMPCOND("i==", frame); return null;
            case "i!=": emitIF_ICMPCOND("i!=", frame); return null;
        }
        frame.pop();
    }
    return null;
}

public Object visitVarExpr(VarExpr ast, Object o) {
    Frame frame = (Frame) o;
    Ident varIdent = ((SimpleVar) ast.V).I;
    Decl varDecl = (Decl) varIdent.decl;

    
    if (varDecl.isLocalVarDecl() || varDecl.isParaDecl()) {
        int index = (varDecl).index;
        Type varType = varDecl.T;
        
        if (varType.isIntType() || varType.isBooleanType()) {
            emit(JVM.ILOAD, index);
        } else if (varType.isFloatType()) {
            emit(JVM.FLOAD, index);
        } else if (varType.isArrayType()) {
            emit(JVM.ALOAD, index);
        } 
        frame.push();
    } else if (varDecl.isGlobalVarDecl()) {
        String varName = varIdent.spelling;
        Type varType = varDecl.T;

        if (varType.isIntType() || varType.isBooleanType()) {
            emitGETSTATIC(VCtoJavaType(varType), varName);
        } else if (varType.isFloatType()) {
            emitGETSTATIC(VCtoJavaType(varType), varName);
        }
        else if (varType.isArrayType()) {
            // Get type of elements in the array
            String elementType = VCtoJavaType(((ArrayType) varType).T);
            emitGETSTATIC("[" + elementType, varName);
        }
        frame.push();
    }

    return null;
}

public Object visitEmptyExpr(EmptyExpr ast, Object o) {
    return null;
}

public Object visitIntExpr(IntExpr ast, Object o) {
    ast.IL.visit(this, o);
    return null;
}

public Object visitFloatExpr(FloatExpr ast, Object o) {
    ast.FL.visit(this, o);
    return null;
}

public Object visitBooleanExpr(BooleanExpr ast, Object o) {
    ast.BL.visit(this, o);
    return null;
}

public Object visitStringExpr(StringExpr ast, Object o) {
    ast.SL.visit(this, o);
    return null;
}

// Declarations

public Object visitDeclList(DeclList ast, Object o) {
    ast.D.visit(this, o);
    ast.DL.visit(this, o);
    return null;
}

public Object visitEmptyDeclList(EmptyDeclList ast, Object o) {
    return null;
}

public Object visitFuncDecl(FuncDecl ast, Object o) {

    Frame frame; 

    if (ast.I.spelling.equals("main")) {

    	frame = new Frame(true);

    	// Assume that main has one String parameter and reserve 0 for it
    	frame.getNewIndex(); 

    	emit(JVM.METHOD_START, "public static main([Ljava/lang/String;)V"); 
    	// Assume implicitly that
    	//      classname vc$; 
    	// appears before all local variable declarations.
    	// (1) Reserve 1 for this object reference.

    	frame.getNewIndex(); 

    } else {

    	frame = new Frame(false);

    	// all other programmer-defined functions are treated as if
    	// they were instance methods
    	frame.getNewIndex(); // reserve 0 for "this"

    	String retType = VCtoJavaType(ast.T);

    	// The types of the parameters of the called function are not
    	// directly available in the FuncDecl node but can be gathered
    	// by traversing its field PL.

    	StringBuffer argsTypes = new StringBuffer("");
    	List fpl = ast.PL;
    	while (! fpl.isEmpty()) {
            if (((ParaList) fpl).P.T.equals(StdEnvironment.booleanType))
        	argsTypes.append("Z");         
            else if (((ParaList) fpl).P.T.equals(StdEnvironment.intType))
        	argsTypes.append("I");         
            else
        	argsTypes.append("F");         
        fpl = ((ParaList) fpl).PL;
    }

    emit(JVM.METHOD_START, ast.I.spelling + "(" + argsTypes + ")" + retType);
    }

    ast.S.visit(this, frame);

    // JVM requires an explicit return in every method. 
    // In VC, a function returning void may not contain a return, and
    // a function returning int or float is not guaranteed to contain
    // a return. Therefore, we add one at the end just to be sure.

    if (ast.T.equals(StdEnvironment.voidType)) {
    	emit("");
    	emit("; return may not be present in a VC function returning void"); 
    	emit("; The following return inserted by the VC compiler");
    	emit(JVM.RETURN); 
    } else if (ast.I.spelling.equals("main")) {
    	// In case VC's main does not have a return itself
    	emit(JVM.RETURN);
    } else
    	emit(JVM.NOP); 

    emit("");
    emit("; set limits used by this method");

    emit(JVM.LIMIT, "locals", frame.getNewIndex());
    emit(JVM.LIMIT, "stack", frame.getMaximumStackSize());

    emit(".end method");

    return null;
}

public Object visitGlobalVarDecl(GlobalVarDecl ast, Object o) {
    // nothing to be done
    return null;
}

public Object visitLocalVarDecl(LocalVarDecl ast, Object o) {

    /* You need to add code to handle arrays */

    Frame frame = (Frame) o;
    ast.index = frame.getNewIndex();
    String T = VCtoJavaType(ast.T);

    emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek() + " to " +  (String) frame.scopeEnd.peek());

    if (!ast.E.isEmptyExpr()) {
        // If expression is array initialisation, leave reference on stack
        if (ast.E.type.isArrayType()) {
            IntLiteral arraySize = ((IntExpr)((ArrayType) ast.T).E).IL;
            String arraySizeStr = arraySize.spelling;
            emitICONST(Integer.parseInt(arraySizeStr)); // Push array size
            // Check type of array local variable declaration
            if (ast.T.isIntType() || ast.T.isBooleanType()) {
                emit(JVM.NEWARRAY, "int"); // addresses on stack
            } else if (ast.T.isFloatType()) {
                emit(JVM.NEWARRAY, "float"); // addresses on stack
            }
            ast.E.visit(this, o);
            emit(JVM.ASTORE, ast.index); // Store reference in local variable
            frame.pop();
        } else { // Scalar case
            ast.E.visit(this, o);
            if (ast.T.equals(StdEnvironment.floatType)) {
                emitFSTORE(ast.I);
            } else {
                emitISTORE(ast.I);
            }
        frame.pop();
        }
    } else {
        if(ast.E.type.isArrayType()){
            IntLiteral arraySize = ((IntExpr)((ArrayType) ast.T).E).IL;
            String arraySizeStr = arraySize.spelling;
            emitICONST(Integer.parseInt(arraySizeStr)); // Push array size
            // Check type of array local variable declaration
            if (ast.T.isIntType() || ast.T.isBooleanType()) {
                emit(JVM.NEWARRAY, "int"); // addresses on stack
            } else if (ast.T.isFloatType()) {
                emit(JVM.NEWARRAY, "float"); // addresses on stack
            }
            emit(JVM.ASTORE, ast.index); // Store reference in local variable
            frame.pop();
        }
    }

    return null;
}

// Arrays
public Object visitArrayExprList(ArrayExprList ast, Object o) {
    Frame frame = (Frame) o;
    emit(JVM.DUP);
    emitICONST(currentArrayIndex);
    frame.push();
    currentArrayIndex++;
    ast.E.visit(this, o);
    if (ast.E.type.isIntType() || ast.E.type.isBooleanType()) {
        emit(JVM.IASTORE);
    } else if (ast.E.type.isFloatType()) {
        emit(JVM.FASTORE);
    }
    frame.pop(3);
    ast.EL.visit(this, o);
    return null;
}

public Object visitArrayInitExpr(ArrayInitExpr ast, Object o) {
    Frame frame = (Frame) o;
    currentArrayIndex = 0;
    ast.IL.visit(this, o);
    return null;
}

public Object visitArrayExpr(ArrayExpr ast, Object o) {
    Frame frame = (Frame) o;
    Ident arrayExprIdent = ((SimpleVar) ast.V).I;
    Decl arrayExprDecl = (Decl) arrayExprIdent.decl;

    if (arrayExprDecl.isLocalVarDecl() || arrayExprDecl.isParaDecl()) {
        int index = (arrayExprDecl).index; // local variable index
        Type arrayExprType = arrayExprDecl.T;

        emit(JVM.ALOAD, index); // Load array reference
        frame.push();
        ast.E.visit(this, o); // Push index of array element
        
        if (arrayExprType.isIntType() || arrayExprType.isBooleanType()) {
            emit(JVM.IALOAD);
        } else if (arrayExprType.isFloatType()) {
            emit(JVM.FALOAD);
        }
        frame.pop();
    } else if (arrayExprDecl.isGlobalVarDecl()) {
        String arrayExprName = arrayExprIdent.spelling;
        Type arrayExprType = arrayExprDecl.T;
        emitGETSTATIC("["+VCtoJavaType(arrayExprType), arrayExprName);

        ast.E.visit(this, o);

        if (arrayExprType.isIntType() || arrayExprType.isBooleanType()) {
            emit(JVM.IALOAD);
        } else if (arrayExprType.isFloatType()) {
            emit(JVM.FALOAD);
        }
        frame.pop();
    }
    return null;
}

public Object visitEmptyArrayExprList(EmptyArrayExprList ast, Object o) {
    return null;
}

// Parameters

public Object visitParaList(ParaList ast, Object o) {
    ast.P.visit(this, o);
    ast.PL.visit(this, o);
    return null;
}

public Object visitParaDecl(ParaDecl ast, Object o) {

    /* You need to add code to handle arrays */

    Frame frame = (Frame) o;
    ast.index = frame.getNewIndex();
    String T = VCtoJavaType(ast.T);

    emit(JVM.VAR + " " + ast.index + " is " + ast.I.spelling + " " + T + " from " + (String) frame.scopeStart.peek() + " to " +  (String) frame.scopeEnd.peek());
    return null;
}

public Object visitEmptyParaList(EmptyParaList ast, Object o) {
    return null;
}

// Arguments

public Object visitArgList(ArgList ast, Object o) {
    ast.A.visit(this, o);
    ast.AL.visit(this, o);
    return null;
}

public Object visitArg(Arg ast, Object o) {
    ast.E.visit(this, o);
    return null;
}

public Object visitEmptyArgList(EmptyArgList ast, Object o) {
    return null;
}

// Types

public Object visitIntType(IntType ast, Object o) {
    return null;
}

public Object visitFloatType(FloatType ast, Object o) {
    return null;
}

public Object visitBooleanType(BooleanType ast, Object o) {
    return null;
}

public Object visitVoidType(VoidType ast, Object o) {
    return null;
}

public Object visitErrorType(ErrorType ast, Object o) {
    return null;
}

public Object visitStringType(StringType ast, Object o) {
    return null;
}

public Object visitArrayType(ArrayType ast, Object o) {
    return null;
}



// Literals, Identifiers and Operators 

public Object visitIdent(Ident ast, Object o) {
    return null;
}

public Object visitIntLiteral(IntLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitICONST(Integer.parseInt(ast.spelling));
    frame.push();
    return null;
}

public Object visitFloatLiteral(FloatLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitFCONST(Float.parseFloat(ast.spelling));
    frame.push();
    return null;
}

public Object visitBooleanLiteral(BooleanLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emitBCONST(ast.spelling.equals("true"));
    frame.push();
    return null;
}

public Object visitStringLiteral(StringLiteral ast, Object o) {
    Frame frame = (Frame) o;
    emit(JVM.LDC, "\"" + ast.spelling.replace("\"","\\\"") + "\"");
    frame.push();
    return null;
}

public Object visitOperator(Operator ast, Object o) {
    return null;
}

// Variables 

public Object visitSimpleVar(SimpleVar ast, Object o) {
    return null;
}

// Auxiliary methods for byte code generation

// The following method appends an instruction directly into the JVM 
// Code Store. It is called by all other overloaded emit methods.

private void emit(String s) {
    JVM.append(new Instruction(s)); 
}

private void emit(String s1, String s2) {
    emit(s1 + " " + s2);
}

private void emit(String s1, int i) {
    emit(s1 + " " + i);
}

private void emit(String s1, float f) {
    emit(s1 + " " + f);
}

private void emit(String s1, String s2, int i) {
    emit(s1 + " " + s2 + " " + i);
}

private void emit(String s1, String s2, String s3) {
    emit(s1 + " " + s2 + " " + s3);
}

private void emitIF_ICMPCOND(String op, Frame frame) {
    String opcode;

    if (op.equals("i!="))
    	opcode = JVM.IF_ICMPNE;
    else if (op.equals("i=="))
    	opcode = JVM.IF_ICMPEQ;
    else if (op.equals("i<"))
    	opcode = JVM.IF_ICMPLT;
    else if (op.equals("i<="))
    	opcode = JVM.IF_ICMPLE;
    else if (op.equals("i>"))
    	opcode = JVM.IF_ICMPGT;
    else // if (op.equals("i>="))
    	opcode = JVM.IF_ICMPGE;

    String falseLabel = frame.getNewLabel();
    String nextLabel = frame.getNewLabel();

    emit(opcode, falseLabel);
    frame.pop(2); 
    emit("iconst_0");
    emit("goto", nextLabel);
    emit(falseLabel + ":");
    emit(JVM.ICONST_1);
    frame.push(); 
    emit(nextLabel + ":");
}

private void emitFCMP(String op, Frame frame) {
    String opcode;

    if (op.equals("f!="))
    	opcode = JVM.IFNE;
    else if (op.equals("f=="))
    	opcode = JVM.IFEQ;
    else if (op.equals("f<"))
    	opcode = JVM.IFLT;
    else if (op.equals("f<="))
    	opcode = JVM.IFLE;
    else if (op.equals("f>"))
    	opcode = JVM.IFGT;
    else // if (op.equals("f>="))
    	opcode = JVM.IFGE;

    String falseLabel = frame.getNewLabel();
    String nextLabel = frame.getNewLabel();

    emit(JVM.FCMPG);
    frame.pop(2);
    emit(opcode, falseLabel);
    emit(JVM.ICONST_0);
    emit("goto", nextLabel);
    emit(falseLabel + ":");
    emit(JVM.ICONST_1);
    frame.push();
    emit(nextLabel + ":");

}

private void emitILOAD(int index) {
    if (index >= 0 && index <= 3) 
    	emit(JVM.ILOAD + "_" + index); 
    else
    	emit(JVM.ILOAD, index); 
}

private void emitFLOAD(int index) {
    if (index >= 0 && index <= 3) 
    	emit(JVM.FLOAD + "_"  + index); 
    else
    	emit(JVM.FLOAD, index); 
}

private void emitGETSTATIC(String T, String I) {
    emit(JVM.GETSTATIC, classname + "/" + I, T); 
}

private void emitISTORE(Ident ast) {
    int index;
    if (ast.decl instanceof ParaDecl)
    	index = ((ParaDecl) ast.decl).index; 
    else
    	index = ((LocalVarDecl) ast.decl).index; 
    
    if (index >= 0 && index <= 3) 
    	emit(JVM.ISTORE + "_" + index); 
    else
    	emit(JVM.ISTORE, index); 
}

private void emitFSTORE(Ident ast) {
    int index;
    if (ast.decl instanceof ParaDecl)
    	index = ((ParaDecl) ast.decl).index; 
    else
    	index = ((LocalVarDecl) ast.decl).index; 
    if (index >= 0 && index <= 3) 
    	emit(JVM.FSTORE + "_" + index); 
    else
    	emit(JVM.FSTORE, index); 
}

private void emitPUTSTATIC(String T, String I) {
    emit(JVM.PUTSTATIC, classname + "/" + I, T); 
}

private void emitICONST(int value) {
    if (value == -1)
    	emit(JVM.ICONST_M1); 
    else if (value >= 0 && value <= 5) 
    	emit(JVM.ICONST + "_" + value); 
    else if (value >= -128 && value <= 127) 
    	emit(JVM.BIPUSH, value); 
    else if (value >= -32768 && value <= 32767)
    	emit(JVM.SIPUSH, value); 
    else 
    	emit(JVM.LDC, value); 
}

private void emitFCONST(float value) {
    if(value == 0.0)
    	emit(JVM.FCONST_0); 
    else if(value == 1.0)
    	emit(JVM.FCONST_1); 
    else if(value == 2.0)
    	emit(JVM.FCONST_2); 
    else 
    	emit(JVM.LDC, value); 
}

private void emitBCONST(boolean value) {
    if (value)
    	emit(JVM.ICONST_1);
    else
    	emit(JVM.ICONST_0);
}

private String VCtoJavaType(Type t) {
    if (t.equals(StdEnvironment.booleanType))
    	return "Z";
    else if (t.equals(StdEnvironment.intType))
    	return "I";
    else if (t.equals(StdEnvironment.floatType))
    	return "F";
    else // if (t.equals(StdEnvironment.voidType))
    	return "V";
}

}
