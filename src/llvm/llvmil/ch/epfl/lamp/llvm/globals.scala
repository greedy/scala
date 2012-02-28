package ch.epfl.lamp.llvm

import scala.text.Document

class LMGlobalVariable[T<:ConcreteType](val name: String, val tpe: T, val linkage: Linkage, val visibility: Visibility, val isConstant: Boolean) {
  def declare = new LMGlobalVariableDecl(this)
  def define(v: Constant[T]) = new LMGlobalVariableDefn(this, v)
  def aliased(nm: String, tpe2: ConcreteType, l: Linkage, v: Visibility) = new LMGlobalAlias(this, nm, tpe2, l, v)
}

class LMGlobalVariableDecl(gv: LMGlobalVariable[_<:ConcreteType]) extends ModuleComp {
  def syntax = {
    val constsyn = if (gv.isConstant) "constant" else "global"
    "@\""+gv.name+"\" = external "+constsyn+" "+gv.tpe.rep
  }
}
class LMGlobalVariableDefn[T <:ConcreteType](gv: LMGlobalVariable[T], v: Constant[T]) extends ModuleComp {
  require(gv.tpe == v.tpe)
  def syntax = {
    val constsyn = if (gv.isConstant) "constant" else "global"
    "@\""+gv.name+"\" = "+gv.linkage.syntax+" "+constsyn+" "+v.tperep
  }
}
class LMGlobalAlias(gv: LMGlobalVariable[_<:ConcreteType], name: String, tpe: ConcreteType, linkage: Linkage, visibility: Visibility) extends ModuleComp {
  def syntax = {
    "@\""+name+"\" = alias "+linkage.syntax+" bitcast("+gv.tpe.pointer.rep+" @\""+gv.name+"\" to "+tpe.pointer.rep+")"
  }
}
