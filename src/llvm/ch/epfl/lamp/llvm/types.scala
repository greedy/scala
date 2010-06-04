package ch.epfl.lamp.llvm {

package object types {
  /** A String in LLVM */
  type LMString = String;
  type LlvmFunctionDecls = Seq[LlvmFunctionDecl];

  val i128 = LMInt(128);
  val i64 = LMInt(64);
  val i32 = LMInt(32);
  val i16 = LMInt(16);
  val i8 = LMInt(8);
  val i1 = LMInt(1);
  /* XXX - no good */
  val llvmWord = i32;
  val llvmWordPtr = i32.pointer;
}

package types {

trait LMSource {
  def source: Seq[String]
}

trait LMRep {
  def rep: String;
}

trait LMTyped {
  def tpe: LlvmType;
}

case class LMGlobal(v: LMGlobalVar, s: Option[LlvmStatic]) extends LMTyped with LMSource {
  def tpe = v.tpe;
  def source = {
    this match {
      case LMGlobal(LMGlobalVar(_,LMPointer(ptype),link), None) =>
	List(v.name + " = " + link.rep + " global " + ptype.rep)
      case LMGlobal(LMGlobalVar(_,_,link), Some(stat)) =>
	List(v.name + " = " + link.rep + " global " + stat.rep)
    }
  }
}

case class LMConstant(v: LMGlobalVar, s: LlvmStatic) extends LMTyped with LMSource {
  def tpe = v.tpe;
  def source =  List(v.name + " = " + v.linkage.rep + " constant " + s.rep)
}

/** Llvm Types */
sealed abstract class LlvmType extends LMRep with LMTyped with LMSource {
  def tpe = this;
  def pointer = LMPointer(this);
  def width: Int;
  def source = Seq.empty[String]
}
/** An integer with a given width in bits. */
case class LMInt(width: Int) extends LlvmType {
  def rep = "i" + width.toString
}
trait LMFloatKind extends LlvmType
sealed abstract trait CanFloatLit
/** 32 bit floating point */
case object LMFloat extends LlvmType with CanFloatLit with LMFloatKind {
  def rep = "float"
  def width = 32
}
/** 64 bit floating point */
case object LMDouble extends LlvmType with CanFloatLit with LMFloatKind {
  def rep = "double"
  def width = 64
}
/** 80 bit (x86 only) floating point */
case object LMFloat80 extends LlvmType with LMFloatKind {
  def rep = "x86_fp80"
  def width = 80
}
/** 128 bit floating point */
case object LMFloat128 extends LlvmType with LMFloatKind {
  def rep = "fp128"
  def width = 128
}
/** A pointer to a LlvmType */
case class LMPointer(ptype: LlvmType) extends LlvmType {
  def rep = ptype.rep + "*"
  def width = llvmWord.width
}
/** An array of LlvmType */
case class LMArray(size: Int, etype: LlvmType) extends LlvmType {
  def rep = "[" + size.toString + " x " + etype.rep + "]"
  def width = llvmWord.width
}
/** A LlvmVar can represent a label (address) */
case object LMLabel extends LlvmType {
  def rep = "label"
  override def pointer = error("Pointers to labels are invalid")
  def width = 0
}
/** Void type */
case object LMVoid extends LlvmType {
  def rep = "void"
  override def pointer = error("Pointers to void are invalid")
  def width = 0
}
/** Structure type */
case class LMStruct(elements: LlvmType*) extends LlvmType {
  def rep = elements.map(_.rep).mkString("{",",","}")
  def width = elements.map(_.width).sum
}
/** A type alias */
case class LMAlias(name: LMString, atype: LlvmType) extends LlvmType {
  def rep = "%" + name
  def width = atype.width
  override def source = List(this.rep + " = type " + atype.rep)
}
/** Function type, used to create pointer to functions */
case class LMFunction(decl: LlvmFunctionDecl) extends LlvmType {
  def rep = decl match {
    case LlvmFunctionDecl(_,_,_,r,VarArgs,p) =>
      r.rep + " (" + p.items.map(_.tpe.rep).mkString(",") + ", ...)"
    case LlvmFunctionDecl(_,_,_,r,FixedArgs,p) =>
      r.rep + " (" + p.items.map(_.tpe.rep).mkString(",") + ")"
  }
  override def source = List("declare " + decl.rep)
  def width = 0
}

/** Llvm Variables */
sealed abstract class LlvmVar extends LMRep with LMTyped {
  /** Return the variable name or value of the LlvmVar in Llvm IR textual representation (e.g. @x, %y or 42) */
  def name: LMString
  /** Return the variable name or value of the LlvmVar in a plain textual representation (e.g. x, y or 42) */
  def plainName: LMString
  def deref: LlvmVar
}
/** Variables with a global scope */
case class LMGlobalVar(plainName: LMString, tpe: LlvmType, linkage: LlvmLinkageType) extends LlvmVar {
  def rep = tpe.rep + " " + name
  def name = "@" + plainName
  def deref = tpe match {
    case LMPointer(pt) => LMGlobalVar(plainName, pt, linkage)
    case _ => error("Variable type " + tpe.rep + " is not a pointer")
  }
}
/** Variables local to a function or parameters */
case class LMLocalVar(plainName: LMString, tpe: LlvmType) extends LlvmVar {
  def rep = tpe.rep + " " + name
  def name = "%" + plainName
  def deref = tpe match {
    case LMPointer(pt) => LMLocalVar(plainName, pt)
    case _ => error("Variable type " + tpe.rep + " is not a pointer")
  }
}
/** A constant variable */
case class LMLitVar(lit: LlvmLit) extends LlvmVar {
  def rep = lit.rep
  def tpe = lit.tpe
  def name = lit.lit
  def plainName = name
  def deref = error("Cannot dereference literals")
}

/** Llvm Literal Data. This can be used inline in expressions. */
sealed abstract class LlvmLit extends LMRep with LMTyped  {
  /** Literal value without a type */
  def lit: LMString
  def rep = tpe.rep + " " + lit
}
/** Refers to an integer constant (i64 42). */
case class LMIntLit(value: Long, tpe: LlvmType) extends LlvmLit {
  def lit = value.toString
}
/** Floating point literal */
case class LMFloatLit(value: Double, tpe: LlvmType with CanFloatLit) extends LlvmLit {
  def lit = tpe match {
    case LMFloat => "0x" + java.lang.Integer.toHexString(java.lang.Float.floatToRawIntBits(value.asInstanceOf[Float]))
    case LMDouble => "0x" + java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(value))
  }
}

/** Llvm Static Data. These represent the possible global level variables and constants */
sealed abstract class LlvmStatic extends LMRep
/** A comment in a static section */
case class LMComment(s: LMString) extends LlvmStatic {
  def rep = "; " + s
}
/** A static variant of a literal value */
case class LMStaticLit(l: LlvmLit) extends LlvmStatic with LMTyped {
  def rep = l.rep
  def tpe = l.tpe
}
/** For uninitialized data */
case class LMUninitType(tpe: LlvmType) extends LlvmStatic with LMTyped {
  def rep = tpe.rep + " undef"
}
/** Defines a static LMString */
case class LMStaticStr(s: LMString, tpe: LlvmType) extends LlvmStatic with LMTyped {
  def rep = tpe.rep + " c\"" + s + "\\00\""
}
/** A static array */
case class LMStaticArray(values: Seq[LlvmStatic], tpe: LlvmType) extends LlvmStatic with LMTyped {
  def rep = tpe.rep + " " + values.mkString("[",",","]")
}
/** A static structure type */
case class LMStaticStruc(values: Seq[LlvmStatic], tpe: LlvmType) extends LlvmStatic with LMTyped {
  def rep = tpe.rep + " " + values.mkString("{",",","}")
}
/** A pointer to other data */
case class LMStaticPointer(v: LlvmVar) extends LlvmStatic with LMTyped {
  def rep = v.rep
  def tpe = v.tpe
}
/* Static expressions, could split out but leave for moment for ease of use. Not many of them. */
/** Pointer to Integer conversion */
case class LMPtoI(v: LlvmStatic, tpe: LlvmType) extends LlvmStatic with LMTyped {
  def rep = tpe.rep + " ptrtoint (" + v.rep + " to " + tpe.rep + ")"
}
/** Constant addition operation */
case class LMAdd(s1: LlvmStatic with LMTyped, s2: LlvmStatic with LMTyped) extends LlvmStatic {
  require(s1.tpe == s2.tpe, "LMAdd with different types! s1: " + s1 + " s2: " + s2)
  def tpe = s1.tpe
  def rep = tpe.rep + " add (" + s1.rep + "," + s2.rep + ")"
}
/** Constant subtraction operation */
case class LMSub(s1: LlvmStatic with LMTyped, s2: LlvmStatic with LMTyped) extends LlvmStatic {
  require(s1.tpe == s2.tpe, "LMSub with different types! s1: " + s1 + " s2: " + s2)
  def tpe = s1.tpe
  def rep = tpe.rep + " sub (" + s1.rep + "," + s2.rep + ")"
}

object LlvmFunctionDeclParams {
  implicit def typeParams(tpes: Seq[LlvmType]) = LlvmTypeParams(tpes: _*)
  implicit def varParams(vars: Seq[LMLocalVar]) = LlvmVarParams(vars: _*)
}
sealed abstract class LlvmFunctionDeclParams {
  def items: Seq[LMTyped with LMRep]
}
case class LlvmTypeParams(tpes: LlvmType*) extends LlvmFunctionDeclParams {
  def items = tpes
}
case class LlvmVarParams(vars: LMLocalVar*) extends LlvmFunctionDeclParams {
  def items = vars
}
/** An LLVM Function */
case class LlvmFunctionDecl(
  /** Unique Identifier of the function */
  name: LMString,
  /** LinkageType of the function */
  linkage: LlvmLinkageType,
  /** The calling convetion of the function */
  cc: LlvmCallConvention,
  /** Type of the returned value */
  returnType: LlvmType,
  /** Indicates if this function uses varargs */
  varargs: LlvmParameterListType,
  /** Signature of the parameters, can be just types or full vars */
  params: LlvmFunctionDeclParams
) extends LMRep {
  def rep = varargs match {
    case VarArgs => linkage.rep + " " + cc.rep + " " + returnType.rep + " @" + name + "(" + params.items.map(_.rep).mkString(",") + ", ...)"
    case FixedArgs => linkage.rep + " " + cc.rep + " " + returnType.rep + " @" + name + "(" + params.items.map(_.rep).mkString(",") + ")"
  }
}

/**
 * Llvm Function Attributes.
 *
 * Function attributes are set to communicate additional information about a
 * function. Function attributes are considered to be part of the function, not
 * of the function type, so functions with different parameter attributes can
 * have the same function type. Functions can have multiple attributes.
 */
sealed abstract class LlvmFuncAttr(val rep: LMString) extends LMRep
/**
 * This attribute indicates that the inliner should attempt to inline this
 * function into callers whenever possible, ignoring any active inilining size
 * threshold for this caller.
 */
case object AlwaysInline extends LlvmFuncAttr("alwaysinline")
/**
 * This attribute indicates that the source code contained a hint that
 * inlining this function is desirable (such as the "inline" keyword in C/C++).
 * It is just a hint; it imposes no requirements on the inliner.
 */
case object InlineHint extends LlvmFuncAttr("inlinehint")
/**
 * This attribute indicates that the inliner should never inline this function
 * in any situation. This attribute may not be used tongether with the
 * alwaysinline attribute
 */
case object NoInline extends LlvmFuncAttr("noinline")
/**
 * This attribute suggests that optimization passes and code generator passes
 * make choices that keep the size of this function low, and otherwise do
 * optmizations specifically to reduce code size.
 */
case object OptSize extends LlvmFuncAttr("optsize")
/**
 * This function attribute indicates that the function never returns normally.
 * This produces undefined behavior at runtime if the function ever does
 * dynamically return.
 */
case object NoReturn extends LlvmFuncAttr("noreturn")
/**
 * This function attribute indicates that the function never returns with an
 * unwind or exceptional control flow. If the function does unwind, its runtime
 * behavior is undefined.
 */
case object NoUnwind extends LlvmFuncAttr("nounwind")
/**
 * This attribute indicates that the function computes its result (or decides
 * to unwind an exception) based strictly on its arguments, without
 * dereferencing any pointer arguments or otherwise accessing any mutable state
 * (e.g. memory, control register, etc) visible to caller functions. It does
 * not write through any pointer arguments (including byval arugments) and
 * never changes any state visible to callers. This means that it cannot unwind
 * exceptions by calling the C++ exception throwing methods, but could use the
 * unwind instruction.
 */
case object ReadNone extends LlvmFuncAttr("readnon")
/**
 * This attribute indicates that the function does not write through any
 * pointer arguments (including byval arguments) or otherwise modify any state
 * (e.g. memory, control registers, etc) visible to caller functions. It may
 * dereference pointer arguments and read state that may be set in the caller.
 * A readonly function always returns the same value (or unwinds and exception
 * identically) when called with the same set of arguments and global state. It
 * cannot unwind an exception by calling the C++ exception throwing methods,
 * but may use the unwind instruction.
 */
case object ReadOnly extends LlvmFuncAttr("readonly")
/**
 * This attribute indicates that the function should emit a stack smashing
 * protector. It is in the form of a "canary"-a random value placed on the
 * stack before the local variables that's checked upon return from the
 * function to see if it has been overwritten. A heuristic is used to determine
 * if a function needs stack protectors or not.
 *
 * If a function that as an ssp attribute is inlined into a function that
 * doesn't have an ssp attribute, then the resulting function will have an ssp
 * attribute.
 */
case object Ssp extends LlvmFuncAttr("ssp")
/**
 * This attribute indicates that the function should always emit a stack
 * smashing protector. This overrides the ssp function attribute.
 *
 * If a function that has an sspreq attribute is inlined into a function that
 * doesn't have an sspreq attribute or which has an ssp attribute, then the
 * resulting function will have an sspreq attribute.
 */
case object SspReq extends LlvmFuncAttr("sspreq")
/**
 * This attribute indicates that the code generator should not use a red zone,
 * even if the target-specific ABI normally permits it.
 */
case object NoRedZone extends LlvmFuncAttr("noredzone")
/**
 * This attribute disables implicit floating point instructions.
 */
case object NoImplicitFloat extends LlvmFuncAttr("noimplicitfloat")
/**
 * This attribute disables prologue / epilogue emission for the function. This
 * can have very system-specific consequences.
 */
case object Naked extends LlvmFuncAttr("naked")

/**
 * Different types to call a function.
 */
sealed abstract class LlvmCallType
/** Normal call, allocate a new stack frame. */
case object StdCall extends LlvmCallType
/** Tail call, perform the call in the current stack frame. */
case object TailCall extends LlvmCallType

/**
 * Different calling conventions a function can use.
 */
sealed abstract class LlvmCallConvention extends LMRep
sealed abstract class NamedCallConvention(val rep: LMString) extends LlvmCallConvention
/**
 * The C calling convention.
 * This calling convention (the default if no other calling convention is
 * specified) mtches the target C calling conventions. This calling convention
 * supports varargs function calls and tolerates some mismatch in the declared
 * prototype and implemented declaration of the function (as does normal C).
 */
case object CC_Ccc extends NamedCallConvention("ccc")
/**
 * This calling convetion attempts to make calls as fast as possible (e.g. by
 * passing things in registers). This calling convetion allows the target to
 * use whatever tricks it wants to produce fast code for the target, without
 * having to conform to an externally specified ABI (Application Binary
 * Interface). Implementations of this convention should allow arbitrary tail
 * call optimization to be supported. This calling convention does not support
 * varargs and requires the prototype of all callees to exactly match the
 * prototype of the function definition.
 */
case object CC_Fastcc extends NamedCallConvention("fastcc")
/**
 * This calling convention attempts to make code in the caller as efficient as
 * possible under the assumption that the call is not commonly executed. As
 * such, these calls often preserve all registers so that the call does not
 * break any live ranges in the caller side. This calling convention does not
 * support varargs and requires the prototype of all callees to exactly match
 * the prototype of the function definition.
 */
case object CC_Coldcc extends NamedCallConvention("coldcc")
/**
 * Any calling convention may be specified by number, allowing target-specific
 * calling conventions to be used. Target specific calling conventions start
 * at 64.
 */
case class CC_Ncc(n: Int) extends LlvmCallConvention {
  def rep = "cc " + n.toString
}
/**
 * X86 Specific 'StdCall' convention. LLVM includes a specific alias for it
 * rather than just using CC_Ncc.
 */
case object CC_X86_Stdcc extends NamedCallConvention("x86_stdcallcc")

/** Functions can have a fixed amount of parameters, or a variable amount */
sealed abstract class LlvmParameterListType
/** Fixed amount of arguments */
case object FixedArgs extends LlvmParameterListType
/** Variable amount of arguments */
case object VarArgs extends LlvmParameterListType

/**
 * Linkage type of a symbol.
 */
sealed abstract class LlvmLinkageType(val rep: LMString) extends LMRep
/**
 * Global values with internal linkage are only directly accessible by objects
 * in the current module. In particular, linking code into a module with an
 * internal global value may cause the internal to be renamed as necessary to
 * avoid collisions. Because the symbol is internal to the module, all
 * references can be updated. This corresponds to the notion of the static
 * keyword in C.
 */
case object Internal extends LlvmLinkageType("internal")
/**
 * Globals with the linkonce linkage are merged with other globals of the same
 * name when linkage occurs. This is typically used to implement inline
 * functions, templates, or other code which must be generated in each
 * translation unit that uses it. Unreferenced linkonce globals are allowed to
 * be discarded.
 */
case object LinkOnce extends LlvmLinkageType("linkonce")
/**
 * weak linkage is exactly the same as linkone linkage, except that the
 * unreferenced weak globals may not be discarded. This is used for globals
 * that may be emitted in multiple translation units, but that are not
 * guaranteed to be emitted into every translation unit that uses them. One
 * example of this are common globals in C, such as 'int X;' at global scope.
 */
case object Weak extends LlvmLinkageType("weak")
/**
 * appending linkage may only be applied to global variables of pointer to
 * array type. When two global variables with appending linkage are linked
 * together, the two global arrays are appended together. This is the Llvm,
 * typesafe, equivalent of having the system linker append together sections
 * with identical names when .o files are linked.
 */
case object Appending extends LlvmLinkageType("appending")
/**
 * The semantics of this linkage follow the ELF model: the symbol is weak
 * until links, if not linked, the symbol becomes null instead of being an
 * undefined reference.
 */
case object ExternWeak extends LlvmLinkageType("extern_weak")
/**
 * The symbol participates in linkage and can be used to resolve external
 * symbol references.
 */
case object ExternallyVisible extends LlvmLinkageType("")
/**
 * Alias for ExternallyVisible but with explicit textual form in LLVM
 * assembly.
 */
case object External extends LlvmLinkageType("external")

/** Llvm binary operators machine operations. */
sealed abstract class LlvmMachOp(mnemonic: LMString) extends LMRep {
  def rep = mnemonic
}
/** Add two integer, floating point or vector values */
case object LM_MO_Add extends LlvmMachOp("add")
/** Subtract two integer, floating point or vector values */
case object LM_MO_Sub extends LlvmMachOp("sub")
/** Multiply two integer, floating point or vector values */
case object LM_MO_Mul extends LlvmMachOp("mul")
/** Unsigned integer or vector division */
case object LM_MO_UDiv extends LlvmMachOp("udiv")
/** Signed integer or vector division */
case object LM_MO_SDiv extends LlvmMachOp("sdiv")
/** Floating point or vector division */
case object LM_MO_FDiv extends LlvmMachOp("fdiv")
/** Unsigned integer or vector remainder (mod) */
case object LM_MO_URem extends LlvmMachOp("urem")
/** Signed integer or vector remainder (mod) */
case object LM_MO_SRem extends LlvmMachOp("srem")
/** Floating point or vector remainder (mod) */
case object LM_MO_FRem extends LlvmMachOp("frem")
/** Left shift */
case object LM_MO_Shl extends LlvmMachOp("shl")
/** Logical shift right. Shift right, filling with zero */
case object LM_MO_LShr extends LlvmMachOp("lshr")
/** Arithmetic shift right. The most significant bits of the result will be
 * equal to the sign bit of the left operand */
case object LM_MO_AShr extends LlvmMachOp("ashr")
/** AND bitwise logical operation */
case object LM_MO_And extends LlvmMachOp("and")
/** OR bitwise logical operation */
case object LM_MO_Or extends LlvmMachOp("or")
/** XOR bitwise logical operation */
case object LM_MO_Xor extends LlvmMachOp("xor")

/** Llvm compare operations. */
sealed abstract class LlvmCmpOp(val rep: LMString) extends LMRep
/** Equal (Signed and Unsigned) */
case object LM_CMP_Eq extends LlvmCmpOp("eq")
/** Not equal (Signed and Unsigned) */
case object LM_CMP_Ne extends LlvmCmpOp("ne")
/** Unsigned greater than */
case object LM_CMP_Ugt extends LlvmCmpOp("ugt")
/** Unsigned greater than or equal */
case object LM_CMP_Uge extends LlvmCmpOp("uge")
/** Unsigned less than */
case object LM_CMP_Ult extends LlvmCmpOp("ult")
/** Unsigned less than or equal */
case object LM_CMP_Ule extends LlvmCmpOp("ule")
/** Signed greater than */
case object LM_CMP_Sgt extends LlvmCmpOp("sgt")
/** Signed greater than or equal */
case object LM_CMP_Sge extends LlvmCmpOp("sge")
/** Signed less than */
case object LM_CMP_Slt extends LlvmCmpOp("slt")
/** Signed less than or equal */
case object LM_CMP_Sle extends LlvmCmpOp("sle")
/** Float equal */
case object LM_CMP_Feq extends LlvmCmpOp("oeq")
/** Float not equal */
case object LM_CMP_Fne extends LlvmCmpOp("une")
/** Float greater than */
case object LM_CMP_Fgt extends LlvmCmpOp("ogt")
/** Float greater than or equal */
case object LM_CMP_Fge extends LlvmCmpOp("oge")
/** Float less than */
case object LM_CMP_Flt extends LlvmCmpOp("olt")
/** Float less than or equal */
case object LM_CMP_Fle extends LlvmCmpOp("ole")

/** Llvm case operations. */
sealed abstract class LlvmCastOp(val rep: LMString) extends LMRep
/** Integer truncate */
case object LM_Trunc extends LlvmCastOp("trunc")
/** Integer extend (zero fill) */
case object LM_Zext extends LlvmCastOp("zext")
/** Integer extend (sign fill) */
case object LM_Sext extends LlvmCastOp("sext")
/** Float truncate */
case object LM_Fptruc extends LlvmCastOp("fptrunc")
/** Float extend */
case object LM_Fpext extends LlvmCastOp("fpext")
/** Float to unsigned integer */
case object LM_Fptoui extends LlvmCastOp("fptoui")
/** Float to signed integer */
case object LM_Fptosi extends LlvmCastOp("fptosi")
/** Unsigned integer to float */
case object LM_Uitofp extends LlvmCastOp("uitofp")
/** Signed integer to float */
case object LM_Sitofp extends LlvmCastOp("sitofp")
/** Pointer to integer */
case object LM_Ptrtoint extends LlvmCastOp("ptrtoint")
/** Integer to pointer */
case object LM_Inttoptr extends LlvmCastOp("inttoptr")
/** Cast between types where no bit manipulation is needed */
case object LM_Bitcast extends LlvmCastOp("bitcast")

}
}
