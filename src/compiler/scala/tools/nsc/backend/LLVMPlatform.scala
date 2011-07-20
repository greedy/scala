/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Geoffrey Reedy
 */

package scala.tools.nsc
package backend

import llvm.{GenLLVM, LLVMPathResolver}
import io.AbstractFile
import scala.tools.util.JavaPathResolver

trait LLVMPlatform extends Platform[AbstractFile] {
  import global._
  import definitions._

  lazy val classPath  = new LLVMPathResolver(settings).result
  def rootLoader = new loaders.LLVMPackageLoader(classPath)

  object genLLVM extends {
    val global: LLVMPlatform.this.global.type = LLVMPlatform.this.global
    val runsAfter = List[String]("dce")
    val runsRightAfter = None
  } with GenLLVM

  def platformPhases = List(genLLVM)

  lazy val externalEquals = getMember(BoxesRunTimeClass, nme.equals_)

  def isMaybeBoxed(sym: Symbol) = {
    (sym == ObjectClass) ||
    (sym == JavaSerializableClass) ||
    (sym == ComparableClass) ||
    (sym isNonBottomSubClass BoxedNumberClass) ||
    (sym isNonBottomSubClass BoxedCharacterClass) ||
    (sym isNonBottomSubClass BoxedBooleanClass)
  }
}
