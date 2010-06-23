package ch.epfl.lamp.llvm

case class ArgSpec(lmvar: LocalVariable[_ <: ConcreteType], attrs: Seq[ParameterAttribute] = Seq.empty) {
  def declSyn = {
    lmvar.tpe.rep+attrs.map(_.syntax).mkString(" "," ","").trim
  }
  def defnSyn = {
    lmvar.tperep+attrs.map(_.syntax).mkString(" "," ","").trim
  }
}
object ArgSpec {
  implicit def local2arg(lvar: LocalVariable[_ <: ConcreteType]) = new ArgSpec(lvar)
}

class LMFunction(val resultType: ConcreteType, val name: String, val args: Seq[ArgSpec], val varargs: Boolean, val linkage: Linkage, val visibility: Visibility, val cconv: CallingConvention, val ret_attrs: Seq[ReturnAttribute], val fn_attrs: Seq[FunctionAttribute], val section: Option[String], val align: Option[Int], val gc: Option[String]) {
  def tpe = new LMFunctionType(resultType, args.map(_.lmvar.tpe), varargs)
  def declare = new LMFunctionDecl(this)
  def define(blocks: Seq[Block]) = new LMFunctionDef(this, blocks)
}

class LMFunctionDecl(val fun: LMFunction) extends ModuleComp {
  def syntax = {
    val theargs = if (fun.varargs) fun.args.map(_.declSyn):+"..." else fun.args.map(_.declSyn)
    val sectionSyn = fun.section.map("section \""+_+"\"")
    val alignSyn = fun.align.map("align "+_.toString)
    val gcSyn = fun.section.map("gc \""+_+"\"")
    val nameAndArgs = "@\""+fun.name+"\""+theargs.mkString("(",",",")")
    (Seq("declare",fun.linkage.syntax,fun.visibility.syntax,fun.cconv.syntax)++fun.ret_attrs.map(_.syntax)++Seq(fun.resultType.rep,nameAndArgs)++fun.fn_attrs.map(_.syntax)++sectionSyn++alignSyn++gcSyn).mkString(" ")
  }
}
class LMFunctionDef(val fun: LMFunction, val blocks: Seq[Block]) extends ModuleComp {
  def syntax = {
    val theargs = if (fun.varargs) fun.args.map(_.defnSyn):+"..." else fun.args.map(_.defnSyn)
    val sectionSyn = fun.section.map("section \""+_+"\"")
    val alignSyn = fun.align.map("align "+_.toString)
    val gcSyn = fun.section.map("gc \""+_+"\"")
    val nameAndArgs = "@\""+fun.name+"\""+theargs.mkString("(",",",")")
    (Seq("define",fun.linkage.syntax,fun.visibility.syntax,fun.cconv.syntax)++fun.ret_attrs.map(_.syntax)++Seq(fun.resultType.rep,nameAndArgs)++fun.fn_attrs.map(_.syntax)++sectionSyn++alignSyn++gcSyn).mkString(" ") + blocks.map(_.syntax).mkString("{\n","\n  ","\n}")
  }
}
