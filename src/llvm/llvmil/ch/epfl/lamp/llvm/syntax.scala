package ch.epfl.lamp.llvm.x {
import types._
package object syntax {
  type BlockId = LMString
  type LlvmBlocks = Seq[LlvmBlock]
  type LlvmFunctions = Seq[LlvmFunction]
  type LlvmStatements = Seq[LlvmStatement]
}

package syntax {

case class LlvmBlock(label: BlockId, statements: LlvmStatements) extends LMSource {
  def source = (Label(label)+:statements).map(_.rep)
}

case class LlvmModule(comments: Seq[LMString], types: Seq[LlvmType], constants: Seq[LMConstant], globals: Seq[LMGlobal], forwardDecls: LlvmFunctionDecls, functions: LlvmFunctions) {
  def source = {
    comments.map(c => "; " + c) ++ types.flatMap(_.source) ++ constants.flatMap(_.source) ++ globals.flatMap(_.source) ++ forwardDecls.map("declare " + _.rep) ++ functions.flatMap(_.source)
  }
}

case class LlvmFunction(decl: LlvmFunctionDecl, attributes: Seq[LlvmFuncAttr], body: LlvmBlocks) extends LMSource {
  def source = List("define " + decl.rep + " " + attributes.map(_.rep).mkString(" "),"{") ++ body.flatMap(_.source) ++ List("}")
}

sealed abstract class LlvmStatement extends LMRep
case class Assignment(dest: LlvmVar, source: LlvmExpression) extends LlvmStatement {
  def rep = dest.name + " = " + source.rep
}
case class Branch(label: LlvmVar) extends LlvmStatement {
  def rep = "br " + label.rep
}
case class BranchIf(cond: LlvmVar, targetTrue: LlvmVar, targetFalse: LlvmVar) extends LlvmStatement {
  def rep = "br " + cond.rep + ", " + targetTrue.rep + ", " + targetFalse.rep
}
case class Comment(s: LMString) extends LlvmStatement {
  def rep = "; " + s
}
case class Label(name: LMString) extends LlvmStatement {
  def rep = name + ":"
}
case class Store(value: LlvmVar, ptr: LlvmVar) extends LlvmStatement {
  def rep = "store " + value.rep + ", " + ptr.rep
}
case class Switch(scrutinee: LlvmVar, default: LlvmVar, targets: Seq[(LlvmVar,LlvmVar)]) extends LlvmStatement {
  def rep = "switch " + scrutinee.rep + ", " + default.rep + " [ " + targets.map(x => x._1.rep + ", " + x._2.rep).mkString(" ") + " ] "
}
case class Return(result: LlvmVar) extends LlvmStatement {
  def rep = result.tpe match {
    case LMVoid => "ret " + result.tpe.rep
    case _ => "ret " + result.rep
  }
}
case object Unreachable extends LlvmStatement {
  def rep = "unreachable"
}
case class Expr(expr: LlvmExpression) extends LlvmStatement {
  def rep = expr.rep
}

sealed abstract class LlvmExpression extends LMRep
case class Alloca(tp: LlvmType, amount: Int) extends LlvmExpression {
  def rep = "alloca " + tp.rep + ", " + LMLitVar(LMIntLit(amount, i32)).rep
}
case class LlvmOp(op: LlvmMachOp, left: LlvmVar, right: LlvmVar) extends LlvmExpression {
  def rep = op.rep + " " + left.tpe.rep + " " + left.name + ", " + right.name
}
case class Compare(op: LlvmCmpOp, left: LlvmVar, right: LlvmVar) extends LlvmExpression {
  def rep = {
    val mainOp = this match {
      case Compare(_, _:LMInt, _:LMInt) => "icmp"
      case Compare(_, _:LMFloatKind, _:LMFloatKind) => "fcmp"
      case _ => error("Can't compare types " + left.tpe.rep + " and " + right.tpe.rep)
    }
    mainOp + " " + op.rep + " " + left.tpe + " " + left.name + ", " + right.name
  }
}
case class Malloc(tp: LlvmType, amount: Int) extends LlvmExpression {
  def rep = "malloc " + tp.rep + ", " + LMLitVar(LMIntLit(amount, i32)).rep
}
case class Load(value: LlvmVar) extends LlvmExpression {
  def rep = "load " + value.rep
}
case class GetElemPtr(ptr: LlvmVar, indexes: Seq[Int]) extends LlvmExpression {
  def rep = "getelementptr " + ptr.rep + indexes.map(", " + i32.rep + " " + _.toString).mkString
}
case class Cast(cast: LlvmCastOp, from: LlvmVar, to: LlvmVar) extends LlvmExpression {
  def rep = cast.rep + " " + from.rep + " to " + to.rep
}
case class Call(tailJumps: LlvmCallType, fnptrval: LlvmVar, args: Seq[LlvmVar], attrs: Seq[LlvmFuncAttr]) extends LlvmExpression {
  def rep = {
    val decl = fnptrval match {
      case LMLocalVar(_,LMPointer(LMFunction(d))) => d
      case LMGlobalVar(_,LMFunction(d),_) => d
      case _ => error("call to non function")
    }
    val argTail = decl.varargs match { case VarArgs => ", ..."; case FixedArgs => "" }
    val callType = if (tailJumps == TailCall) "tail " else ""
    val argTyRep = decl.params.items.map(_.tpe.rep).mkString(", ") + argTail
    val fnty = " (" + argTyRep + ")*"
    callType + "call " + decl.cc.rep + " " + decl.returnType.rep + fnty + " " + fnptrval.name + "(" + args.map(_.rep).mkString(", ") + ")" + attrs.map(_.rep).mkString(" ")
  }
}
case class Phi(tpe: LlvmType, precessors: Seq[(LlvmVar,LlvmVar)]) extends LlvmExpression {
  def rep = {
    "phi " + tpe.rep + precessors.map(x => "[ " + x._1.name + ", " + x._2.name + " ]").mkString(", ")
  }
}

}
}
