package ch.epfl.lamp.llvm

import scala.text.Document

class LMGlobalVariable[T<:ConcreteType](val name: String, val tpe: T, val linkage: Linkage, val visibility: Visibility, val isConstant: Boolean) {
  def declare = new LMGlobalVariableDecl(this)
  def define(v: Constant[T]) = new LMGlobalVariableDefn(this, v)
}

class LMGlobalVariableDecl(gv: LMGlobalVariable[_<:ConcreteType]) extends ModuleComp {
  def syntax = {
    val constsyn = if (gv.isConstant) "constant" else ""
    "@"+gv.name+" = "+gv.linkage.syntax+" "+constsyn+" "+gv.tpe.rep
  }
}
class LMGlobalVariableDefn[T <:ConcreteType](gv: LMGlobalVariable[T], v: Constant[T]) extends ModuleComp {
  require(gv.tpe == v.tpe)
  def syntax = {
    val constsyn = if (gv.isConstant) "constant" else ""
    "@"+gv.name+" = "+gv.linkage.syntax+" "+constsyn+" "+v.tperep
  }
}
