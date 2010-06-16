package ch.epfl.lamp.llvm

sealed abstract class CallingConvention(val syntax: String)
case object Ccc extends CallingConvention("ccc")
case object Fastcc extends CallingConvention("fastcc")
case object Coldcc extends CallingConvention("coldcc")
case class Cc(n: Int) extends CallingConvention("cc "+n.toString)
