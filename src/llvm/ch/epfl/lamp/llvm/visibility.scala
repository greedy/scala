package ch.epfl.lamp.llvm

sealed abstract class Visibility(val syntax: String)
case object Default extends Visibility("default")
case object Hidden extends Visibility("hidden")
case object Protected extends Visibility("protected")
