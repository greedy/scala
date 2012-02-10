/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Geoffrey Reedy
 */

package scala.tools.nsc
package backend

import llvm.GenLLVM

trait LLVMPlatform extends JavaPlatform {
  import global._
  import definitions._

  object genLLVM extends {
    val global: LLVMPlatform.this.global.type = LLVMPlatform.this.global
    val runsAfter = List[String]("dce")
    val runsRightAfter = None
  } with GenLLVM

  override def platformPhases = List(genLLVM)

  lazy val equalsExternal = global.newTermNameCached("equalsExternal")

  override lazy val externalEquals          = getMember(BoxesRunTimeClass, equalsExternal)

}
