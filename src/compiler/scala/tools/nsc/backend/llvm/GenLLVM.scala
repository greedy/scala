/* NSC -- new Scala compiler
 * Copyright 2005-2010 LAMP/EPFL
 * @author  Geoffrey Reedy
 */

package scala.tools.nsc
package backend.llvm

import scala.collection._

import ch.epfl.lamp.llvm.{Instruction => LMInstruction, Constant => LMConstant, Block => LMBlock, _}

import io.AbstractFile
import java.io.OutputStreamWriter

abstract class GenLLVM extends SubComponent {
  import global._
  import icodes._
  import icodes.opcodes._

  val phaseName = "llvm"

  override def newPhase(p: Phase) = new LLVMPhase(p)

  class LLVMPhase(prev: Phase) extends ICodePhase(prev) {
    def name = phaseName

    override def run {
      classes.values foreach apply
    }

    object moduleGenerator extends ModuleGenerator

    override def apply(cls: IClass) {
      moduleGenerator.genClass(cls)
    }
  }


  class ModuleGenerator {

    val LlvmimplAnnotSym = definitions.getClass("scala.llvmimpl")
    val LlvmdefsAnnotSym = definitions.getClass("scala.llvmdefs")

    def fields(s: Symbol) = {
      classes.get(s).map(_.fields)
    }

    object Runtime {
      val rtIfaceRef: LMStructure with AliasedType = new LMStructure(Seq(rtObject.pointer, rtVtable)).aliased(".ifaceref")
      val rtVtable = LMInt.i8.pointer.pointer.aliased(".vtable")
      val rtIfaceInfo = new LMStructure(Seq(rtClass.pointer, rtVtable)).aliased(".ifaceinfo")
      val rtClass: LMStructure with AliasedType = new LMStructure(Seq(LMInt.i8.pointer, LMInt.i32, rtClass.pointer, rtVtable, LMInt.i32, new LMArray(0, rtIfaceInfo))).aliased(".class")
      val rtObject = new LMStructure(Seq(rtClass.pointer)).aliased(".object")
      val rtDispatch = new LMStructure(Seq(rtClass.pointer, LMInt.i8.pointer)).aliased(".dispatch")
      val rtNew = new LMFunction(rtObject.pointer, ".rt.new", Seq(ArgSpec(new LocalVariable("cls", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtInitobj= new LMFunction(rtObject.pointer, ".rt.initobj", Seq(ArgSpec(new LocalVariable("object", rtObject.pointer)), ArgSpec(new LocalVariable("cls", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtGetClass = new LMFunction(rtClass.pointer, ".rt.get_class", Seq(ArgSpec(new LocalVariable("object", rtObject.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtLookupMethod = new LMFunction(LMInt.i8.pointer, ".rt.lookup_method", Seq(ArgSpec(new LocalVariable("dtable", rtDispatch.pointer)), ArgSpec(new LocalVariable("cls", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxi1 = new LMFunction(symType(definitions.BoxedBooleanClass), ".rt.box.i1", Seq(ArgSpec(new LocalVariable("v", LMInt.i1))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxi8 = new LMFunction(symType(definitions.BoxedByteClass), ".rt.box.i8", Seq(ArgSpec(new LocalVariable("v", LMInt.i8))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxi16 = new LMFunction(symType(definitions.BoxedShortClass), ".rt.box.i16", Seq(ArgSpec(new LocalVariable("v", LMInt.i16))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxi32 = new LMFunction(symType(definitions.BoxedIntClass), ".rt.box.i32", Seq(ArgSpec(new LocalVariable("v", LMInt.i32))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxi64 = new LMFunction(symType(definitions.BoxedLongClass), ".rt.box.i64", Seq(ArgSpec(new LocalVariable("v", LMInt.i64))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxFloat = new LMFunction(symType(definitions.BoxedFloatClass), ".rt.box.float", Seq(ArgSpec(new LocalVariable("v", LMFloat))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxDouble = new LMFunction(symType(definitions.BoxedDoubleClass), ".rt.box.double", Seq(ArgSpec(new LocalVariable("v", LMDouble))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxChar = new LMFunction(symType(definitions.BoxedCharacterClass), ".rt.box.char", Seq(ArgSpec(new LocalVariable("v", LMInt.i16))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxi1 = new LMFunction(LMInt.i1, ".rt.unbox.i1", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedBooleanClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxi8 = new LMFunction(LMInt.i8, ".rt.unbox.i8", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedByteClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxi16 = new LMFunction(LMInt.i16, ".rt.unbox.i16", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedShortClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxi32 = new LMFunction(LMInt.i32, ".rt.unbox.i32", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedIntClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxi64 = new LMFunction(LMInt.i64, ".rt.unbox.i64", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedLongClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxFloat = new LMFunction(LMFloat, ".rt.unbox.float", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedFloatClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxDouble = new LMFunction(LMDouble, ".rt.unbox.double", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedDoubleClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtUnboxChar = new LMFunction(LMInt.i16, ".rt.unbox.char", Seq(ArgSpec(new LocalVariable("v", symType(definitions.BoxedCharacterClass)))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtMakeString = new LMFunction(symType(definitions.StringClass), ".rt.makestring", Seq(ArgSpec(new LocalVariable("s", LMInt.i8.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtIfaceCast = new LMFunction(rtIfaceRef, ".rt.iface.cast", Seq(ArgSpec(new LocalVariable("obj", rtObject.pointer)), ArgSpec(new LocalVariable("iface", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtIsinstanceIface = new LMFunction(LMInt.i1, ".rt.isinstance.iface", Seq(ArgSpec(new LocalVariable("obj", rtObject.pointer)), ArgSpec(new LocalVariable("iface", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtIsinstanceClass = new LMFunction(LMInt.i1, ".rt.isinstance.class", Seq(ArgSpec(new LocalVariable("obj", rtObject.pointer)), ArgSpec(new LocalVariable("iface", rtClass.pointer))), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
      val rtBoxedUnit = new LMGlobalVariable(".rt.boxedUnit", symType(definitions.BoxedUnitClass), Externally_visible, Default, true)
      val rtOpaqueTypes = Seq(
        LMOpaque.aliased("java.lang.Boolean"),
        LMOpaque.aliased("java.lang.Byte"),
        LMOpaque.aliased("java.lang.Short"),
        LMOpaque.aliased("java.lang.Integer"),
        LMOpaque.aliased("java.lang.Long"),
        LMOpaque.aliased("java.lang.Float"),
        LMOpaque.aliased("java.lang.Double"),
        LMOpaque.aliased("java.lang.String"),
        LMOpaque.aliased("java.lang.Character"),
        LMOpaque.aliased("scala.runtime.BoxedUnit")
      )
      val rtHeader: Seq[ModuleComp] = Seq(
        new TypeAlias(rtVtable),
        new TypeAlias(rtClass),
        new TypeAlias(rtDispatch),
        new TypeAlias(rtIfaceInfo),
        new TypeAlias(rtIfaceRef),
        rtLookupMethod.declare,
        rtNew.declare,
        rtInitobj.declare,
        rtGetClass.declare,
        rtLookupMethod.declare,
        rtBoxi1.declare,
        rtBoxi8.declare,
        rtBoxi16.declare,
        rtBoxi32.declare,
        rtBoxi64.declare,
        rtBoxFloat.declare,
        rtBoxDouble.declare,
        rtBoxChar.declare,
        rtUnboxi1.declare,
        rtUnboxi8.declare,
        rtUnboxi16.declare,
        rtUnboxi32.declare,
        rtUnboxi64.declare,
        rtUnboxFloat.declare,
        rtUnboxDouble.declare,
        rtUnboxChar.declare,
        rtMakeString.declare,
        rtIsinstanceIface.declare,
        rtIsinstanceClass.declare,
        rtIfaceCast.declare,
        rtBoxedUnit.declare
      )
    }

    import Runtime._

    def isMain(c: IClass) = c.symbol.isClassOfModule && c.symbol.fullName('.') == settings.Xmainclass.value

    def genClass(c: IClass) {
      val externFuns: mutable.Map[Symbol,LMFunction] = new mutable.HashMap
      val externClasses: mutable.Map[Symbol,LMGlobalVariable[_<:ConcreteType]] = new mutable.HashMap
      val internalFuns: mutable.Set[Symbol] = new mutable.HashSet
      val internalClasses: mutable.Set[Symbol] = new mutable.HashSet
      val extraDefs: mutable.ListBuffer[ModuleComp] = new mutable.ListBuffer
      val externModules: mutable.Map[Symbol,LMGlobalVariable[_<:ConcreteType]] = new mutable.HashMap
      val externTypes: mutable.ListBuffer[AliasedType] = new mutable.ListBuffer
      var globidx = 0

      c.symbol.getAnnotation(LlvmdefsAnnotSym) match {
        case Some(AnnotationInfo(_, List(Literal(Constant(s: String))), _)) => extraDefs += new ModuleComp { val syntax = s }
        case Some(x) => error("Invalid llvmdefs annotation")
        case None => ()
      }

      externTypes ++= rtOpaqueTypes

      def nextconst(v: LMConstant[_<:ConcreteType], linkage: Linkage = Private) = {
        val gv = new LMGlobalVariable(".global."+globidx.toString, v.tpe, linkage, Default, false)
        extraDefs += gv.define(v)
        globidx = globidx + 1
        gv
      }

      def generateMain(f: LMFunction) = {
        val mainfun = new LMFunction(LMInt.i32, "main", Seq(ArgSpec(new LocalVariable("argc", LMInt.i32)), ArgSpec(new LocalVariable("argv", LMInt.i8.pointer.pointer))), false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
        val asobject = new LocalVariable("instance.o", rtObject.pointer)
        val asinstance = new LocalVariable("instance", symType(c.symbol))
        mainfun.define(Seq(
          LMBlock(None, Seq(
            new call_void(f, Seq(new CGlobalAddress(externModule(c.symbol)))),
            new ret(new CInt(LMInt.i32, 0))))))
      }

      def recordType(t: LMType) {
        t match {
          case a:AliasedType => externTypes += a
          case p:LMPointer => recordType(p.target)
          case f:LMFunctionType => { recordType(f.returnType); f.argTypes.foreach(recordType) }
          case _ => ()
        }
      }

      def externModule(s: Symbol) = {
        recordType(symType(s))
        if (c.symbol == s.moduleClass) {
            new LMGlobalVariable(moduleInstanceName(s), symType(s).asInstanceOf[LMPointer].target, Externally_visible, Default, false)
        } else {
          externModules.getOrElseUpdate(s, {
            new LMGlobalVariable(moduleInstanceName(s), symType(s).asInstanceOf[LMPointer].target, Externally_visible, Default, true)
          })
        }
      }

      def externFun(s: Symbol) = {
        externFuns.getOrElseUpdate(s, {
          val funtype = symType(s).asInstanceOf[LMFunctionType]
          funtype.argTypes.foreach(recordType)
          recordType(funtype.returnType)
          val args = funtype.argTypes.zipWithIndex.map{case (t,i) => ArgSpec(new LocalVariable("arg"+i, t), Seq.empty)}
          new LMFunction(funtype.returnType, llvmName(s), args, false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
        })
      }

      var cig: LMGlobalVariable[LMStructure] = null

      def externClassP(s: Symbol) = new Cbitcast(new CGlobalAddress(externClass(s)), rtClass.pointer)

      def externClass(s: Symbol) = {
        if (c.symbol == s) {
          cig
        } else {
          externClasses.getOrElseUpdate(s, {
            new LMGlobalVariable(classInfoName(s), rtClass, Externally_visible, Default, true)
          })
        }
      }

      def virtualMethods(s: Symbol) = {
        val supers = Stream.iterate(s)(_.superClass).takeWhile(s => s != NoSymbol).filter(s => s.isFinal || s.isClassOfModule)
        val virtdecls = supers.reverse.flatMap(_.info.decls.toList.filter(d => d.isMethod && !d.isConstructor && !d.isOverride && !d.isEffectivelyFinal))
        virtdecls
      }

      val classInfo: Seq[ModuleComp] = {
        val ct = classType(c)
        val supers = Stream.iterate(c.symbol.superClass)(_.superClass).takeWhile(s => s != NoSymbol)
        val traits = c.symbol.info.baseClasses.filter(_.isTrait)
        supers.map(classType).foreach(recordType)
        recordType(ct)
        val basevirts = virtualMethods(c.symbol)
        val virts = basevirts.map(m => c.symbol.info.member(m.name).filter(_.info <:< m.info))
        val vfuns = virts.map(v => if (v.isIncompleteIn(c.symbol)) new CNull(LMInt.i8.pointer) else new CFunctionAddress(externFun(v)))
        val vtable = new CArray(LMInt.i8.pointer, vfuns.map(f => new Cbitcast(f, LMInt.i8.pointer)))
        val vtableg = new LMGlobalVariable(".vtable", vtable.tpe, Private, Default, true)
        val traitinfo = traits.map { t =>
          val tvirts = virtualMethods(t)
          val tvimpls = tvirts.map(m => c.symbol.info.member(m.name).filter(_.info <:< m.info))
          val tvfuns = tvimpls.map(v => if (v.isIncompleteIn(c.symbol)) new CNull(LMInt.i8.pointer) else new CFunctionAddress(externFun(v)))
          val tvtable = new CArray(LMInt.i8.pointer, tvfuns.map(f => new Cbitcast(f, LMInt.i8.pointer)))
          val tvtableg = new LMGlobalVariable(".vtable."+llvmName(t), tvtable.tpe, Private, Default, true)
          (tvtableg,tvtable)
        }
        val n = new CArray(LMInt.i8, (llvmName(c.symbol)+"\0").getBytes("UTF-8").map(new CInt(LMInt.i8, _)))
        val ng = new LMGlobalVariable(".classname", n.tpe, Private, Default, true)
        val ci = new CStruct(Seq(new Cgetelementptr(ng, Seq[LMConstant[LMInt]](0,0), LMInt.i8.pointer),
                                 new Cptrtoint(new Cgetelementptr(new CNull(ct.pointer), Seq[LMConstant[LMInt]](1), ct.pointer), LMInt.i32),
                                 externClassP(c.symbol.superClass),
                                 new Cgetelementptr(vtableg, Seq[CInt](0,0), rtVtable),
                                 new CInt(LMInt.i32, traitinfo.length),
                                 new CArray(rtIfaceInfo, traits.zip(traitinfo).map{ case (t, (tvg, _)) => new CStruct(Seq(externClassP(t), new Cgetelementptr(new CGlobalAddress(tvg), Seq[CInt](0,0), rtVtable)))})))
        cig = new LMGlobalVariable[LMStructure](classInfoName(c.symbol), ci.tpe, Externally_visible, Default, true)
        Seq[LMGlobalVariableDefn[_<:ConcreteType]](cig.define(ci), ng.define(n), vtableg.define(vtable))++traitinfo.map(ti => ti._1.define(ti._2))
      }

      val moduleInfo: Seq[ModuleComp] = {
        if (c.symbol.isClassOfModule) {
          val ig = new LMGlobalVariable(moduleInstanceName(c.symbol), symType(c.symbol).asInstanceOf[LMPointer].target, Externally_visible, Default, false)
          val initFun = new LMFunction(LMVoid, ".init."+moduleInstanceName(c.symbol), Seq.empty, false, Internal, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
          val asobject = new LocalVariable("object", rtObject.pointer)
          val asinstance = new LocalVariable("instance", symType(c.symbol).asInstanceOf[LMPointer])
          val asvalue = new LocalVariable("value", symType(c.symbol).asInstanceOf[LMPointer].target)
          val initFundef = initFun.define(Seq(
            LMBlock(None, Seq(
              new call(asobject, rtInitobj, Seq(new Cbitcast(new CGlobalAddress(ig), rtObject.pointer), externClassP(c.symbol))),
              new call_void(externFun(c.lookupMethod("<init>").get.symbol), Seq(new CGlobalAddress(ig))),
              retvoid
            ))))
          val ctors = new LMGlobalVariable("llvm.global_ctors", new LMArray(1, new LMStructure(Seq(LMInt.i32, new LMFunctionType(LMVoid, Seq.empty, false).pointer))), Appending, Default, false)
          val ctorsdef = ctors.define(new CArray(ctors.tpe.elementtype, Seq(new CStruct(Seq(new CInt(LMInt.i32, 1), new CFunctionAddress(initFun))))))
          Seq(ig.define(new CUndef(ig.tpe)),initFundef,ctorsdef)
        } else {
          Seq.empty
        }
      }

      def genNativeFun(m: IMethod) = {
        val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
        val args = thisarg +: m.params.map(p => ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))))
        val fun = new LMFunction(typeType(m.returnType.toType), llvmName(m.symbol), args, false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
        recordType(fun.tpe)
        fun.args.map(_.lmvar.tpe).foreach(recordType)
        if (isMain(c) && m.params.isEmpty && m.symbol.simpleName.toString().trim() == "main" && fun.resultType == LMVoid) {
          extraDefs += generateMain(fun)
        }
        val body = m.symbol.getAnnotation(LlvmimplAnnotSym) match {
          case Some(AnnotationInfo(_,List(Literal(Constant(s: String))),_)) => s
          case Some(x) => error("Invalid llvmimpl annotation"); ""
          case None => error("Generating native funtion but no llvmimpl annotation"); ""
        }
        fun.define(body)
      }
      def genFun(m: IMethod) = {
        internalFuns += m.symbol
        val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
        val args = (thisarg, c.symbol) +: m.params.map(p => (ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))), p.sym))
        val fun = new LMFunction(typeType(m.returnType.toType), llvmName(m.symbol), args.map(_._1), false, Externally_visible, Default, Fastcc, Seq.empty, Seq.empty, None, None, None)
        recordType(fun.tpe)
        fun.args.map(_.lmvar.tpe).foreach(recordType)
        if (isMain(c) && m.params.isEmpty && m.symbol.simpleName.toString().trim() == "main" && fun.resultType == LMVoid) {
          extraDefs += generateMain(fun)
        }
        val blocks: mutable.ListBuffer[LMBlock] = new mutable.ListBuffer
        var varidx = 0
        def nextvar[T <: ConcreteType](t: T) = {
          val v = new LocalVariable(varidx.toString, t)
          varidx = varidx + 1
          v
        }
        m.code.blocks.filter(reachable).foreach { bb =>
          val stack: mutable.Stack[(LMValue[_<:ConcreteType],Symbol)] = mutable.Stack()
          def popn(n: Int) {
            for (_ <- 0 until n) stack.pop
          }
          val insns: mutable.ListBuffer[LMInstruction] = new mutable.ListBuffer
          insns.append(new icomment("predecessors: "+bb.predecessors.filter(reachable).map(_.flagsString).mkString(",")+" successors: "+bb.successors.map(_.flagsString).mkString(" ")))
          def cast(src: LMValue[_<:ConcreteType], srcsym: Symbol, targetsym: Symbol): LMValue[_<:ConcreteType] = {
            insns.append(new icomment("cast "+src.rep+" from "+srcsym+" to "+targetsym))
            if (srcsym == targetsym) {
              src
            } else {
              recordType(symType(targetsym))
              if (targetsym.isTrait) {
                if (srcsym.isTrait) {
                  cast(cast(src, srcsym, definitions.ObjectClass), definitions.ObjectClass, targetsym)
                } else {
                  srcsym.info.baseClasses.filter(_.isTrait).indexOf(targetsym) match {
                    case -1 => {
                      val iface = nextvar(rtIfaceRef)
                      insns.append(new call(iface, rtIfaceCast, Seq(src, externClassP(targetsym))))
                      iface
                    }
                    case tgtidx => {
                      val vtbl = nextvar(rtVtable)
                      val vtblptr = nextvar(rtVtable.pointer)
                      val clsptrptr = nextvar(rtClass.pointer.pointer)
                      val clsptr = nextvar(rtClass.pointer)
                      val asobj = nextvar(rtObject.pointer)
                      val iref0 = nextvar(rtIfaceRef)
                      val iref1 = nextvar(rtIfaceRef)
                      insns.append(new bitcast(asobj, src))
                      insns.append(new getelementptr(clsptrptr, asobj, Seq[LMConstant[LMInt]](0, 0)))
                      insns.append(new load(clsptr, clsptrptr))
                      insns.append(new getelementptr(vtblptr, clsptr, Seq[LMConstant[LMInt]](0, 5, tgtidx, 1)))
                      insns.append(new load(vtbl, vtblptr))
                      insns.append(new insertvalue(iref0, new CUndef(rtIfaceRef), asobj, Seq[LMConstant[LMInt]](0)))
                      insns.append(new insertvalue(iref1, iref0, vtbl, Seq[LMConstant[LMInt]](1)))
                      iref1
                    }
                  }
                }
              } else {
                if (srcsym.isTrait) {
                  if (targetsym == definitions.ObjectClass) {
                    val obj = nextvar(rtObject.pointer)
                    insns.append(new extractvalue(obj, src.asInstanceOf[LMValue[rtIfaceRef.type]], Seq[CInt](0)))
                    obj
                  } else {
                    cast(cast(src, srcsym, definitions.ObjectClass), definitions.ObjectClass, targetsym)
                  }
                } else {
                  val v = nextvar(symType(targetsym))
                  insns.append(new bitcast(v, src))
                  v
                }
              }
            }
          }
          val (consumedTypes,producedTypes) = stackUsage(bb)
          insns.append(new icomment("consumed types: " + consumedTypes.mkString(", ")))
          insns.append(new icomment("produced types: " + producedTypes.mkString(", ")))
          val predcasts = new mutable.ListBuffer[LMInstruction]
          consumedTypes.zipWithIndex.foreach { case (sym,n) =>
            val tpe = symType(sym)
            recordType(tpe)
            if (definitions.isValueClass(sym) || sym.isTrait) {
              val reg = new LocalVariable(blockName(bb)+".in."+n.toString,tpe)
              val sources = bb.predecessors.filter(reachable).map(pred => (blockLabel(pred), new LocalVariable(blockName(pred)+".out."+n.toString,tpe)))
              insns.append(new phi(reg, sources))
              stack.push((reg, sym))
            } else {
              val reg = new LocalVariable(blockName(bb)+".in."+n.toString,tpe)
              val asobj = nextvar(rtObject.pointer)
              val sources = bb.predecessors.filter(reachable).map(pred => (blockLabel(pred), new LocalVariable(blockName(pred)+".out."+n.toString,rtObject.pointer)))
              insns.append(new phi(asobj, sources))
              predcasts.append(new bitcast(reg, asobj))
              stack.push((reg, sym))
            }
          }
          val locals: mutable.HashMap[Symbol,LocalVariable[_<:ConcreteType]] = new mutable.HashMap
          m.locals.foreach { l =>
            val tpe = typeKindType(l.kind)
            val sym = l.sym
            val reg = new LocalVariable(blockName(bb)+".local.in."+llvmName(sym)+"."+sym.id, tpe)
            locals(sym) = reg
            val sources = bb.predecessors.filter(reachable).map(pred => (blockLabel(pred), new LocalVariable(blockName(pred)+".local.out."+llvmName(sym)+"."+sym.id,tpe)))
            val insn = if (sources.isEmpty) new select(reg, CTrue, args.find(_._2==sym).map(_._1.lmvar).getOrElse(new CUndef(reg.tpe)), new CUndef(reg.tpe)) else new phi(reg, sources)
            insns.append(insn)
          }
          insns.appendAll(predcasts)
          bb.foreach { i =>
            insns.append(new icomment(i.toString))
            insns.append(new icomment("stack before: "+stack.map{case (v,s) => v.rep + " " + s}.mkString(", ")))
            i match {
              case THIS(clasz) => {
                stack.push((thisarg.lmvar,clasz))
              }
              case CONSTANT(const) => {
                val value = constValue(const) match {
                  case s if const.tag == StringTag => {
                    val g = nextconst(s)
                    val v = nextvar(rtMakeString.resultType)
                    insns.append(new call(v, rtMakeString, Seq(new Cgetelementptr(new CGlobalAddress(g), Seq[CInt](0,0), LMInt.i8.pointer))))
                    v
                  }
                  case v => v
                }
                stack.push((value,const.tpe.typeSymbol))
              }
              case LOAD_ARRAY_ITEM(kind) => {
                warning("unhandled " + i)
                popn(i.consumed)
                stack.push((new CUndef(typeKindType(kind)), kind.toType.typeSymbol))
              }
              case LOAD_LOCAL(local) => {
                stack.push((locals(local.sym), local.sym.tpe.typeSymbol))
              }
              case LOAD_FIELD(field, isStatic) if (field == definitions.BoxedUnit_UNIT) => {
                stack.push((new CGlobalAddress(rtBoxedUnit), definitions.BoxedUnitClass))
              }
              case i@LOAD_FIELD(field, isStatic) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                fields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    val (ivar,isym) = stack.pop
                    val instance = cast(ivar,isym,field.owner)
                    insns.append(new getelementptr(fieldptr, instance.asInstanceOf[LMValue[LMPointer]], Seq(new CInt(LMInt.i8,0),new CInt(LMInt.i32,fieldidx+1))))
                    insns.append(new load(v, fieldptr))
                    stack.push((v,field.tpe.typeSymbol))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                    m.dump
                    stack.push((new CUndef(v.tpe),NoSymbol))
                }
              }
              case LOAD_MODULE(module) => {
                stack.push((new CGlobalAddress(externModule(module)), module))
              }
              case STORE_ARRAY_ITEM(kind) => {
                warning("unhandled " + i)
                popn(i.consumed)
              }
              case STORE_LOCAL(local) => {
                val l = nextvar(symType(local.sym))
                val (v,s) = stack.pop
                insns.append(new select(l, CTrue, cast(v,s,local.sym.tpe.typeSymbol), cast(v,s,local.sym.tpe.typeSymbol)))
                locals(local.sym) = l
              }
              case STORE_FIELD(field, isStatic) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                fields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    val (value,valuesym) = stack.pop
                    val instance = stack.pop._1
                    insns.append(new getelementptr(fieldptr, instance.asInstanceOf[LMValue[LMPointer]], Seq(new CInt(LMInt.i8,0),new CInt(LMInt.i32,fieldidx+1))))
                    insns.append(new store(cast(value, valuesym, field.tpe.typeSymbol), fieldptr))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                }
              }
              case CALL_PRIMITIVE(primitive) => {
                primitive match {
                  case Negation(_) => {
                    val (arg,argsym) = stack.pop
                    val v = nextvar(arg.tpe)
                    arg.tpe match {
                      case i:LMInt => insns.append(new mul(v.asInstanceOf[LocalVariable[LMInt]], arg.asInstanceOf[LMValue[LMInt]], new CInt(i,-1)))
                      case f:LMFloat => insns.append(new fmul(v.asInstanceOf[LocalVariable[LMFloat]], arg.asInstanceOf[LMValue[LMFloat]], new CFloat(-1)))
                      case d:LMDouble => insns.append(new fmul(v.asInstanceOf[LocalVariable[LMDouble]], arg.asInstanceOf[LMValue[LMDouble]], new CDouble(-1)))
                    }
                    stack.push((v,argsym))
                  }
                  case Test(op, k, z) => {
                    val cmpto = if (z) new CZeroInit(typeKindType(k)) else stack.pop._1
                    val arg = stack.pop._1
                    val result = nextvar(LMInt.i1)
                    def cmp(intop: ICond, floatop: FCond) = {
                      arg.tpe match {
                        case _:LMInt => insns.append(new icmp(result, intop, arg.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                        case _:LMFloat => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMFloat]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                        case _:LMDouble => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMDouble]], cmpto.asInstanceOf[LMValue[LMDouble]]))
                        case _:LMPointer => insns.append(new icmp_p(result, intop, arg.asInstanceOf[LMValue[LMPointer]], cmpto.asInstanceOf[LMValue[LMPointer]]))
                      }
                    }
                    val cond = op match {
                      case EQ => cmp(ICond.ieq, FCond.oeq)
                      case NE => cmp(ICond.ine, FCond.one)
                      case LT => cmp(ICond.slt, FCond.olt)
                      case GE => cmp(ICond.sge, FCond.oge)
                      case LE => cmp(ICond.sle, FCond.ole)
                      case GT => cmp(ICond.sgt, FCond.ogt)
                    }
                    stack.push((result, definitions.BooleanClass))
                  }
                  case Comparison(op, _) => {
                    val v2 = stack.pop._1
                    val v1 = stack.pop._1
                    val result = nextvar(LMInt.i32)
                    v1.tpe match {
                      case _:LMFloatingPointType => {
                        val v1greater = nextvar(LMInt.i1)
                        val equal = nextvar(LMInt.i1)
                        val unordered = nextvar(LMInt.i1)
                        val unorderedval = op match {
                          case CMPL => new CInt(LMInt.i32, -1)
                          case CMPG => new CInt(LMInt.i32, 1)
                          case CMP => new CInt(LMInt.i32, 1)
                        }
                        insns.append(new fcmp(equal, FCond.oeq, v1.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        insns.append(new fcmp(v1greater, FCond.ogt, v1.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        insns.append(new fcmp(unordered, FCond.uno, v1.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        val temp1 = nextvar(LMInt.i32)
                        val temp2 = nextvar(LMInt.i32)
                        insns.append(new select(temp1, v1greater, new CInt(LMInt.i32, 1), new CInt(LMInt.i32, -1)))
                        insns.append(new select(temp2, equal, new CInt(LMInt.i32, 0), temp1))
                        insns.append(new select(result, unordered, unorderedval, temp2))
                      }
                      case it:LMInt => {
                        val v1greater = nextvar(LMInt.i1)
                        val equal = nextvar(LMInt.i1)
                        insns.append(new icmp(equal, ICond.ieq, v1.asInstanceOf[LMValue[LMInt]], v2.asInstanceOf[LMValue[LMInt]]))
                        insns.append(new icmp(v1greater, ICond.sgt, v1.asInstanceOf[LMValue[LMInt]], v2.asInstanceOf[LMValue[LMInt]]))
                        val temp = nextvar(LMInt.i32)
                        insns.append(new select(temp, v1greater, new CInt(LMInt.i32, 1), new CInt(LMInt.i32, -1)))
                        insns.append(new select(result, equal, new CInt(LMInt.i32, 0), temp))
                      }
                    }
                    stack.push((result, definitions.IntClass))
                  }
                  case Arithmetic(NOT, _) => {
                    val (argt, s) = stack.pop
                    val arg = argt.asInstanceOf[LMValue[LMInt]]
                    val res = nextvar(arg.tpe)
                    insns.append(new xor(res, arg.asInstanceOf[LMValue[LMInt]], new CInt(arg.tpe, -1)))
                    stack.push((res,s))
                  }
                  case Arithmetic(op, k) => {
                    val (v2,ressym) = stack.pop
                    val v1 = stack.pop._1
                    val result = nextvar(typeKindType(k))
                    def choose(intop: =>LMInstruction, floatop: =>LMInstruction) {
                      v1.tpe match {
                        case _:LMInt => insns.append(intop)
                        case _:LMFloatingPointType => insns.append(floatop)
                      }
                    }
                    def cv[T <: ConcreteType](x: LocalVariable[_]) = x.asInstanceOf[LocalVariable[T]]
                    def c[T <: ConcreteType](x: LMValue[_]) = x.asInstanceOf[LMValue[T]]
                    def extend(v: LMValue[LMInt]) = {
                      val resultAsInt = result.asInstanceOf[LocalVariable[LMInt]]
                      if (v.tpe.bits < resultAsInt.tpe.bits) {
                        val temp = nextvar(resultAsInt.tpe)
                        insns.append(new sext(temp, v))
                        temp
                      } else {
                        v
                      }
                    }
                    op match {
                      case ADD => choose(new add(cv(result), extend(c(v1)), extend(c(v2))), new fadd(cv(result), c(v1), c(v2)))
                      case SUB => choose(new sub(cv(result), extend(c(v1)), extend(c(v2))), new fsub(cv(result), c(v1), c(v2)))
                      case MUL => choose(new mul(cv(result), extend(c(v1)), extend(c(v2))), new fmul(cv(result), c(v1), c(v2)))
                      case DIV => choose(new sdiv(cv(result), extend(c(v1)), extend(c(v2))), new fdiv(cv(result), c(v1), c(v2)))
                      case REM => choose(new srem(cv(result), extend(c(v1)), extend(c(v2))), new frem(cv(result), c(v1), c(v2)))
                    }
                    stack.push((result, ressym))
                  }
                  case Logical(op, _) => {
                    val (v2t,ressym) = stack.pop
                    val v2 = v2t.asInstanceOf[LMValue[LMInt]]
                    val v1 = stack.pop._1.asInstanceOf[LMValue[LMInt]]
                    val result = nextvar(v2.tpe)
                    val insn = op match {
                      case AND => new and(result, v1, v2)
                      case OR => new or(result, v1, v2)
                      case XOR => new xor(result, v1, v2)
                    }
                    insns.append(insn)
                    stack.push((result, ressym))
                  }
                  case Shift(op, _) => {
                    val (argt,ressym) = stack.pop
                    val arg = argt.asInstanceOf[LMValue[LMInt]]
                    val shiftamt = stack.pop._1.asInstanceOf[LMValue[LMInt]]
                    val temp = nextvar(arg.tpe)
                    val result = nextvar(arg.tpe)
                    insns.append(new zext(temp, shiftamt))
                    val insn = op match {
                      case LSL => new shl(result, arg, temp)
                      case ASR => new ashr(result, arg, temp)
                      case LSR => new lshr(result, arg, temp)
                    }
                    insns.append(insn)
                    stack.push((result, ressym))
                  }
                  case Conversion(_, dk) => {
                    val dt = typeKindType(dk)
                    val src = stack.pop._1
                    val result = nextvar(dt)
                    dt match {
                      case dit:LMInt => {
                        src.tpe match {
                          case sit:LMInt => if (dit.bits > sit.bits) {
                            insns.append(new sext(result.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMInt]]))
                          } else {
                            insns.append(new trunc(result.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMInt]]))
                          }
                          case sft:LMFloatingPointType => {
                            insns.append(new fptosi(result.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                          }
                        }
                      }
                      case dft:LMFloatingPointType => {
                        src.tpe match {
                          case sit:LMInt => insns.append(new sitofp(result.asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]], src.asInstanceOf[LMValue[LMInt]]))
                          case sft:LMFloatingPointType => if (dft.bits > sft.bits) {
                            insns.append(new fpext(result.asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]], src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                          } else {
                            insns.append(new fptrunc(result.asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]], src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                          }
                        }
                      }
                    }
                    stack.push((result,dk.toType.typeSymbol))
                  }
                  case _ => warning("unsupported primitive op " + primitive)
                }
              }
              case CALL_METHOD(method, style) => {
                val funtype = symType(method).asInstanceOf[LMFunctionType]
                val contextargs = style match {
                  case Static(true) | Dynamic | SuperCall(_) => if (method.owner.isTrait) Seq(definitions.ObjectClass) else Seq(method.owner)
                  case Static(false) => Seq.empty
                }
                val argsyms = contextargs ++ method.tpe.paramTypes.map(_.typeSymbol)
                val args = stack.take(funtype.argTypes.size).reverse.toBuffer
                val fun = style match {
                  case Dynamic if method.isEffectivelyFinal => new CFunctionAddress(externFun(method))
                  case Dynamic => {
                    val vtbl = nextvar(rtVtable)
                    val mnum = virtualMethods(method.owner).indexOf(method)
                    if (args.head._2.isTrait) {
                      val ifaceinfo = args.head._1.asInstanceOf[LMValue[rtIfaceRef.type]]
                      val receiverobj = nextvar(rtObject.pointer)
                      args(0) = (receiverobj, definitions.ObjectClass)
                      insns.append(new extractvalue(receiverobj, ifaceinfo, Seq[LMConstant[LMInt]](0)))
                      insns.append(new extractvalue(vtbl, ifaceinfo, Seq[LMConstant[LMInt]](1)))
                    } else {
                      val asobj = nextvar(rtObject.pointer)
                      val clsa = nextvar(rtClass.pointer.pointer)
                      val cls = nextvar(rtClass.pointer)
                      val vtbla = nextvar(rtVtable.pointer)
                      insns.append(new bitcast(asobj, args.head._1))
                      insns.append(new getelementptr(clsa, asobj, Seq[CInt](0,0)))
                      insns.append(new load(cls, clsa))
                      insns.append(new getelementptr(vtbla, cls, Seq[CInt](0,3)))
                      insns.append(new load(vtbl, vtbla))
                    }
                    val funptraddr = nextvar(LMInt.i8.pointer.pointer)
                    val funptr = nextvar(LMInt.i8.pointer)
                    insns.append(new getelementptr(funptraddr, vtbl, Seq[CInt](mnum)))
                    insns.append(new load(funptr, funptraddr))
                    funptr
                  }
                  case _ => new CFunctionAddress(externFun(method))
                }
                recordType(funtype.returnType)
                funtype.argTypes.foreach(recordType)
                popn(funtype.argTypes.length)
                val castedfun = nextvar(funtype.pointer)
                insns.append(new bitcast(castedfun, fun))
                val castedargs = args.zip(argsyms).map { case ((v,s),d) => cast(v,s,d) }
                if (funtype.returnType == LMVoid) {
                  insns.append(new call_void(Fastcc, Seq.empty, castedfun, castedargs, Seq.empty))
                } else {
                  val v = nextvar(funtype.returnType)
                  insns.append(new call(v, Fastcc, Seq.empty, castedfun, castedargs, Seq.empty))
                  stack.push((v,method.tpe.resultType.typeSymbol))
                }
              }
              case NEW(kind) => {
                val asobject = nextvar(rtObject.pointer)
                insns.append(new call(asobject, rtNew, Seq(externClassP(kind.toType.typeSymbol))))
                val casted = nextvar(typeKindType(kind))
                recordType(casted.tpe)
                insns.append(new bitcast(casted, asobject))
                stack.push((casted,kind.toType.typeSymbol))
              }
              case CREATE_ARRAY(elem, dims) => {
                warning("unhandled " + i)
                popn(i.consumed)
                stack.push((new CUndef(rtObject.pointer), NoSymbol))
              }
              case IS_INSTANCE(tpe) => {
                val (ref,sym) = stack.pop()
                val v = nextvar(LMInt.i1)
                val tpes = tpe.toType.typeSymbol
                val cls = externClassP(tpes)
                val fun = if (tpes.isTrait) rtIsinstanceIface else rtIsinstanceClass
                insns.append(new call(v, fun, Seq(cast(ref, sym, definitions.ObjectClass), cls)))
                stack.push((v, definitions.BooleanClass))
              }
              case CHECK_CAST(tpe) => {
                val (ref,sym) = stack.pop()
                stack.push((cast(ref, sym, tpe.toType.typeSymbol), tpe.toType.typeSymbol))
              }
              case SWITCH(tags, labels) => {
                val v = stack.pop._1.asInstanceOf[LMValue[LMInt]]
                val deflabel = blockLabel(labels.last)
                val taggedlabels = tags.zip(labels.dropRight(1))
                val dests = taggedlabels.flatMap { case (tvs,b) =>
                  tvs.map(tv => (new CInt(v.tpe, tv), blockLabel(b)))
                }
                insns.append(new switch(v, deflabel, dests))
              }
              case JUMP(whereto) => insns.append(new br(blockLabel(whereto)))
              case CJUMP(success, failure, op, kind) => {
                val (cmpto,cmptosym) = stack.pop
                val (arg,argsym) = stack.pop
                val result = nextvar(LMInt.i1)
                def cmp(intop: ICond, floatop: FCond) = {
                  arg.tpe match {
                    case _:LMInt => insns.append(new icmp(result, intop, arg.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case _:LMFloat => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMFloat]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case _:LMDouble => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMDouble]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case _:LMPointer => insns.append(new icmp_p(result, intop, cast(arg, argsym, definitions.ObjectClass).asInstanceOf[LMValue[LMPointer]], cast(cmpto, cmptosym, definitions.ObjectClass).asInstanceOf[LMValue[LMPointer]]))
                  }
                }
                val cond = op match {
                  case EQ => cmp(ICond.ieq, FCond.oeq)
                  case NE => cmp(ICond.ine, FCond.one)
                  case LT => cmp(ICond.slt, FCond.olt)
                  case GE => cmp(ICond.sge, FCond.oge)
                  case LE => cmp(ICond.sle, FCond.ole)
                  case GT => cmp(ICond.sgt, FCond.ogt)
                }
                insns.append(new br_cond(result, blockLabel(success), blockLabel(failure)))
              }
              case CZJUMP(success, failure, op, kind) => {
                val arg = stack.pop._1
                val cmpto = new CZeroInit(arg.tpe)
                val result = nextvar(LMInt.i1)
                def cmp(intop: ICond, floatop: FCond) = {
                  arg.tpe match {
                    case _:LMInt => insns.append(new icmp(result, intop, arg.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case _:LMFloat => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMFloat]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case _:LMDouble => insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMDouble]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case _:LMPointer => insns.append(new icmp_p(result, intop, arg.asInstanceOf[LMValue[LMPointer]], cmpto.asInstanceOf[LMValue[LMPointer]]))
                  }
                }
                val cond = op match {
                  case EQ => cmp(ICond.ieq, FCond.oeq)
                  case NE => cmp(ICond.ine, FCond.one)
                  case LT => cmp(ICond.slt, FCond.olt)
                  case GE => cmp(ICond.sge, FCond.oge)
                  case LE => cmp(ICond.sle, FCond.ole)
                  case GT => cmp(ICond.sgt, FCond.ogt)
                }
                insns.append(new br_cond(result, blockLabel(success), blockLabel(failure)))
              }
              case RETURN(kind) => {
                if (kind == UNIT) {
                  insns.append(retvoid)
                } else {
                  val (v,s) = stack.pop
                  insns.append(new ret(cast(v,s,m.returnType.toType.typeSymbol)))
                }
              }
              case THROW() => {
                warning("unhandled " + i)
                popn(i.consumed)
                insns.append(unwind)
              }
              case DROP(kind) => stack.pop
              case DUP(kind) => stack.push(stack.top)
              case MONITOR_ENTER() => {
                warning("unhandled " + i)
                popn(i.consumed)
              }
              case MONITOR_EXIT() => {
                warning("unhandled " + i)
                popn(i.consumed)
              }
              case SCOPE_ENTER(lv) => ()
              case SCOPE_EXIT(lv) => ()
              case LOAD_EXCEPTION() => warning("unhandled " + i)
              case BOX(k) => {
                val unboxed = stack.pop._1
                val fun = k match {
                  case BOOL => rtBoxi1
                  case BYTE => rtBoxi8
                  case SHORT => rtBoxi16
                  case INT => rtBoxi32
                  case LONG => rtBoxi64
                  case FLOAT => rtBoxFloat
                  case DOUBLE => rtBoxDouble
                  case CHAR => rtBoxChar
                  case x => error("Don't know how to box "+x); rtBoxi1
                }
                val boxed = nextvar(fun.resultType)
                insns.append(new call(boxed, fun, Seq(unboxed)))
                stack.push((boxed,definitions.boxedClass(k.toType.typeSymbol)))
              }
              case UNBOX(k) => {
                val boxed = stack.pop
                val (fun,sym) = k match {
                  case BOOL => (rtUnboxi1, definitions.BooleanClass)
                  case BYTE => (rtUnboxi8, definitions.ByteClass)
                  case SHORT => (rtUnboxi16, definitions.ShortClass)
                  case INT => (rtUnboxi32, definitions.IntClass)
                  case LONG => (rtUnboxi64, definitions.LongClass)
                  case FLOAT => (rtUnboxFloat, definitions.FloatClass)
                  case DOUBLE => (rtUnboxDouble, definitions.DoubleClass)
                  case CHAR => (rtUnboxChar, definitions.CharClass)
                  case x => error("Don't know how to unbox "+x); (rtUnboxi1, definitions.BooleanClass)
                }
                val unboxed = nextvar(fun.resultType)
                insns.append(new call(unboxed, fun, Seq(cast(boxed._1,boxed._2,definitions.boxedClass(k.toType.typeSymbol)))))
                stack.push((unboxed, sym))
              }
            }
            insns.append(new icomment("stack after: "+stack.map{case (v,s) => v.rep + " " + s}.mkString(", ")))
          }

          // minus 2 to account for comment after
          val terminator = insns.remove(insns.length-2)

          locals.foreach { case (l,v) =>
            val reg = new LocalVariable(blockName(bb)+".local.out."+llvmName(l)+"."+l.id, v.tpe)
            insns.append(new select(reg, CTrue, v, v))
          }

          producedTypes.zip(stack.reverse).zipWithIndex.foreach { case ((sym,(src,ssym)),n) =>
            val tpe = symType(sym)
            if (definitions.isValueClass(sym) || sym.isTrait) {
              insns.append(new select(new LocalVariable(blockName(bb)+".out."+n.toString, tpe), CTrue, src, new CUndef(tpe)))
            } else {
              val asobj = new LocalVariable(blockName(bb)+".out."+n.toString, rtObject.pointer)
              insns.append(new bitcast(asobj, src))
            }
          }
          insns.append(terminator)
          blocks.append(LMBlock(Some(blockLabel(bb)), insns))
        }
        blocks.prepend(LMBlock(Some(Label("entry")), Seq(new br(blocks.head.label.get))))
        fun.define(blocks)
      }

      val name = llvmName(c.symbol)
      val outfile = getFile(c.symbol, ".ll")
      val outstream = new OutputStreamWriter(outfile.bufferedOutput,"US-ASCII")
      val header_comment = new Comment("Module for " + c.symbol.fullName('.'))
      val concreteMethods = c.methods.filter(!_.isDeferred)
      val (llvmmethods, methods) = concreteMethods.partition(_.symbol.hasAnnotation(LlvmimplAnnotSym))
      val methodFuns = methods.map(genFun)
      val llvmmethodFuns = llvmmethods.map(genNativeFun)
      c.methods.filter(_.native).map(_.symbol).foreach(externFun)
      val externDecls = externFuns.filterKeys(!internalFuns.contains(_)).values.map(_.declare)++externClasses.values.map(_.declare)
      val module = new Module(Seq(header_comment)++externTypes.groupBy(_.name).values.map(ats => new TypeAlias(ats.first))++rtHeader++externDecls++externModules.filterKeys(_.moduleClass!=c.symbol).values.map(_.declare)++classInfo++methodFuns++llvmmethodFuns++extraDefs++moduleInfo)
      outstream.write(module.syntax)
      outstream.write("\n")
      outstream.close()
    }

    def constValue(c: Constant): LMConstant[_<:ConcreteType] = {
      c.tag match {
        case UnitTag => new CUndef(LMVoid)
        case BooleanTag => c.booleanValue
        case ByteTag => c.byteValue
        case ShortTag => c.shortValue
        case CharTag => c.shortValue
        case IntTag => c.intValue
        case LongTag => c.longValue
        case FloatTag => c.floatValue
        case DoubleTag => c.doubleValue
        case StringTag => new CArray(LMInt.i8, (c.stringValue+"\0").getBytes("UTF-8").map(new CInt(LMInt.i8, _)))
        case _ => {
          warning("Can't handle " + c + " tagged " + c.tag)
          new CUndef(LMVoid)
        }
      }
    }

    def classType(s: Symbol): ConcreteType with AliasedType = {
      classes.get(s) match {
        case Some(c) => classType(c)
        case None => externalType(s)
      }
    }

    def classType(c: IClass): ConcreteType with AliasedType = {
      val s = c.symbol
      val fieldTypes = c.fields.map(fieldType _)
      val typelist = if (s.superClass != NoSymbol) classType(s.superClass)+:fieldTypes else fieldTypes
      new LMStructure(typelist).aliased(llvmName(s))
    }

    def fieldType(f: IField) = {
      symType(f.symbol)
    }

    def localType(l: Local) = {
      typeKindType(l.kind)
    }

    def typeKindType(tk: TypeKind) = {
      typeType(tk.toType)
    }

    def typeType(t: Type) = {
      t.typeSymbol match {
        case definitions.BooleanClass => LMInt.i1
        case definitions.ByteClass => LMInt.i8
        case definitions.ShortClass => LMInt.i16
        case definitions.IntClass => LMInt.i32
        case definitions.LongClass => LMInt.i64
        case definitions.FloatClass => LMFloat
        case definitions.DoubleClass => LMDouble
        case definitions.CharClass => LMInt.i16
        case definitions.UnitClass => LMVoid
        case x if x.isTrait => externalType(x)
        case x => externalType(x).pointer
      }
    }

    def externalType(s: Symbol) = {
      if (s == definitions.ObjectClass) {
        rtObject
      } else if (s.isTrait) {
        rtIfaceRef
      } else {
        LMOpaque.aliased(llvmName(s))
      }
    }

    def symType(s: Symbol): ConcreteType = {
      if (s.isMethod) {
        val baseArgTypes = s.tpe.paramTypes.map(typeType)
        val argTypes = if (s.isStaticMember) {
          baseArgTypes
        } else {
          val recvtpe = if (s.owner.isTrait) rtObject.pointer else symType(s.owner)
          recvtpe +: baseArgTypes
        }
        new LMFunctionType(
          if (s.isClassConstructor) LMVoid else typeType(s.tpe.resultType),
          argTypes,
          false)
      } else {
        typeType(s.tpe)
      }
    }

    def getFile(sym: Symbol, suffix: String): AbstractFile = {
      val sourceFile = atPhase(currentRun.phaseNamed("llvm").prev)(sym.sourceFile)
      val dir: AbstractFile = settings.outputDirs.outputDirFor(sourceFile)
      if (sym.isClassOfModule)
        dir.fileNamed(sym.fullName('_')+"#module" + suffix)
      else
        dir.fileNamed(sym.fullName('_') + suffix)
    }

    def llvmName(sym: Symbol): String = {
      if (sym.isClass && !sym.isModule && !sym.isClassOfModule)
        sym.fullName('.')
      else if (sym.isClassOfModule || (sym.isModule && !sym.isMethod))
	sym.fullName('.')+"!module"
      else if (sym.isMethod)
        llvmName(sym.owner)+"/"+sym.simpleName.toString.trim()+sym.info.paramss.flatten.map(ps => llvmName(ps.info.typeSymbol)).mkString("(",",",")")+llvmName(sym.info.resultType.typeSymbol)
      else
	sym.simpleName.toString.trim()
    }

    def blockLabel(bb: BasicBlock) = Label(blockName(bb))
    def blockName(bb: BasicBlock) = "bb."+bb.label

    def classInfoName(s: Symbol) = {
      ".classinfo."+llvmName(s)
    }

    def moduleInstanceName(s: Symbol) = {
      ".instance."+llvmName(s)
    }

    def reachable(b: BasicBlock): Boolean = reachable(b, immutable.BitSet.empty)

    def reachable(b: BasicBlock, memo: immutable.BitSet): Boolean = {
      val newmemo = memo + b.label
      (b.code.startBlock == b) || !b.predecessors.filter(p => !newmemo(p.label) && reachable(p,newmemo)).isEmpty
    }

    def stackUsage(bb: BasicBlock): (Seq[Symbol],Seq[Symbol]) = {
      stackUsage(bb, immutable.BitSet.empty)
    }

    def stackUsage(bb: BasicBlock, memo: immutable.BitSet): (Seq[Symbol],Seq[Symbol]) = {
      val (ict,ipt) = internalUsage(bb)
      val predextras = bb.predecessors.filter(pb => reachable(pb) && !memo(pb.label)).headOption match {
        case Some(p) => stackUsage(p, memo+bb.label)._2.dropRight(ict.length)
        case None => Seq.empty
      }
      (predextras++ict,predextras++ipt)
    }

    def internalUsage(bb: BasicBlock) = {
      /* first is deepest in produced stack */
      val produced = new mutable.ListBuffer[Symbol]
      val needed = new mutable.ListBuffer[Symbol]
      bb foreach { instruction =>
        val consumedTypes = instruction match {
          case RETURN(UNIT) => Seq.empty
          case RETURN(k) => Seq(k.toType.typeSymbol)
          case CJUMP(_,_,_,k) => Seq(k.toType.typeSymbol, k.toType.typeSymbol)
          case CALL_METHOD(m,SuperCall(_)) => Seq(m.owner.tpe.typeSymbol)
          case DROP(k) => Seq(k.toType.typeSymbol)
          case DUP(k) => Seq(k.toType.typeSymbol)
          case CZJUMP(_, _, _, k) => Seq(k.toType.typeSymbol)
          case THROW() => Seq(definitions.ThrowableClass)
          case CALL_PRIMITIVE(StringConcat(kind)) => Seq(NoSymbol, kind.toType.typeSymbol)
          case CALL_PRIMITIVE(EndConcat) => Seq(NoSymbol)
          case SWITCH(_,_) => Seq(definitions.IntClass)
          case i => i.consumedTypes.map(_.toType.typeSymbol)
        }
        val producedTypes = instruction match {
          case CALL_METHOD(m, _) => {
            if (m.tpe.resultType.typeSymbol == definitions.UnitClass)
              Seq.empty
            else if (m.isConstructor)
              Seq.empty
            else
              Seq(m.tpe.resultType.typeSymbol)
          }
          case BOX(bt) => Seq(bt.toType.typeSymbol)
          case UNBOX(bt) => Seq(bt.toType.typeSymbol)
          case DUP(k) => Seq(k.toType.typeSymbol,k.toType.typeSymbol)
          case NEW(k) => Seq(k.toType.typeSymbol)
          case IS_INSTANCE(_) => Seq(definitions.BooleanClass)
          case CALL_PRIMITIVE(StartConcat) => Seq(NoSymbol)
          case CALL_PRIMITIVE(StringConcat(_)) => Seq(NoSymbol)
          case CREATE_ARRAY(elem, dims) => Seq(definitions.ArrayClass)
          case i => i.producedTypes.map(_.toType.typeSymbol)
        }
        if (instruction.consumed != consumedTypes.length) {
          error("Consumption mismatch for " + instruction+": "+instruction.consumed.toString +" "+ consumedTypes)
        }
        if (instruction.produced != producedTypes.length) {
          error("Production mismatch for " + instruction+": "+instruction.produced.toString +" "+ producedTypes)
        }
        val takenInternally = consumedTypes.length.min(produced.length)
        if (takenInternally > 0) produced.remove(produced.length-takenInternally, takenInternally) else ()
        needed.appendAll(consumedTypes.dropRight(takenInternally))
        produced.appendAll(producedTypes)
      }
      (needed.toList,produced.toList)
    }

  }
}
