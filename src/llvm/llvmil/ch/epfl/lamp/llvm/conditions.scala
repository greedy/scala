package ch.epfl.lamp.llvm

sealed abstract class Cond {
  def name: String
}

case class ICond(name: String) extends Cond
object ICond {
  val ieq = ICond("eq")
  val ine = ICond("ne")
  val ugt = ICond("ugt")
  val uge = ICond("uge")
  val ult = ICond("ult")
  val ule = ICond("ule")
  val sgt = ICond("sgt")
  val sge = ICond("sge")
  val slt = ICond("slt")
  val sle = ICond("sle")
}

case class FCond(name: String) extends Cond
object FCond {
  val fals = FCond("false")
  val oeq = FCond("oeq")
  val ogt = FCond("ogt")
  val oge = FCond("oge")
  val olt = FCond("olt")
  val ole = FCond("ole")
  val one = FCond("one")
  val ord = FCond("ord")
  val ueq = FCond("ueq")
  val ugt = FCond("ugt")
  val uge = FCond("uge")
  val ult = FCond("ult")
  val ule = FCond("ule")
  val une = FCond("une")
  val uno = FCond("uno")
  val tru = FCond("true")
}
