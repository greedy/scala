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

    def localCell(l: Local) = LocalVariable("local."+llvmName(l.sym)+"."+l.sym.id, typeKindType(l.kind).pointer)

    object Runtime {
      /* Types */

      lazy val rtIfaceRef: LMStructure with AliasedType =
        new LMStructure(Seq(
          rtObject.pointer,
          rtVtable
        )).aliased(".ifaceref")

      lazy val rtVtable = LMInt.i8.pointer.pointer.aliased(".vtable")

      lazy val rtIfaceInfo =
        new LMStructure(Seq(
          rtClass.pointer,
          rtVtable
        )).aliased(".ifaceinfo")

      lazy val rtClass: LMStructure with AliasedType =
        new LMStructure(Seq(
          new LMStructure(Seq(LMInt.i32, LMInt.i8.pointer)),
          LMInt.i32,
          rtClass.pointer,
          rtVtable,
          rtClass.pointer,
          rtClass.pointer,
          LMInt.i32,
          new LMArray(0, rtIfaceInfo)
        )).aliased(".class")

      lazy val rtObject =
        new LMStructure(Seq(
          rtClass.pointer
        )).aliased(".object")

      /* Functions */

      lazy val rtNew =
        new LMFunction(
          rtObject.pointer, "rt_new",
          Seq(
            ArgSpec(new LocalVariable("cls", rtClass.pointer))
          ), false,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      lazy val rtInitobj =
        new LMFunction(
          LMVoid, "rt_initobj",
          Seq(
            ArgSpec(new LocalVariable("object", rtObject.pointer)),
            ArgSpec(new LocalVariable("cls", rtClass.pointer))
          ), false,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      lazy val rtInitLoop =
        new LMFunction(
          LMVoid, "rt_init_loop",
          Seq.empty, false,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      lazy val rtNewArray =
        new LMFunction(
          symType(definitions.ArrayClass), "new_array",
          Seq(
            new LocalVariable("kind", LMInt.i8),
            new LocalVariable("etype", rtClass.pointer),
            new LocalVariable("ndims", LMInt.i32),
            new LocalVariable("dim0", LMInt.i32)
          ), /* dim1, dim2, ..., dimn */ true,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      def rtArrayKind(tk: TypeKind): Byte = tk match {
        case BOOL =>          0
        case BYTE =>          1
        case SHORT =>         2
        case CHAR =>          3
        case INT =>           4
        case LONG =>          5
        case FLOAT =>         6
        case DOUBLE =>        7
        case REFERENCE(_) =>  8
        case ARRAY(_) =>      9
      }

      def sappendfn(t: Symbol) = new LMFunction(
        LMVoid, "rt_string_append_"+t.simpleName,
        Seq(
          ArgSpec(new LocalVariable("s", LMInt.i8.pointer.pointer)),
          ArgSpec(new LocalVariable("v", symType(t)))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtAppendBool = sappendfn(definitions.BooleanClass)
      lazy val rtAppendByte = sappendfn(definitions.ByteClass)
      lazy val rtAppendShort = sappendfn(definitions.ShortClass)
      lazy val rtAppendChar = sappendfn(definitions.CharClass)
      lazy val rtAppendInt = sappendfn(definitions.IntClass)
      lazy val rtAppendLong = sappendfn(definitions.LongClass)
      lazy val rtAppendFloat = sappendfn(definitions.FloatClass)
      lazy val rtAppendDouble = sappendfn(definitions.DoubleClass)
      lazy val rtAppendString = new LMFunction(
        LMVoid, "rt_string_append_string",
        Seq(
          ArgSpec(new LocalVariable("s", LMInt.i8.pointer.pointer)),
          ArgSpec(new LocalVariable("sobj", rtObject.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      def boxfn(c: Symbol, lt: ConcreteType) = new LMFunction(
        symType(c), "rt_box_"+c.simpleName,
        Seq(
          ArgSpec(new LocalVariable("v", lt))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)
      def unboxfn(c: Symbol, lt: ConcreteType) = new LMFunction(
        lt, "rt_unbox_"+c.simpleName,
        Seq(
          ArgSpec(new LocalVariable("v", symType(c)))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtBoxi1 = boxfn(definitions.BoxedBooleanClass, LMInt.i1)
      lazy val rtBoxi8 = boxfn(definitions.BoxedByteClass, LMInt.i8)
      lazy val rtBoxi16 = boxfn(definitions.BoxedShortClass, LMInt.i16)
      lazy val rtBoxi32 = boxfn(definitions.BoxedIntClass, LMInt.i32)
      lazy val rtBoxi64 = boxfn(definitions.BoxedLongClass, LMInt.i64)
      lazy val rtBoxFloat = boxfn(definitions.BoxedFloatClass, LMFloat)
      lazy val rtBoxDouble = boxfn(definitions.BoxedDoubleClass, LMDouble)
      lazy val rtBoxChar = boxfn(definitions.BoxedCharacterClass, LMInt.i32)
      lazy val rtUnboxi1 = unboxfn(definitions.BoxedBooleanClass, LMInt.i1)
      lazy val rtUnboxi8 = unboxfn(definitions.BoxedByteClass, LMInt.i8)
      lazy val rtUnboxi16 = unboxfn(definitions.BoxedShortClass, LMInt.i16)
      lazy val rtUnboxi32 = unboxfn(definitions.BoxedIntClass, LMInt.i32)
      lazy val rtUnboxi64 = unboxfn(definitions.BoxedLongClass, LMInt.i64)
      lazy val rtUnboxFloat = unboxfn(definitions.BoxedFloatClass, LMFloat)
      lazy val rtUnboxDouble = unboxfn(definitions.BoxedDoubleClass, LMDouble)
      lazy val rtUnboxChar = unboxfn(definitions.BoxedCharacterClass, LMInt.i32)

      lazy val rtMakeString =
        new LMFunction(
          symType(definitions.StringClass), "rt_makestring",
          Seq(
            ArgSpec(new LocalVariable("s", new LMStructure(Seq(LMInt.i32, LMInt.i8.pointer)).pointer))
          ), false,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      lazy val rtStringConcat =
        new LMFunction(
          symType(definitions.StringClass), "rt_stringconcat",
          Seq(
            ArgSpec(new LocalVariable("b", LMInt.i8.pointer.pointer))
          ), false,
          Externally_visible, Default, Ccc,
          Seq.empty, Seq.empty, None, None, None)

      lazy val rtIfaceCast = new LMFunction(
        rtIfaceRef, "rt_iface_cast",
        Seq(
          ArgSpec(new LocalVariable("obj", rtObject.pointer)),
          ArgSpec(new LocalVariable("iface", rtClass.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtIsinstance = new LMFunction(
        LMInt.i1, "rt_isinstance",
        Seq(
          ArgSpec(new LocalVariable("obj", rtObject.pointer)),
          ArgSpec(new LocalVariable("clsoriface", rtClass.pointer))
        ), false,
        Externally_visible, Default,
        Ccc, Seq.empty, Seq.empty, None, None, None)

      lazy val rtIsinstanceIface = new LMFunction(
        LMInt.i1, "rt_isinstance_iface",
        Seq(
          ArgSpec(new LocalVariable("obj", rtObject.pointer)),
          ArgSpec(new LocalVariable("iface", rtClass.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtIsinstanceClass = new LMFunction(
        LMInt.i1, "rt_isinstance_class",
        Seq(
          ArgSpec(new LocalVariable("obj", rtObject.pointer)),
          ArgSpec(new LocalVariable("cls", rtClass.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      /* Constants */

      lazy val rtBoxedUnit = new LMGlobalVariable(
        ".rt.boxedUnit", symType(definitions.BoxedUnitClass),
        Externally_visible, Default, true)

      /* Exception Handling */

      lazy val scalaPersonality = new LMFunction(
        LMInt.i32, "scalaPersonality",
        Seq(
          ArgSpec(new LocalVariable("a", LMInt.i32)),
          ArgSpec(new LocalVariable("b", LMInt.i32)),
          ArgSpec(new LocalVariable("c", LMInt.i64)),
          ArgSpec(new LocalVariable("d", LMInt.i8.pointer)),
          ArgSpec(new LocalVariable("e", LMInt.i8.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val llvmEhException = new LMFunction(
        LMInt.i8.pointer, "llvm.eh.exception",
        Seq.empty, false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val llvmEhSelector = new LMFunction(
        LMInt.i32, "llvm.eh.selector",
        Seq(
          ArgSpec(new LocalVariable("e", LMInt.i8.pointer)),
          ArgSpec(new LocalVariable("p", LMInt.i8.pointer))
        ), true,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtGetExceptionObject = new LMFunction(
        rtObject.pointer, "getExceptionObject",
        Seq(
          ArgSpec(new LocalVariable("uwx", LMInt.i8.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val unwindResume = new LMFunction(
        LMVoid, "_Unwind_Resume",
        Seq(
          ArgSpec(new LocalVariable("e", LMInt.i8.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val unwindRaiseException = new LMFunction(
        LMInt.i32, "_Unwind_RaiseException",
        Seq(
          ArgSpec(new LocalVariable("e", LMInt.i8.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val createOurException = new LMFunction(
        LMInt.i8.pointer, "createOurException",
        Seq(
          ArgSpec(new LocalVariable("e", rtObject.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtAssertNotNull = new LMFunction(
        LMVoid, "rt_assertNotNull",
        Seq(
          ArgSpec(new LocalVariable("o", rtObject.pointer))
        ), false,
        Externally_visible, Default, Ccc,
        Seq.empty, Seq.empty, None, None, None)

      lazy val rtTypes = Seq(
        definitions.BoxedBooleanClass,
        definitions.BoxedByteClass,
        definitions.BoxedShortClass,
        definitions.BoxedIntClass,
        definitions.BoxedLongClass,
        definitions.BoxedFloatClass,
        definitions.BoxedDoubleClass,
        definitions.StringClass,
        definitions.BoxedCharacterClass,
        definitions.BoxedUnitClass,
        definitions.ArrayClass
      )
      lazy val rtHeader: Seq[ModuleComp] = Seq(
        new TypeAlias(rtVtable),
        new TypeAlias(rtClass),
        new TypeAlias(rtIfaceInfo),
        new TypeAlias(rtIfaceRef),
        scalaPersonality.declare,
        rtNew.declare,
        rtInitobj.declare,
        rtInitLoop.declare,
        rtNewArray.declare,
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
        rtAppendBool.declare,
        rtAppendByte.declare,
        rtAppendShort.declare,
        rtAppendChar.declare,
        rtAppendInt.declare,
        rtAppendLong.declare,
        rtAppendFloat.declare,
        rtAppendDouble.declare,
        rtAppendString.declare,
        rtStringConcat.declare,
        rtIsinstance.declare,
        rtIsinstanceIface.declare,
        rtIsinstanceClass.declare,
        rtIfaceCast.declare,
        rtBoxedUnit.declare,
        llvmEhException.declare,
        llvmEhSelector.declare,
        rtGetExceptionObject.declare,
        unwindResume.declare,
        unwindRaiseException.declare,
        createOurException.declare,
        rtAssertNotNull.declare
      )
    }

    import Runtime._

    def isMain(c: IClass) = c.symbol.isModuleClass && c.symbol.fullName('.') == settings.Xmainclass.value

    def genClass(c: IClass) {
      println("Generating " + c)
      val externFuns: mutable.Map[Symbol,LMFunction] = new mutable.HashMap
      val externClasses: mutable.Map[Symbol,LMGlobalVariable[_<:ConcreteType]] = new mutable.HashMap
      val internalFuns: mutable.Set[Symbol] = new mutable.HashSet
      val internalClasses: mutable.Set[Symbol] = new mutable.HashSet
      val extraDefs: mutable.ListBuffer[ModuleComp] = new mutable.ListBuffer
      val externModules: mutable.Map[Symbol,LMGlobalVariable[_<:ConcreteType]] = new mutable.HashMap
      val externTypes: mutable.Map[String,AliasedType] = new mutable.HashMap
      val recorded: mutable.Set[LMType] = new mutable.HashSet

      var globidx = 0

      c.symbol.getAnnotation(LlvmdefsAnnotSym) match {
        case Some(AnnotationInfo(_, List(Literal(Constant(s: String))), _)) => extraDefs += new ModuleComp { val syntax = s }
        case Some(x) => error("Invalid llvmdefs annotation")
        case None => ()
      }

      def nextconst(v: LMConstant[_<:ConcreteType], linkage: Linkage = Private) = {
        val gv = new LMGlobalVariable(".global."+globidx.toString, v.tpe, linkage, Default, false)
        extraDefs += gv.define(v)
        globidx = globidx + 1
        gv
      }

      def stringConstant(s: String) = {
        val bytes = s.getBytes("UTF-8").map(new CInt(LMInt.i8, _))
        val bytesconst = nextconst(new CArray(LMInt.i8, bytes))
        new CStruct(Seq(
          LMConstant.intconst(bytes.length),
          new Cgetelementptr(new CGlobalAddress(bytesconst), Seq(LMConstant.intconst(0), LMConstant.intconst(0)), LMInt.i8.pointer)
        ))
      }

      def constValue(c: Constant): LMConstant[_<:ConcreteType] = {
        c.tag match {
          case UnitTag => new CGlobalAddress(rtBoxedUnit)
          case BooleanTag => c.booleanValue
          case ByteTag => c.byteValue
          case ShortTag => c.shortValue
          case CharTag => c.intValue
          case IntTag => c.intValue
          case LongTag => c.longValue
          case FloatTag => c.floatValue
          case DoubleTag => c.doubleValue
          case StringTag => stringConstant(c.stringValue)
          case NullTag if c.tpe.typeSymbol.isTrait => new CStruct(Seq(new CNull(rtObject.pointer), new CNull(rtVtable)))
          case NullTag => new CNull(rtObject.pointer)
          case _ => {
            warning("Can't handle " + c + " tagged " + c.tag)
            new CUndef(typeType(c.tpe))
          }
        }
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
          case t:LMOpaque with AliasedType => {
            if (!externTypes.contains(t.name)) {
              externTypes.put(t.name, t)
            }
          }
          case t:AliasedType => {
            externTypes.put(t.name, t)
          }
          case _ => ()
        }
        if (!recorded(t)) {
          recorded += t
          t match {
            case s:LMStructure => s.types.foreach(recordType)
            case s:LMPackedStructure => s.types.foreach(recordType)
            case u:LMUnion => u.types.foreach(recordType)
            case a:LMArray => recordType(a.elementtype)
            case p:LMPointer => recordType(p.target)
            case f:LMFunctionType => { recordType(f.returnType); f.argTypes.foreach(recordType) }
            case _ => ()
          }
        }
      }

      classes.values.foreach(tc => recordType(classType(tc)))
      rtTypes.foreach(t => recordType(symType(t)))

      def moduleInitFun(s: Symbol) = {
        new LMFunction(LMVoid, "initmodule_"+moduleInstanceName(s), Seq.empty, false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
      }

      def externModule(s: Symbol) = {
        recordType(symType(s))
        if (c.symbol == s) {
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
          new LMFunction(funtype.returnType, "method_"+llvmName(s), args, false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
        })
      }

      def externClassP(s: Symbol) = new Cbitcast(new CGlobalAddress(externClass(s)), rtClass.pointer)

      def externClass(s: Symbol) = {
        if (c.symbol == s) {
          new LMGlobalVariable(classInfoName(s), LMOpaque.aliased("thisclass"), Externally_visible, Default, true)
        } else {
          externClasses.getOrElseUpdate(s, {
            new LMGlobalVariable(classInfoName(s), rtClass, Externally_visible, Default, true)
          })
        }
      }

      def virtualMethods(s: Symbol): List[Symbol] = {
        if (s == NoSymbol) Nil
        else {
          val myvirts = s.info.decls.toList.filter(d => d.isMethod && !d.isConstructor && !d.isOverride && !d.isEffectivelyFinal)
          (virtualMethods(s.superClass) ++ myvirts.sortBy(methodSig _)).toList
        }
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
        val prefix = if (c.symbol.isModuleClass || c.symbol.isModule) "module " else ""
        val n = stringConstant(prefix+c.symbol.fullName('.'))
        val ci = new CStruct(Seq(n,
                                 new Cptrtoint(new Cgetelementptr(new CNull(ct.pointer), Seq[LMConstant[LMInt]](1), ct.pointer), LMInt.i32),
                                 externClassP(c.symbol.superClass),
                                 new Cgetelementptr(vtableg, Seq[CInt](0,0), rtVtable),
                                 new CNull(rtClass.pointer),
                                 new CNull(rtClass.pointer),
                                 new CInt(LMInt.i32, traitinfo.length),
                                 new CArray(rtIfaceInfo, traits.zip(traitinfo).map{ case (t, (tvg, _)) => new CStruct(Seq(externClassP(t), new Cgetelementptr(new CGlobalAddress(tvg), Seq[CInt](0,0), rtVtable)))})))
        val cig = new LMGlobalVariable[LMStructure](classInfoName(c.symbol), ci.tpe, Externally_visible, Default, true)
        Seq.concat(
          Seq(
            new TypeAlias(cig.tpe.aliased("thisclass")),
            cig.define(ci),
            vtableg.define(vtable)
          ),
          traitinfo.map(ti => ti._1.define(ti._2))
        )
      }

      val moduleInfo: Seq[ModuleComp] = {
        if (c.symbol.isModuleClass && !c.symbol.isImplClass) {
          val ig = new LMGlobalVariable(moduleInstanceName(c.symbol), symType(c.symbol).asInstanceOf[LMPointer].target, Externally_visible, Default, false)
          val initStarted = new LMGlobalVariable("init_started", LMInt.i1, Internal, Default, false)
          val initFinished = new LMGlobalVariable("init_finished", LMInt.i1, Internal, Default, false)
          val initFun = moduleInitFun(c.symbol)
          val asinstance = new LocalVariable("instance", symType(c.symbol).asInstanceOf[LMPointer])
          val asvalue = new LocalVariable("value", symType(c.symbol).asInstanceOf[LMPointer].target)
          val initStartedv = new LocalVariable("init_started_v", LMInt.i1)
          val initFinishedv = new LocalVariable("init_finished_v", LMInt.i1)
          val initFundef = initFun.define(Seq(
            LMBlock(Some(Label("start")), Seq(
              new load(initStartedv, new CGlobalAddress(initStarted)),
              new load(initFinishedv, new CGlobalAddress(initFinished)),
              new br_cond(initFinishedv, Label("finished"), Label("not_finished")))),
            LMBlock(Some(Label("finished")), Seq(
              retvoid)),
            LMBlock(Some(Label("not_finished")), Seq(
              new br_cond(initStartedv, Label("started"), Label("not_started")))),
            LMBlock(Some(Label("started")), Seq(
              new call_void(rtInitLoop, Seq.empty),
              unreachable)),
            LMBlock(Some(Label("not_started")), Seq(
              new store(LMConstant.boolconst(true), new CGlobalAddress(initStarted)),
              new call_void(rtInitobj, Seq(new Cbitcast(new CGlobalAddress(ig), rtObject.pointer), externClassP(c.symbol))),
              new call_void(externFun(c.lookupMethod("<init>").get.symbol), Seq(new CGlobalAddress(ig))),
              new store(LMConstant.boolconst(true), new CGlobalAddress(initFinished)),
              retvoid
            ))))
          //val ctors = new LMGlobalVariable("llvm.global_ctors", new LMArray(1, new LMStructure(Seq(LMInt.i32, new LMFunctionType(LMVoid, Seq.empty, false).pointer))), Appending, Default, false)
          //val ctorsdef = ctors.define(new CArray(ctors.tpe.elementtype, Seq(new CStruct(Seq(new CInt(LMInt.i32, 1), new CFunctionAddress(initFun))))))
          Seq(
            ig.define(new CUndef(ig.tpe)),
            initStarted.define(false),
            initFinished.define(false),
            initFundef)
        } else {
          Seq.empty
        }
      }

      def genNativeFun(m: IMethod) = {
        val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
        val args = thisarg +: m.params.map(p => ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))))
        val fun = new LMFunction(typeType(m.returnType.toType), "method_"+llvmName(m.symbol), args, false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
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
        val recvarg = if (m.symbol.isStaticMember) { Seq.empty } else { Seq((thisarg, c.symbol)) }
        val args = recvarg ++ m.params.map(p => (ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))), p.sym))
        val fun = new LMFunction(typeType(m.returnType.toType), "method_"+llvmName(m.symbol), args.map(_._1), false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
        recordType(fun.tpe)
        fun.args.map(_.lmvar.tpe).foreach(recordType)
        if (isMain(c) && m.params.isEmpty && m.symbol.simpleName.toString().trim() == "main" && fun.resultType == LMVoid) {
          extraDefs += generateMain(fun)
        }
        val blocks: mutable.ListBuffer[LMBlock] = new mutable.ListBuffer
        val unreachableBlock = Label("_unreachable_")
        blocks.append(new LMBlock(Some(unreachableBlock), Seq(unreachable)))
        var varidx = 0
        def nextvar[T <: ConcreteType](t: T) = {
          val v = new LocalVariable(varidx.toString, t)
          varidx = varidx + 1
          v
        }

        val currentException = new LocalVariable("_currentException", rtObject.pointer.pointer)

        def exSels(bb: BasicBlock): Seq[LMBlock] = {
          val uwx = nextvar(LMInt.i8.pointer)
          val selres = nextvar(LMInt.i32)
          val exval = nextvar(rtObject.pointer)
          val handlers = bb.method.exh.filter(_.covers(bb))
          val header =
            LMBlock(Some(blockExSelLabel(bb, -2)), Seq(
              new call(uwx, llvmEhException, Seq.empty),
              new call(selres, llvmEhSelector, Seq(
                uwx, new Cbitcast(new CFunctionAddress(scalaPersonality), LMInt.i8.pointer), externClassP(definitions.ThrowableClass), LMConstant.intconst(0))),
              new call(exval, rtGetExceptionObject, Seq(uwx)),
              new store(exval, currentException),
              new br(blockExSelLabel(bb, 0))))
          val hblocks = handlers.zipWithIndex.map { case (h,n) =>
            val isinst = nextvar(LMInt.i1)
            val exi = nextvar(rtObject.pointer)
            LMBlock(Some(blockExSelLabel(bb,n)), Seq(
              new call(isinst, rtIsinstance, Seq(exval, externClassP(h.loadExceptionClass))),
              new br_cond(isinst, blockLabel(h.startBlock), blockExSelLabel(bb, n+1))
            ))
          }
          val junk = nextvar(LMInt.i32)
          val footer = LMBlock(Some(blockExSelLabel(bb, handlers.length)), Seq(
            new call(junk, unwindRaiseException, Seq(uwx)),
            unreachable))
          Seq(header) ++ hblocks ++ Seq(footer)
        }

        m.code.blocks.filter(reachable).foreach { bb =>
          val stack: mutable.Stack[(LMValue[_<:ConcreteType],TypeKind)] = mutable.Stack()
          def popn(n: Int) {
            for (_ <- 0 until n) stack.pop
          }
          val insns: mutable.ListBuffer[LMInstruction] = new mutable.ListBuffer
          val pass = Label("__PASS__")
          val excpreds = bb.code.blocks.filter(pb => pb.exceptionSuccessors.contains(bb))
          val dirpreds = bb.code.blocks.filter(_.directSuccessors contains bb)
          val preds = (excpreds ++ dirpreds).filter(reachable)
          insns.append(new icomment("predecessors: "+preds.map(_.fullString).mkString(",")+" successors: "+bb.successors.map(_.fullString).mkString(" ")))
          def cast(src: LMValue[_<:ConcreteType], srctk: TypeKind, targettk: TypeKind): LMValue[_<:ConcreteType] = {
            /* FIXME */
            val srcsym = srctk.toType.typeSymbol
            val targetsym = targettk.toType.typeSymbol
            insns.append(new icomment("cast "+src.rep+" from "+srcsym+" to "+targetsym))
            if (srcsym == targetsym) {
              src
            } else {
              recordType(symType(targetsym))
              if (targetsym.isTrait) {
                if (srcsym.isTrait) {
                  cast(cast(src, srctk, REFERENCE(definitions.ObjectClass)), REFERENCE(definitions.ObjectClass), targettk)
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
                      insns.append(new getelementptr(vtblptr, clsptr, Seq[LMConstant[LMInt]](0, 7, tgtidx, 1)))
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
                    cast(cast(src, srctk, REFERENCE(definitions.ObjectClass)), REFERENCE(definitions.ObjectClass), targettk)
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
          insns.append(new icomment("blockinfo: " + bb.fullString))
          insns.append(new icomment("direct successors: " + bb.directSuccessors.map(_.fullString).mkString("; ")))
          insns.append(new icomment("exception successors: " + bb.exceptionSuccessors.map(_.fullString).mkString("; ")))
          insns.append(new icomment("indirect exception successors: " + bb.indirectExceptionSuccessors.map(_.fullString).mkString("; ")))
          val predcasts = new mutable.ListBuffer[LMInstruction]
          consumedTypes.reverse.zipWithIndex.foreach { case (sym,n) =>
            val tpe = symType(sym)
            recordType(tpe)
            if (definitions.isValueClass(sym) || sym.isTrait) {
              val reg = new LocalVariable(blockName(bb)+".in."+n.toString,tpe)
              val dirsources = dirpreds.map(pred => (blockLabel(pred,-1), new LocalVariable(blockName(pred)+".out."+n.toString,tpe)))
              val excsources = excpreds.filter(reachable).map(pred => (blockExSelLabel(pred,pred.method.exh.filter(_.covers(pred)).findIndexOf(_.startBlock == bb)), new LocalVariable(blockName(pred)+".out."+n.toString,tpe)))
              val sources = dirsources ++ excsources
              insns.append(new phi(reg, sources))
              stack.push((reg, toTypeKind(sym.tpe)))
            } else {
              val reg = new LocalVariable(blockName(bb)+".in."+n.toString,tpe)
              val asobj = nextvar(rtObject.pointer)
              val dirsources = dirpreds.filter(reachable).map(pred => (blockLabel(pred,-1), new LocalVariable(blockName(pred)+".out."+n.toString,rtObject.pointer)))
              val excsources = excpreds.filter(reachable).map(pred => (blockExSelLabel(pred,pred.method.exh.filter(_.covers(pred)).findIndexOf(_.startBlock == bb)), new LocalVariable(blockName(pred)+".out."+n.toString,rtObject.pointer)))
              val sources = dirsources ++ excsources
              insns.append(new phi(asobj, sources))
              predcasts.append(new bitcast(reg, asobj))
              stack.push((reg, toTypeKind(sym.tpe)))
            }
          }

          insns.appendAll(predcasts)
          val headinsns = insns.toList
          insns.clear()
          bb.foreach { i =>
            insns.append(new icomment(i.toString))
            insns.append(new icomment("stack before: "+stack.map{case (v,s) => v.rep + " " + s}.mkString(", ")))
            i match {
              case THIS(clasz) => {
                stack.push((thisarg.lmvar,REFERENCE(clasz)))
              }
              case CONSTANT(const) => {
                val value = constValue(const)
                if (const.tag == StringTag) {
                    val g = nextconst(value)
                    val v = nextvar(rtMakeString.resultType)
                    insns.append(new call(v, rtMakeString, Seq(new CGlobalAddress(g))))
                    stack.push((v,toTypeKind(const.tpe)))
                } else {
                  stack.push((value,toTypeKind(const.tpe)))
                }
              }
              case LOAD_ARRAY_ITEM(kind) => {
                val arraylmtype = arrayType(kind.toType.typeSymbol)
                val index = stack.pop._1
                val array = stack.pop._1
                val asarray = nextvar(arraylmtype)
                val asobj = nextvar(rtObject.pointer)
                val itemptr = nextvar(typeKindType(kind).pointer)
                val item = nextvar(typeKindType(kind))
                insns.append(new bitcast(asobj, array))
                insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                insns.append(new bitcast(asarray, array))
                insns.append(new getelementptr(itemptr, asarray.asInstanceOf[LMValue[LMPointer]],
                  Seq(LMConstant.intconst(0), LMConstant.intconst(2), index.asInstanceOf[LMValue[LMInt]])))
                insns.append(new load(item, itemptr))
                stack.push((item, kind))
              }
              case LOAD_LOCAL(local) => {
                val v = nextvar(typeKindType(local.kind))
                insns.append(new load(v, localCell(local)))
                stack.push((v, local.kind))
              }
              case LOAD_FIELD(field, isStatic) if (field == definitions.BoxedUnit_UNIT) => {
                stack.push((new CGlobalAddress(rtBoxedUnit), REFERENCE(definitions.BoxedUnitClass)))
              }
              case i@LOAD_FIELD(field, isStatic) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                fields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    val (ivar,isym) = stack.pop
                    val instance = cast(ivar,isym,toTypeKind(field.owner.tpe))
                    val asobj = nextvar(rtObject.pointer)
                    insns.append(new bitcast(asobj, instance))
                    insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                    insns.append(new getelementptr(fieldptr, instance.asInstanceOf[LMValue[LMPointer]], Seq(new CInt(LMInt.i8,0),new CInt(LMInt.i32,fieldidx+1))))
                    insns.append(new load(v, fieldptr))
                    stack.push((v,toTypeKind(field.tpe)))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                    m.dump
                    stack.push((new CUndef(v.tpe), REFERENCE(definitions.ObjectClass)))
                }
              }
              case LOAD_MODULE(module) => {
                insns.append(new call_void(moduleInitFun(module), Seq.empty))
                stack.push((new CGlobalAddress(externModule(module)), REFERENCE(module)))
              }
              case STORE_ARRAY_ITEM(kind) => {
                val arraylmtype = arrayType(kind.toType.typeSymbol)
                val item = stack.pop._1
                val index = stack.pop._1
                val array = stack.pop._1
                val asarray = nextvar(arraylmtype)
                val itemptr = nextvar(typeKindType(kind).pointer)
                insns.append(new bitcast(asarray, array))
                val asobj = nextvar(rtObject.pointer)
                insns.append(new bitcast(asobj, array))
                insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                insns.append(new getelementptr(itemptr, asarray.asInstanceOf[LMValue[LMPointer]],
                  Seq(LMConstant.intconst(0), LMConstant.intconst(2), index.asInstanceOf[LMValue[LMInt]])))
                insns.append(new store(item, itemptr))
              }
              case STORE_LOCAL(local) => {
                val (v,s) = stack.pop
                insns.append(new store(cast(v,s,local.kind), localCell(local)))
              }
              case STORE_FIELD(field, isStatic) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                fields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    val (value,valuesym) = stack.pop
                    val instance = stack.pop._1
                    val asobj = nextvar(rtObject.pointer)
                    insns.append(new bitcast(asobj, instance))
                    insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                    insns.append(new getelementptr(fieldptr, instance.asInstanceOf[LMValue[LMPointer]], Seq(new CInt(LMInt.i8,0),new CInt(LMInt.i32,fieldidx+1))))
                    insns.append(new store(cast(value, valuesym, toTypeKind(field.tpe)), fieldptr))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                }
              }
              case CALL_PRIMITIVE(primitive) => {
                primitive match {
                  case Negation(_) => {
                    val (arg,argtk) = stack.pop
                    val v = nextvar(arg.tpe)
                    arg.tpe match {
                      case i:LMInt => insns.append(new mul(v.asInstanceOf[LocalVariable[LMInt]], arg.asInstanceOf[LMValue[LMInt]], new CInt(i,-1)))
                      case f:LMFloat => insns.append(new fmul(v.asInstanceOf[LocalVariable[LMFloat]], arg.asInstanceOf[LMValue[LMFloat]], new CFloat(-1)))
                      case d:LMDouble => insns.append(new fmul(v.asInstanceOf[LocalVariable[LMDouble]], arg.asInstanceOf[LMValue[LMDouble]], new CDouble(-1)))
                    }
                    stack.push((v,argtk))
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
                    stack.push((result, BOOL))
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
                    stack.push((result, INT))
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
                    val shiftamt = stack.pop._1.asInstanceOf[LMValue[LMInt]]
                    val (argt,ressym) = stack.pop
                    val arg = argt.asInstanceOf[LMValue[LMInt]]
                    val temp = {
                      if (arg.tpe.bits > shiftamt.tpe.bits) {
                        val t = nextvar(arg.tpe)
                        insns.append(new zext(t, shiftamt))
                        t
                      } else {
                        shiftamt
                      }
                    }
                    val result = nextvar(arg.tpe)
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
                    var result: LMValue[ConcreteType] = nextvar(dt)
                    dt match {
                      case dit:LMInt => {
                        src.tpe match {
                          case sit:LMInt => if (dit.bits > sit.bits) {
                            insns.append(new sext(result.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMInt]]))
                          } else if (dit.bits < sit.bits) {
                            insns.append(new trunc(result.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMInt]]))
                          } else {
                            result = src
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
                    stack.push((result,dk))
                  }
                  case ArrayLength(kind) => {
                    val arraylmtype = arrayType(kind.toType.typeSymbol)
                    val array = stack.pop._1
                    val asarray = nextvar(arraylmtype)
                    val lenptr = nextvar(LMInt.i32.pointer)
                    val len = nextvar(LMInt.i32)
                    val asobj = nextvar(rtObject.pointer)
                    insns.append(new bitcast(asobj, array))
                    insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                    insns.append(new bitcast(asarray, array))
                    insns.append(new getelementptr(lenptr, asarray.asInstanceOf[LMValue[LMPointer]],
                      Seq(LMConstant.intconst(0), LMConstant.intconst(1))))
                    insns.append(new load(len, lenptr))
                    stack.push((len, INT))
                  }
                  case StartConcat => {
                    val sp = nextvar(LMInt.i8.pointer.pointer)
                    insns.append(new alloca(sp, LMInt.i8.pointer))
                    insns.append(new store(new CNull(LMInt.i8.pointer), sp))
                    stack.push((sp, REFERENCE(definitions.ObjectClass)))
                  }
                  case StringConcat(kind) if kind.isValueType => {
                    val fun = kind match {
                      case BOOL => rtAppendBool
                      case BYTE => rtAppendByte
                      case SHORT => rtAppendShort
                      case CHAR => rtAppendChar
                      case INT => rtAppendInt
                      case LONG => rtAppendLong
                      case FLOAT => rtAppendFloat
                      case DOUBLE => rtAppendDouble
                    }
                    val v = stack.pop._1
                    val s = stack.top match {
                      case (ss,_) if ss.tpe == LMInt.i8.pointer.pointer => ss
                      case (so,_) => {
                        val ss = nextvar(LMInt.i8.pointer.pointer)
                        insns.append(new bitcast(ss, so))
                        ss
                      }
                    }
                    insns.append(new call_void(fun, Seq(s, v)))
                  }
                  case StringConcat(kind) if !kind.isValueType => {
                    val (v,vs) = stack.pop
                    val s = stack.top match {
                      case (ss,_) if ss.tpe == LMInt.i8.pointer.pointer => ss
                      case (so,_) => {
                        val ss = nextvar(LMInt.i8.pointer.pointer)
                        insns.append(new bitcast(ss, so))
                        ss
                      }
                    }
                    val asobj = nextvar(rtObject.pointer)
                    if (vs.toType.typeSymbol.isTrait) {
                      val ifaceinfo = v.asInstanceOf[LMValue[rtIfaceRef.type]]
                      insns.append(new extractvalue(asobj, ifaceinfo, Seq(LMConstant.intconst(0))))
                    } else {
                      insns.append(new bitcast(asobj, v))
                    }
                    insns.append(new invoke_void(rtAppendString, Seq(s, asobj), pass, blockExSelLabel(bb,-2)))
                  }
                  case EndConcat => {
                    val s = stack.pop match {
                      case (ss,_) if ss.tpe == LMInt.i8.pointer.pointer => ss
                      case (so,_) => {
                        val ss = nextvar(LMInt.i8.pointer.pointer)
                        insns.append(new bitcast(ss, so))
                        ss
                      }
                    }
                    val v = nextvar(rtStringConcat.resultType)
                    insns.append(new call(v, rtStringConcat, Seq(s)))
                    stack.push((v, REFERENCE(definitions.StringClass)))
                  }
                  case _ => warning("unsupported primitive op " + primitive)
                }
              }
              case CALL_METHOD(method, style) => {
                val funtype = symType(method).asInstanceOf[LMFunctionType]
                val contextargs = style match {
                  case Static(true) | Dynamic | SuperCall(_) => if (method.owner.isTrait) Seq(REFERENCE(definitions.ObjectClass)) else Seq(toTypeKind(method.owner.tpe))
                  case Static(false) => Seq.empty
                }
                val argsyms = contextargs ++ method.tpe.paramTypes.map(toTypeKind)
                val args = stack.take(funtype.argTypes.size).reverse.toBuffer
                insns.append(new invoke_void(rtAssertNotNull, Seq(cast(args(0)._1, args(0)._2, REFERENCE(definitions.ObjectClass))), pass, blockExSelLabel(bb,-2)))
                val fun = style match {
                  case Dynamic if method.isEffectivelyFinal => new CFunctionAddress(externFun(method))
                  case Dynamic => {
                    val vtbl = nextvar(rtVtable)
                    /* TODO - don't cast to trait if method present in receiver vtable */
                    if (method.owner.isTrait) {
                      args(0) = (cast(args(0)._1, args(0)._2, toTypeKind(method.owner.tpe)), toTypeKind(method.owner.tpe))
                    }
                    val mnum = virtualMethods(method.owner).indexOf(method)
                    if (args.head._2.toType.typeSymbol.isTrait) {
                      val ifaceinfo = args.head._1.asInstanceOf[LMValue[rtIfaceRef.type]]
                      val receiverobj = nextvar(rtObject.pointer)
                      args(0) = (receiverobj, REFERENCE(definitions.ObjectClass))
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
                  insns.append(new invoke_void(castedfun, castedargs, pass, blockExSelLabel(bb,-2), Ccc, Seq.empty, Seq.empty))
                } else {
                  val v = nextvar(funtype.returnType)
                  insns.append(new invoke(v, castedfun, castedargs, pass, blockExSelLabel(bb,-2), Ccc, Seq.empty, Seq.empty))
                  stack.push((v,toTypeKind(method.tpe.resultType)))
                }
              }
              case NEW(kind) => {
                val asobject = nextvar(rtObject.pointer)
                insns.append(new call(asobject, rtNew, Seq(externClassP(kind.toType.typeSymbol))))
                val casted = nextvar(typeKindType(kind))
                recordType(casted.tpe)
                insns.append(new bitcast(casted, asobject))
                stack.push((casted,kind))
              }
              case CREATE_ARRAY(elem, ndims) => {
                val et = if (elem.isValueType) new CNull(rtClass.pointer) else externClassP(elem.toType.typeSymbol)
                val dims = stack.take(ndims).toList.reverse.map(_._1)
                val array = nextvar(symType(definitions.ArrayClass))
                popn(i.consumed)
                insns.append(new call(array, rtNewArray, Seq(LMConstant.byteconst(rtArrayKind(elem)), et, LMConstant.intconst(ndims)) ++ dims))
                stack.push((array, ArrayN(elem, ndims)))
              }
              case IS_INSTANCE(tpe) => {
                val (ref,sym) = stack.pop()
                val v = nextvar(LMInt.i1)
                val tpes = tpe.toType.typeSymbol
                val cls = externClassP(tpes)
                val fun = if (tpes.isTrait) rtIsinstanceIface else rtIsinstanceClass
                insns.append(new call(v, fun, Seq(cast(ref, sym, REFERENCE(definitions.ObjectClass)), cls)))
                stack.push((v, BOOL))
              }
              case CHECK_CAST(tpe) => {
                val (ref,sym) = stack.pop()
                stack.push((cast(ref, sym, tpe), tpe))
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
                    case _:LMPointer => insns.append(new icmp_p(result, intop, cast(arg, argsym, REFERENCE(definitions.ObjectClass)).asInstanceOf[LMValue[LMPointer]], cast(cmpto, cmptosym, REFERENCE(definitions.ObjectClass)).asInstanceOf[LMValue[LMPointer]]))
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
                  insns.append(new ret(cast(v,s,kind)))
                }
              }
              case THROW(_) => {
                val (exception,esym) = stack.pop
                val uwx = nextvar(LMInt.i8.pointer)
                val junk = nextvar(LMInt.i32)
                insns.append(new call(uwx, createOurException, Seq(cast(exception, esym, REFERENCE(definitions.ObjectClass)))))
                insns.append(new invoke(junk, unwindRaiseException, Seq(uwx), unreachableBlock, blockExSelLabel(bb,-2)))
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
              case LOAD_EXCEPTION(klass) => {
                val exception = nextvar(rtObject.pointer)
                insns.append(new load(exception, currentException))
                stack.push((cast(exception,REFERENCE(definitions.ObjectClass),REFERENCE(klass)), REFERENCE(klass)))
              }
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
                stack.push((boxed, REFERENCE(definitions.boxedClass(k.toType.typeSymbol))))
              }
              case UNBOX(k) => {
                val boxed = stack.pop
                val (fun) = k match {
                  case BOOL => rtUnboxi1
                  case BYTE => rtUnboxi8
                  case SHORT => rtUnboxi16
                  case INT => rtUnboxi32
                  case LONG => rtUnboxi64
                  case FLOAT => rtUnboxFloat
                  case DOUBLE => rtUnboxDouble
                  case CHAR => rtUnboxChar
                  case x => error("Don't know how to unbox "+x); rtUnboxi1
                }
                val unboxed = nextvar(fun.resultType)
                insns.append(new call(unboxed, fun, Seq(cast(boxed._1,boxed._2,REFERENCE(definitions.boxedClass(k.toType.typeSymbol))))))
                stack.push((unboxed, k))
              }
            }
            insns.append(new icomment("stack after: "+stack.map{case (v,s) => v.rep + " " + s}.mkString(", ")))
          }

          // minus 2 to account for comment after
          val terminator = insns.remove(insns.length-2)

          val tailinsns = new mutable.ListBuffer[LMInstruction]
          producedTypes.zip(stack.reverse).zipWithIndex.foreach { case ((sym,(src,ssym)),n) =>
            val tpe = symType(sym)
            if (definitions.isValueClass(sym) || sym.isTrait) {
              tailinsns.append(new select(new LocalVariable(blockName(bb)+".out."+n.toString, tpe), CTrue, src, new CUndef(tpe)))
            } else {
              val asobj = new LocalVariable(blockName(bb)+".out."+n.toString, rtObject.pointer)
              tailinsns.append(new bitcast(asobj, src))
            }
          }
          insns.append(terminator)
          blocks.append(LMBlock(Some(blockLabel(bb)), headinsns ++ Seq(new br(blockLabel(bb,0)))))
          var blocknum = 0
          val curblockinsns = new mutable.ListBuffer[LMInstruction]
          insns.foreach {
            case inv: invoke[_] =>
              if (inv.normal eq pass) {
                curblockinsns.append(new invoke(
                  inv.v, inv.funptr, inv.args, blockLabel(bb,blocknum+1),
                  inv.exception, inv.cconv, inv.ret_attrs, inv.fn_attrs))
              } else {
                curblockinsns.append(inv)
              }
              blocks.append(LMBlock(Some(blockLabel(bb,blocknum)), curblockinsns.toList))
              curblockinsns.clear()
              blocknum += 1
            case inv: invoke_void =>
              if (inv.normal eq pass) {
                curblockinsns.append(new invoke_void(
                  inv.funptr, inv.args, blockLabel(bb,blocknum+1),
                  inv.exception, inv.cconv, inv.ret_attrs, inv.fn_attrs))
              } else {
                curblockinsns.append(inv)
              }
              blocks.append(LMBlock(Some(blockLabel(bb,blocknum)), curblockinsns.toList))
              curblockinsns.clear()
              blocknum += 1
            case i if i eq terminator =>
              curblockinsns.append(new br(blockLabel(bb,-1)))
              blocks.append(LMBlock(Some(blockLabel(bb,blocknum)), curblockinsns.toList))
              curblockinsns.clear()
              blocks.append(LMBlock(Some(blockLabel(bb,-1)), tailinsns ++ Seq(i)))
            case i =>
              curblockinsns.append(i)
          }
          blocks ++= exSels(bb)
        }

        val allocaLocals = m.locals.map(l => new alloca(localCell(l), typeKindType(l.kind)))
        val storeArgs = m.locals.flatMap(l => args.find(_._2==l.sym).map(a => new store(a._1.lmvar, localCell(l))))
        blocks.insert(0,LMBlock(Some(Label("entry")), allocaLocals ++ storeArgs ++ Seq(new alloca(currentException, rtObject.pointer), new br(blockLabel(m.code.startBlock)))))
        fun.define(blocks)
      }

      val name = llvmName(c.symbol)
      val outfile = getFile(c.symbol, ".ll")
      val outstream = new OutputStreamWriter(outfile.bufferedOutput,"US-ASCII")
      val header_comment = new Comment("Module for " + c.symbol.fullName('.'))
      val concreteMethods = c.methods.filter(!_.symbol.isDeferred)
      val (llvmmethods, methods) = concreteMethods.partition(_.symbol.hasAnnotation(LlvmimplAnnotSym))
      val methodFuns = methods.filter(_.code != null).map(genFun)
      val llvmmethodFuns = llvmmethods.map(genNativeFun)
      c.methods.filter(_.native).map(_.symbol).foreach(externFun)
      val externDecls = externFuns.filterKeys(!internalFuns.contains(_)).values.map(_.declare)++externClasses.values.map(_.declare)
      val otherModules = externModules.filterKeys(_.moduleClass!=c.symbol)
      val module = new Module(Seq.concat(
        Seq(header_comment),
        externTypes.values.map(at => new TypeAlias(at)),
        rtHeader,
        externDecls,
        otherModules.values.map(_.declare),
        otherModules.keys.map(mod => moduleInitFun(mod).declare),
        classInfo,
        methodFuns,
        llvmmethodFuns,
        extraDefs,
        moduleInfo
      ))
      outstream.write(module.syntax)
      outstream.write("\n")
      outstream.close()
    }


    def classType(s: Symbol): ConcreteType with AliasedType = {
      externalType(s)
      /*
      classes.get(s.tpe.typeSymbol) match {
        case Some(c) => classType(c)
        case None => externalType(s)
      }
      */
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

    def arrayType(s: Symbol): ConcreteType = {
      new LMStructure(Seq(rtObject, LMInt.i32, new LMArray(0, symType(s)))).pointer
    }

    def typeType(t: Type): ConcreteType = {
        t.typeSymbol match {
          case definitions.BooleanClass => LMInt.i1
          case definitions.ByteClass => LMInt.i8
          case definitions.ShortClass => LMInt.i16
          case definitions.IntClass => LMInt.i32
          case definitions.LongClass => LMInt.i64
          case definitions.FloatClass => LMFloat
          case definitions.DoubleClass => LMDouble
          case definitions.CharClass => LMInt.i32
          case definitions.UnitClass => LMVoid
          case x if x.isTrait => rtIfaceRef
          case x => classType(x).pointer
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
      dir.fileNamed(llvmName(sym) + suffix)
    }

    /* used escapes: _ D L G S O M R A N */

    def encodeName(s: String) = {
      s.replace("_","__")
       .replace(".","_D")
       .replace("<","_L")
       .replace(">","_G")
       .replace("$","_S")
    }

    def methodSig(sym: Symbol): String = {
      def typeSig(t: Type): String = typeKindSig(toTypeKind(t))
      def typeKindSig(tk: TypeKind): String = tk match {
        case ARRAY(ek) => "_N" + typeKindSig(ek)
        case _ => llvmName(tk.toType.typeSymbol)
      }
      "_M"+encodeName(sym.simpleName.toString.trim)+sym.info.paramss.flatten.map(ps => "_A"+typeSig(ps.info)).mkString("")+"_R"+typeSig(sym.info.resultType)
    }

    def llvmName(sym: Symbol): String = {
      if (sym.isClass && !sym.isModule && !sym.isModuleClass)
        encodeName(sym.fullName('.'))
      else if (sym.isModuleClass || (sym.isModule && !sym.isMethod))
	"_O"+encodeName(sym.fullName('.'))
      else if (sym.isMethod)
        llvmName(sym.owner)+methodSig(sym)
      else
	encodeName(sym.simpleName.toString.trim)
    }

    def blockLabel(bb: BasicBlock) = Label(blockName(bb))
    def blockName(bb: BasicBlock) = "bb."+bb.label
    def blockLabel(bb: BasicBlock, x: Int) = Label(blockName(bb,x))
    def blockName(bb: BasicBlock, x: Int) = "bb."+bb.label+"."+x.toString
    def blockExSelName(bb: BasicBlock, x: Int) = "bb."+bb.label+".exh."+x.toString
    def blockExSelLabel(bb: BasicBlock, x: Int) = Label(blockExSelName(bb, x))

    def privateClassInfoName(s: Symbol) = {
      "priv_classinfo_"+llvmName(s)
    }

    def classInfoName(s: Symbol) = {
      "class_"+llvmName(s)
    }

    def moduleInstanceName(s: Symbol) = {
      "module_"+llvmName(s)
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
          case CALL_METHOD(m,SuperCall(_)) => {
            m.owner.tpe.typeSymbol +: instruction.consumedTypes.drop(1).map(_.toType.typeSymbol)
          }
          case DROP(k) => Seq(k.toType.typeSymbol)
          case DUP(k) => Seq(k.toType.typeSymbol)
          case CZJUMP(_, _, _, k) => Seq(k.toType.typeSymbol)
          case THROW(_) => Seq(definitions.ThrowableClass)
          case CALL_PRIMITIVE(StringConcat(kind)) => Seq(definitions.ObjectClass, kind.toType.typeSymbol)
          case CALL_PRIMITIVE(EndConcat) => Seq(definitions.ObjectClass)
          case SWITCH(_,_) => Seq(definitions.IntClass)
          case LOAD_EXCEPTION(_) => Seq.empty
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
          case CALL_PRIMITIVE(StartConcat) => Seq(definitions.ObjectClass)
          case CALL_PRIMITIVE(StringConcat(_)) => Seq(definitions.ObjectClass)
          case CREATE_ARRAY(elem, dims) => Seq(definitions.ArrayClass)
          case i => i.producedTypes.map(_.toType.typeSymbol)
        }
        instruction match {
          case LOAD_EXCEPTION(_) => ()
          case _ =>
            if (instruction.consumed != consumedTypes.length) {
              error("Consumption mismatch for " + instruction+": "+instruction.consumed.toString +" "+ consumedTypes)
          }
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
