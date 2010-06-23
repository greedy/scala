package ch.epfl.lamp.llvm

import scala.text.Document

case class Label(name: String) {
  def tpe = LMLabel
  def rep = "%\""+name+"\""
  def tperep = tpe.rep+" "+rep
}
case class Block(label: Option[Label], instructions: Seq[Instruction]) {
  def syntax = {
    label match {
      case Some(l) => l.name+instructions.map(_.syntax).mkString(":\n  ","\n  ","")
      case None => instructions.map(_.syntax).mkString("  ","\n  ","")
    }
  }
}

abstract class LMValue[+T <: ConcreteType] {
  def tpe: T
  def rep: String
  def tperep = tpe.rep+" "+rep
}

case class LocalVariable[+T <: ConcreteType](name: String, tpe: T) extends LMValue[T] {
  def rep = "%\""+name+"\""
}

sealed abstract class Instruction {
  def syntax: String
}
object retvoid extends Instruction {
  def syntax = "ret void"
}
class ret(v: LMValue[_ <: ConcreteType]) extends Instruction {
  def syntax = "ret "+v.tperep
}
object ret {
  def apply(v: LMValue[_ <: ConcreteType]) = new ret(v)
  def void = retvoid
}
class br_cond(cond: LMValue[LMInt], l1: Label, l2: Label) extends Instruction {
  //require(cond.tpe == LMInt.i1)
  def syntax = "br "+cond.tperep+", "+l1.tperep+", "+l2.tperep
}
class br(l: Label) extends Instruction {
  def syntax = "br "+l.tperep
}
object br {
  def apply(cond: LMValue[LMInt], l1: Label, l2: Label) = new br_cond(cond, l1, l2)
  def apply(l: Label) = new br(l)
}
class switch(v: LMValue[LMInt], default: Label, dests: Seq[(LMValue[LMInt],Label)]) extends Instruction {
  def syntax = {
    def destsyntax(v: LMValue[LMInt], l: Label) = v.tperep+", "+l.tperep
    "switch "+v.tperep+", "+default.tperep+dests.map((destsyntax _).tupled).mkString(" [ "," "," ]")
  }
}
object switch {
  def apply(v: LMValue[LMInt], default: Label, dests: Seq[(LMValue[LMInt],Label)]) = new switch(v,default,dests)
}
class indirectbr(v: LMValue[LMPointer], dests: Seq[Label]) extends Instruction {
  def syntax = "indirectbr "+v.tpe.rep+" "+v.rep+dests.map(_.tperep).mkString(", [ ",", "," ]")
}
object indirectbr {
  def apply(v: LMValue[LMPointer], dests: Seq[Label]) = new indirectbr(v, dests)
}
class invoke[T<:ConcreteType](v: LocalVariable[T], funptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], normal: Label, exception: Label, cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fn_attrs: Seq[FunctionAttribute]) {
  def syntax = {
    v.tperep+" = invoke "+cconv.syntax+" "+ret_attrs.map(_.syntax).mkString(" ")+funptr.tperep+args.map(_.tperep).mkString("(",", ",")")+" "+fn_attrs.map(_.syntax).mkString(" ")+" to "+normal.tperep+" unwind "+exception.tperep
  }
}
object invoke {
  def apply[T <: ConcreteType](v: LocalVariable[T], funptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], normal: Label, exception: Label, cconv: CallingConvention = Ccc, retattrs: Seq[ReturnAttribute] = Seq.empty, fnattrs: Seq[FunctionAttribute] = Seq.empty) = new invoke(v, funptr, args, normal, exception, cconv, retattrs, fnattrs)
  def apply[T <: ConcreteType](v: LocalVariable[T], fun: LMFunction, args: Seq[LMValue[_<:ConcreteType]], normal: Label, exception: Label) = new invoke(v, new CFunctionAddress(fun), args, normal, exception, fun.cconv, fun.ret_attrs, fun.fn_attrs)
}
class invoke_void(funptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], normal: Label, exception: Label, cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fn_attrs: Seq[FunctionAttribute]) {
  def syntax = {
    "invoke "+cconv.syntax+" "+ret_attrs.map(_.syntax).mkString(" ")+funptr.tperep+args.map(_.tperep).mkString("(",", ",")")+" "+fn_attrs.map(_.syntax).mkString(" ")+" to "+normal.tperep+" unwind "+exception.tperep
  }
}
object unwind extends Instruction {
  def syntax = "unwind"
}
object unreachable extends Instruction {
  def syntax = "unreachable"
}
class binop[T<:ConcreteType](op: String, v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends Instruction {
  //require(v.tpe == op1.tpe && op1.tpe == op2.tpe)
  def syntax = v.rep+" = "+op+" "+v.tpe.rep+" "+op1.rep+", "+op2.rep
}
class add(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("add",v,op1,op2)
class add_nuw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("add nuw",v,op1,op2)
class add_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("add nsw",v,op1,op2)
class add_nuw_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("add nuw nsw",v,op1,op2)
class fadd[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends binop[T]("fadd",v,op1,op2)
class sub(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("sub",v,op1,op2)
class sub_nuw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("sub nuw",v,op1,op2)
class sub_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("sub nsw",v,op1,op2)
class sub_nuw_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("subn nuw nsw",v,op1,op2)
class fsub[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends binop[T]("fsub",v,op1,op2)
class mul(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("mul",v,op1,op2)
class mul_nuw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("mul nuw",v,op1,op2)
class mul_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("mul nsw",v,op1,op2)
class mul_nuw_nsw(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("mul nuw nsw",v,op1,op2)
class fmul[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends binop[T]("fmul",v,op1,op2)
class udiv(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("udiv",v,op1,op2)
class sdiv(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("sdiv",v,op1,op2)
class sdiv_exact(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("sdiv exact",v,op1,op2)
class fdiv[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends binop[T]("fdiv",v,op1,op2)
class urem(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("urem",v,op1,op2)
class srem(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("srem",v,op1,op2)
class frem[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[T], op1: LMValue[T], op2: LMValue[T]) extends binop[T]("frem",v,op1,op1)
class shl(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("shl",v,op1,op2)
class lshr(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("lshr",v,op1,op2)
class ashr(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("lshr",v,op1,op2)
class and(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("and",v,op1,op2)
class or(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("or",v,op1,op2)
class xor(v: LocalVariable[LMInt], op1: LMValue[LMInt], op2: LMValue[LMInt]) extends binop[LMInt]("xor",v,op1,op2)
class extractelement[T <: LMPrimitiveType with ConcreteType](v: LocalVariable[T], arg: LMValue[LMVector], idx: LMValue[LMInt]) extends Instruction {
  //require(v.tpe == arg.tpe.elementtype && idx.tpe == LMInt.i32)
  def syntax = v.rep+" = extractelement "+arg.tperep+", "+idx.tperep
}
class insertelement(v: LocalVariable[LMVector], arg: LMValue[LMVector], elt: LMValue[_<:LMPrimitiveType with ConcreteType], idx: LMValue[LMInt]) extends Instruction {
  //require(v.tpe == arg.tpe && arg.tpe.elementtype == elt.tpe && idx.tpe == LMInt.i32)
  def syntax = v.rep+" = insertelement "+arg.tperep+", "+elt.tperep+", "+idx.tperep
}
class shufflevector(v: LocalVariable[LMVector], v1: LMValue[LMVector], v2: LMValue[LMVector], mask: LMValue[LMVector]) extends Instruction {
  //require(v1.tpe == v2.tpe && v.tpe.elementtype == v1.tpe.elementtype && v.tpe.n == mask.tpe.n && mask.tpe.elementtype == LMInt.i32)
  def syntax = v.rep+" = shufflevector "+v1.tperep+", "+v2.tperep+", "+mask.tperep
}
class extractvalue(v: LocalVariable[_<:ConcreteType], arg: LMValue[_<:LMAggregateType with ConcreteType], idxs: Seq[Constant[LMInt]]) extends Instruction {
  def syntax = v.rep+" = extractvalue "+arg.tperep+", "+idxs.map(_.rep).mkString(", ")
}
class insertvalue[T <: LMAggregateType with ConcreteType](v: LocalVariable[T], arg: LMValue[T], elt: LMValue[_<:ConcreteType], idxs: Seq[Constant[LMInt]]) extends Instruction {
  def syntax = v.rep+" = insertvalue "+arg.tperep+", "+elt.tperep+", "+idxs.map(_.rep).mkString(", ")
}
class alloca(v: LocalVariable[LMPointer], tpe: ConcreteType, numelts: LMValue[LMInt] = 1:Constant[LMInt], align: Int = 0) extends Instruction {
  def syntax = v.rep+" = alloca "+tpe.rep+", "+numelts.tperep+", align "+align.toString
}
class load(v: LocalVariable[_<:ConcreteType], ptr: LMValue[LMPointer], align: Int = 0) extends Instruction {
  //require(v.tpe == ptr.tpe.target)
  def syntax = v.rep+" = load "+ptr.tperep+", align "+align.toString
}
class volatile_load(v: LocalVariable[_<:ConcreteType], ptr: LMValue[LMPointer], align: Int = 0) extends Instruction {
  //require(v.tpe == ptr.tpe.target)
  def syntax = v.rep+" = volatile load "+ptr.tperep+", align "+align.toString
}
class store(value: LMValue[_<:ConcreteType], ptr: LMValue[LMPointer], align: Int = 0) extends Instruction {
  //require(value.tpe == ptr.tpe.target)
  def syntax = "store "+value.tperep+", "+ptr.tperep
}
class volatile_store(value: LMValue[_<:ConcreteType], ptr: LMValue[LMPointer], align: Int = 0) extends Instruction {
  //require(value.tpe == ptr.tpe.target)
  def syntax = "volatile store "+value.tperep+", "+ptr.tperep
}
class getelementptr(v: LocalVariable[LMPointer], ptr: LMValue[LMPointer], idxs: Seq[LMValue[LMInt]]) extends Instruction {
  def syntax = v.rep+" = getelementptr "+ptr.tperep+", "+idxs.map(_.tperep).mkString(", ")
}
class getelementptr_inbounds(v: LocalVariable[LMPointer], ptr: LMValue[LMPointer], idxs: Seq[LMValue[LMInt]]) extends Instruction {
  def syntax = v.rep+" = getelementptr inbounds "+ptr.tperep+", "+idxs.map(_.tperep).mkString(", ")
}
abstract class convertop[T1<:ConcreteType,T2<:ConcreteType](op: String, v: LocalVariable[T1], value: LMValue[T2]) extends Instruction {
  def syntax = v.rep+" = "+op+" "+value.tperep+" to "+v.tpe.rep
}
class trunc(v: LocalVariable[LMInt], value: LMValue[LMInt]) extends convertop[LMInt,LMInt]("trunc",v,value) {
  //require(v.tpe.bits < value.tpe.bits)
}
class zext(v: LocalVariable[LMInt], value: LMValue[LMInt]) extends convertop[LMInt,LMInt]("zext",v,value) {
  //require(v.tpe.bits >= value.tpe.bits)
}
class sext(v: LocalVariable[LMInt], value: LMValue[LMInt]) extends convertop[LMInt,LMInt]("sext",v,value) {
  //require(v.tpe.bits >= value.tpe.bits)
}
class fptrunc[T1<:LMFloatingPointType with ConcreteType,T2<:LMFloatingPointType with ConcreteType](v: LocalVariable[T1], value: LMValue[T2]) extends convertop[T1,T2]("fptrunc",v,value) {
  //require(v.tpe.bits < value.tpe.bits)
}
class fpext[T1<:LMFloatingPointType with ConcreteType,T2<:LMFloatingPointType with ConcreteType](v: LocalVariable[T1], value: LMValue[T2]) extends convertop[T1,T2]("fpext",v,value) {
  //require(v.tpe.bits >= value.tpe.bits)
}
class fptoui[FT<:LMFloatingPointType with ConcreteType](v: LocalVariable[LMInt], value: LMValue[FT]) extends convertop[LMInt,FT]("fptoui",v,value)
class fptosi[FT<:LMFloatingPointType with ConcreteType](v: LocalVariable[LMInt], value: LMValue[FT]) extends convertop[LMInt,FT]("fptosi",v,value)
class uitofp[FT<:LMFloatingPointType with ConcreteType](v: LocalVariable[FT], value: LMValue[LMInt]) extends convertop[FT,LMInt]("uitofp",v,value)
class sitofp[FT<:LMFloatingPointType with ConcreteType](v: LocalVariable[FT], value: LMValue[LMInt]) extends convertop[FT,LMInt]("sitofp",v,value)
class ptrtoint(v: LocalVariable[LMInt], value: LMValue[LMPointer]) extends convertop[LMInt,LMPointer]("ptrtoint",v,value)
class inttoptr(v: LocalVariable[LMPointer], value: LMValue[LMInt]) extends convertop[LMPointer,LMInt]("inttoptr",v,value)
class bitcast[T1<:ConcreteType,T2<:ConcreteType](v: LocalVariable[T1], value: LMValue[T2]) extends convertop[T1,T2]("bitcast",v,value)
abstract class cmpop[R<:ConcreteType,T<:ConcreteType](op: String, v: LocalVariable[R], cond: Cond, op1: LMValue[T], op2: LMValue[T]) extends Instruction {
  def syntax = v.rep+" = "+op+" "+cond.name+" "+op1.tpe.rep+" "+op1.rep+", "+op2.rep
}
class icmp(v: LocalVariable[LMInt], cond: ICond, op1: LMValue[LMInt], op2: LMValue[LMInt]) extends cmpop[LMInt,LMInt]("icmp",v,cond,op1,op2) {
  //require(v.tpe == LMInt.i1 && op1.tpe == op2.tpe)
}
class icmp_p(v: LocalVariable[LMInt], cond: ICond, op1: LMValue[LMPointer], op2: LMValue[LMPointer]) extends cmpop[LMInt,LMPointer]("icmp",v,cond,op1,op2) {
  //require(v.tpe == LMInt.i1)
}
class icmp_v(v: LocalVariable[LMVector], cond: ICond, op1: LMValue[LMVector], op2: LMValue[LMVector]) extends cmpop[LMVector,LMVector]("icmp",v,cond,op1,op2) {
  //require(v.tpe.elementtype == LMInt.i1 && v.tpe.n == op1.tpe.n && op1.tpe == op2.tpe)
}
class fcmp[T <: LMFloatingPointType with ConcreteType](v: LocalVariable[LMInt], cond: FCond, op1: LMValue[T], op2: LMValue[T]) extends cmpop[LMInt,T]("fcmp",v,cond,op1,op2) {
  //require(v.tpe == LMInt.i1)
}
class fcmp_v(v: LocalVariable[LMVector], cond: FCond, op1: LMValue[LMVector], op2: LMValue[LMVector]) extends cmpop[LMVector,LMVector]("fcmp",v,cond,op1,op2) {
  //require(v.tpe.elementtype == LMInt.i1 && v.tpe.n == op1.tpe.n && op1.tpe == op2.tpe && op1.tpe.isInstanceOf[LMFloatingPointType with ConcreteType])
}
class phi[T<:ConcreteType](v: LocalVariable[T], predecessors: Seq[(Label,LMValue[T])]) extends Instruction {
  //require(predecessors.forall(_._2.tpe == v.tpe))
  def syntax = {
    v.rep+" = phi "+v.tpe.rep+predecessors.map{case (l,v) => "[ "+v.rep+", "+l.rep+" ]"}.mkString(" ",", ","")
  }
}
class select[T<:ConcreteType](v: LocalVariable[T], cond: LMValue[LMInt], v1: LMValue[T], v2: LMValue[T]) extends Instruction {
  //require(cond.tpe == LMInt.i1 && v1.tpe == v2.tpe && v.tpe == v1.tpe)
  def syntax = v.rep+" = select "+cond.tperep+", "+v1.tperep+", "+v2.tperep
}
class select_v(v: LocalVariable[LMVector], cond: LMValue[LMVector], v1: LMValue[LMVector], v2: LMValue[LMVector]) extends Instruction {
  //require(cond.tpe.elementtype == LMInt.i1 && v1.tpe == v2.tpe && v.tpe == v1.tpe && cond.tpe.n == v.tpe.n)
  def syntax = v.rep+" = select "+cond.tperep+", "+v1.tperep+", "+v2.tperep
}
class call(v: LocalVariable[_<:ConcreteType], cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fnptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], fn_attrs: Seq[FunctionAttribute]) extends Instruction {
  def this(v: LocalVariable[_<:ConcreteType], fun: LMFunction, args: Seq[LMValue[_<:ConcreteType]]) = this(v, fun.cconv, fun.ret_attrs, new CFunctionAddress(fun), args, fun.fn_attrs)
  def syntax = v.rep+" = call "+cconv.syntax+ret_attrs.map(_.syntax).mkString(" "," "," ")+fnptr.tpe.target.asInstanceOf[LMFunctionType].returnType.rep+" "+fnptr.rep+args.map(_.tperep).mkString("(",", ",")")+fn_attrs.map(_.syntax).mkString(" "," ","")
}
class tail_call(v: LocalVariable[_<:ConcreteType], cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fnptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], fn_attrs: Seq[FunctionAttribute]) extends Instruction {
  def this(v: LocalVariable[_<:ConcreteType], fun: LMFunction, args: Seq[LMValue[_<:ConcreteType]]) = this(v, fun.cconv, fun.ret_attrs, new CFunctionAddress(fun), args, fun.fn_attrs)
  def syntax = v.rep+" = tail call "+cconv.syntax+ret_attrs.map(_.syntax).mkString(" "," "," ")+fnptr.tpe.target.asInstanceOf[LMFunctionType].returnType.rep+" "+fnptr.rep+args.map(_.tperep).mkString("(",", ",")")+fn_attrs.map(_.syntax).mkString(" "," ","")
}
class call_void(cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fnptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], fn_attrs: Seq[FunctionAttribute]) extends Instruction {
  def this(fun: LMFunction, args: Seq[LMValue[_<:ConcreteType]]) = this(fun.cconv, fun.ret_attrs, new CFunctionAddress(fun), args, fun.fn_attrs)
  def syntax = "call "+cconv.syntax+ret_attrs.map(_.syntax).mkString(" "," "," ")+fnptr.tpe.target.asInstanceOf[LMFunctionType].returnType.rep+" "+fnptr.rep+args.map(_.tperep).mkString("(",", ",")")+fn_attrs.map(_.syntax).mkString(" "," ","")
}
class tail_call_void(cconv: CallingConvention, ret_attrs: Seq[ReturnAttribute], fnptr: LMValue[LMPointer], args: Seq[LMValue[_<:ConcreteType]], fn_attrs: Seq[FunctionAttribute]) extends Instruction {
  def this(fun: LMFunction, args: Seq[LMValue[_<:ConcreteType]]) = this(fun.cconv, fun.ret_attrs, new CFunctionAddress(fun), args, fun.fn_attrs)
  def syntax = "tail call "+cconv.syntax+ret_attrs.map(_.syntax).mkString(" "," "," ")+fnptr.tpe.target.asInstanceOf[LMFunctionType].returnType.rep+" "+fnptr.rep+args.map(_.tperep).mkString("(",", ",")")+fn_attrs.map(_.syntax).mkString(" "," ","")
}
class va_arg(v: LocalVariable[_<:ConcreteType], list: LMValue[LMPointer]) extends Instruction {
  def syntax = v.rep+" = va_arg "+list.tperep+", "+v.tpe
}
