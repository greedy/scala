package scala.tools.nsc
package backend.llvm

import scala.tools.util.PathResolver
import util.ClassPath.ClassPathContext
import util.{ClassPath, DirectoryClassPath, MergedClassPath}
import io.AbstractFile

class LLVMContext extends ClassPathContext[AbstractFile] {

  private def endsClass(s: String) =
    s.length > 6 && s.substring(s.length - 6) == ".class"

  private def endsSym(s: String) =
    s.length > 4 && s.substring(s.length - 4) == ".sym"

  override def validClassFile(name: String) = {
    (endsClass(name) || endsSym(name)) && isValidName(name)
  }

  override def toBinaryName(file: AbstractFile) = {
    val name = file.name
    if(endsClass(name)) name.substring(0, name.length - 6)
    else {
      assert(endsSym(name), name)
      name.substring(0, name.length - 4)
    }
  }

  override def newClassPath(dir: AbstractFile) =
    new DirectoryClassPath(dir, this)
}

class LLVMClassPath(containers: IndexedSeq[ClassPath[AbstractFile]],
                    context: LLVMContext)
extends MergedClassPath[AbstractFile](containers, context)

class LLVMPathResolver(settings: Settings)
extends PathResolver[LLVMContext](settings, new LLVMContext) {
  def newClassPath(containers: IndexedSeq[ClassPath[AbstractFile]],
                   context: LLVMContext) = {
    new LLVMClassPath(containers, context)
  }
}
