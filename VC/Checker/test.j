.class public VC/Checker/test
.super java/lang/Object
	
	
	; standard class static initializer 
.method static <clinit>()V
	
	
	; set limits used by this method
.limit locals 0
.limit stack 0
	return
.end method
	
	; standard constructor initializer 
.method public <init>()V
.limit stack 1
.limit locals 1
	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method
.method f()V
L0:
.var 0 is this LVC/Checker/test; from L0 to L1
	ldc "string"
L1:
	
	; return may not be present in a VC function returning void
	; The following return inserted by the VC compiler
	return
	
	; set limits used by this method
.limit locals 1
.limit stack 1
.end method
.method public static main([Ljava/lang/String;)V
L0:
.var 0 is argv [Ljava/lang/String; from L0 to L1
.var 1 is vc$ LVC/Checker/test; from L0 to L1
	new VC/Checker/test
	dup
	invokenonvirtual VC/Checker/test/<init>()V
	astore_1
	return
L1:
	return
	
	; set limits used by this method
.limit locals 2
.limit stack 2
.end method
