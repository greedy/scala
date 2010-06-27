/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2003-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */



package scala

/** Specify LLVM implementation for method.
  *
  * {{{
  * @llvmimpl(...) def f(x: Int, y: List[Long]): String = ...
  * }}}
  *
  * LLVM assembly code provided as annotation argument is emitted as method
  * body instead of generating code from scala.
  *
  */
class llvmimpl(s: String) extends StaticAnnotation {}
