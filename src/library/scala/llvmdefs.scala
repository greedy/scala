/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala

/** Specify extra definitions to be included in LLVM output.
  *
  * {{{
  * @llvmdefs(...) class Foo { ... }
  * }}}
  *
  * LLVM assembly code provided as annotation argument is emitted as method
  * body instead of generating code from scala.
  *
  */
class llvmdefs(s: String) extends StaticAnnotation {}
