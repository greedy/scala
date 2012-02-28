package ch.epfl.lamp.llvm

class Module(comps: Seq[ModuleComp]) {
  def syntax = comps.map(_.syntax).mkString("\n")
}
trait SourceElement {
  def syntax: String
}
trait ModuleComp extends SourceElement

class Comment(s: String) extends ModuleComp {
  def syntax = "; "+s
}

class TypeAlias(at: AliasedType) extends ModuleComp {
  def syntax = at.rep + " = type " + at.realrep
}
