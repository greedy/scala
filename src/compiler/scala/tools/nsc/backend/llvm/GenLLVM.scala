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
    val ForeignAnnotSym = definitions.getClass("scala.ffi.foreign")
    val ForeignValueAnnotSym = definitions.getClass("scala.ffi.foreignValue")
    val ForeignExportAnnotSym = definitions.getClass("scala.ffi.foreignExport")
    val CodegenAnnotations = Set(LlvmimplAnnotSym, ForeignAnnotSym, ForeignValueAnnotSym)
    val PtrSym = definitions.getClass("scala.ffi.Ptr")
    val PtrObjSym = PtrSym.companionModule
    val Ptr_wrap = PtrObjSym.info.member("wrap")
    val Ptr_unwrap = PtrObjSym.info.member("unwrap")
    val MarshalledTypes = Set(BOOL,BYTE,SHORT,CHAR,INT,LONG,FLOAT,DOUBLE,REFERENCE(PtrSym))
    val MarshalledResultTypes = MarshalledTypes ++ Set(UNIT)

    def isMarshallable(t: Type) = {
      toTypeKind(t) match {
        case BOOL|BYTE|SHORT|CHAR|INT|LONG|FLOAT|DOUBLE => true
        /*
        case Reference(_) => t match {
          case TypeRef(_, PtrSym, List(pt)) => isMarshallable(pt)
          case _ => false
        }
        */
        case _ => false
      }
    }

    def isMarshallableResult(t: Type) = isMarshallable(t) || toTypeKind(t) == UNIT

    def marshalledType(t: Type): LMType with ConcreteType = {
      t match {
        case TypeRef(_, PtrSym, List(pt)) => marshalledType(pt).pointer
        case _ if isMarshallable(t) => typeType(t)
        case _ => error("Type "+ t +" is not marshallable"); LMOpaque
      }
    }

    def instFields(c: IClass): List[IField] = {
      c.fields.filterNot(_.symbol.isStaticMember)
    }

    def instFields(s: Symbol): Option[List[IField]] = {
      classes.get(s).map(instFields _)
    }

    def staticFields(c: IClass): List[IField] = {
      c.fields.filter(_.symbol.isStaticMember)
    }

    def staticFields(s: Symbol): Option[List[IField]] = {
      classes.get(s).map(staticFields _)
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
        ".rt.boxedUnit", symType(definitions.BoxedUnitClass).asInstanceOf[LMPointer].target,
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
      val externStatics: mutable.Map[Symbol,LMGlobalVariable[_<:ConcreteType]] = new mutable.HashMap
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

      def functionForMethod(m: IMethod): (LMFunction, Seq[(ArgSpec,Symbol)]) = {
        val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
        val recvarg = if (m.symbol.isStaticMember) { Seq.empty } else { Seq((thisarg, c.symbol)) }
        val args = recvarg ++ m.params.map(p => (ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))), p.sym))
        val fun = new LMFunction(typeType(m.returnType.toType), "method_"+llvmName(m.symbol), args.map(_._1), false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
        (fun, args)
      }

      def functionForMethodSymbol(s: Symbol): LMFunction = {
        require(s.isMethod)
        val funtype = symType(s).asInstanceOf[LMFunctionType]
        val args = funtype.argTypes.zipWithIndex.map{case (t,i) => ArgSpec(new LocalVariable("arg"+i, t), Seq.empty)}
        new LMFunction(funtype.returnType, "method_"+llvmName(s), args, false, Externally_visible, Default, Ccc, Seq.empty, Seq.empty, None, None, None)
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
          case ClassTag => new CNull(rtObject.aliased("java_Dlang_DClass").pointer)
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

      def externModule(s: Symbol): LMGlobalVariable[_<:ConcreteType] = {
        if (s.isModuleClass)
          externModule(s.companionModule)
        else {
          recordType(symType(s))
          if (c.symbol == s) {
              new LMGlobalVariable(moduleInstanceName(s), symType(s).asInstanceOf[LMPointer].target, Externally_visible, Default, false)
          } else {
            externModules.getOrElseUpdate(s, {
              new LMGlobalVariable(moduleInstanceName(s), symType(s).asInstanceOf[LMPointer].target, Externally_visible, Default, true)
            })
          }
        }
      }

      def externFun(s: Symbol) = {
        externFuns.getOrElseUpdate(s, {
          val fun = functionForMethodSymbol(s)
          fun.tpe.argTypes.foreach(recordType)
          recordType(fun.tpe.returnType)
          fun
        })
      }

      def externStaticP(s: Symbol) = new CGlobalAddress(externStatic(s))

      def externStatic(s: Symbol) = {
        if (c.symbol == s) {
          new LMGlobalVariable(staticsName(s), staticsType(s), Externally_visible, Default, false)
        } else {
          recordType(staticsType(s))
          externStatics.getOrElseUpdate(s, {
            new LMGlobalVariable(staticsName(s), staticsType(s), Externally_visible, Default, false)
          })
        }
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
          val myvirts = s.info.decls.toList.filter(d => d.isMethod && !d.isConstructor && !(d.isOverride && s.superClass.info.members.exists(mem => mem.info <:< d.info && mem.name == d.name)) && (d.isDeferred || !d.isEffectivelyFinal))
          (virtualMethods(s.superClass) ++ myvirts.sortBy(methodSig _)).toList
        }
      }

      val classInfo: Seq[ModuleComp] = {
        def implementationOf(m: Symbol) = {
          val typeOrder = Ordering.fromLessThan[Symbol](_.info <:< _.info)
          val alts = c.symbol.info.members.filter(mem => mem.info <:< m.info && mem.name == m.name)
          if (alts.isEmpty) throw new Exception("No implementation of " + m + ": " + m.info + " in " + c) else alts.min(typeOrder)
        }
        val ct = classType(c)
        val supers = Stream.iterate(c.symbol.superClass)(_.superClass).takeWhile(s => s != NoSymbol)
        val traits = c.symbol.info.baseClasses.filter(_.isTrait)
        supers.map(classType).foreach(recordType)
        recordType(ct)
        val basevirts = virtualMethods(c.symbol)
        val virts = basevirts.map(implementationOf _)
        val vfuns = virts.map(v => if (v.isIncompleteIn(c.symbol)) new CNull(LMInt.i8.pointer) else new CFunctionAddress(externFun(v)))
        val vtable = new CArray(LMInt.i8.pointer, vfuns.map(f => new Cbitcast(f, LMInt.i8.pointer)))
        val vtableg = new LMGlobalVariable(".vtable", vtable.tpe, Private, Default, true)
        val traitinfo = traits.map { t =>
          val tvirts = virtualMethods(t)
          val tvimpls = tvirts.map(implementationOf _)
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
        val statType = staticsType(c)
        val statics = new LMGlobalVariable[LMStructure](staticsName(c.symbol), statType, Externally_visible, Default, false)
        recordType(statType)
        Seq.concat(
          Seq(
            new TypeAlias(cig.tpe.aliased("thisclass")),
            cig.define(ci),
            vtableg.define(vtable),
            statics.define(new CZeroInit(statType))
          ),
          traitinfo.map(ti => ti._1.define(ti._2))
        )
      }

      val moduleInfo: Seq[ModuleComp] = {
        if (c.symbol.isModuleClass && c.symbol.isStatic && !c.symbol.isImplClass) {
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
              retvoid)),
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

      type SomeConcreteType = LMType with ConcreteType

      def marshallToNative(local: LocalVariable[_ <: SomeConcreteType], tpe: Type): (LocalVariable[_ <: SomeConcreteType],Seq[LMInstruction]) = {
        toTypeKind(tpe) match {
          case BOOL|BYTE|SHORT|CHAR|INT|LONG|FLOAT|DOUBLE => (local, Seq.empty)
          /*
          case Reference(_) => tpe match {
            case TypeRef(_, PtrSym, List(pt)) => {
              val resultVar = new LocalVariable(".asptr."+local.name, marshalledType(tpe))
              val addressVal = new LocalVariable(".addr."+local.name, typeType(PtrSym.info.member("address").resultType))
              (resultVar, Seq(
                new call(addressVal, externFun(PtrSym.companionModule.info.member("unwrap")), Seq(new CGlobalAddress(externModule(PtrSym.companionModule)), local))
                new inttoptr(resultVar, addressVal)
              ))
            }
          }
          */
        }
      }

      def marshallFromNative(local:LocalVariable[_ <: LMType with ConcreteType], tpe:Type): (LocalVariable[_ <: LMType with ConcreteType],Seq[LMInstruction]) = {
        toTypeKind(tpe) match {
          case BOOL|BYTE|SHORT|CHAR|INT|LONG|FLOAT|DOUBLE => (local, Seq.empty)
          /*
          case Reference(_) => tpe match {
            case TypeRef(_, PtrSym, List(pt)) => {
              val ptrasint = new LocalVariable(".ptrasint."+local.name, typeType(PtrSym.info.member("address").resultType))
              val
            }
            */
        }
      }

      def exportFunction(m: IMethod): Seq[ModuleComp] = {
        m.symbol.getAnnotation(ForeignExportAnnotSym) match {
          case Some(AnnotationInfo(_,List(Literal(Constant(foreignSymbol: String))),_)) => {
            val methType = m.symbol.info
            if (!m.symbol.isStatic) {
              error("Only object methods can be exported"); Seq.empty
            } else if (methType.paramSectionCount != 1) {
              error("Exported functions must take exactly one parameter list"); Seq.empty
            } else if (!isMarshallableResult(methType.resultType)) {
              error("Exported functions must have marshallable return type"); Seq.empty
            } else if (methType.paramTypes.exists(pt => !isMarshallable(pt))) {
              error("All parameters to exported functions must be marshallable"); Seq.empty
            } else {
              val args = m.params.map(p => new LocalVariable(llvmName(p.sym), marshalledType(p.sym.info)))
              val argSpec = args.map(lv => ArgSpec(lv))
              val exportedFun = new LMFunction(
                typeKindType(m.returnType), foreignSymbol, argSpec, false,
                Externally_visible, Default, Ccc,
                Seq.empty, Seq.empty, None, None, None)
              val methodFun = functionForMethod(m)._1
              val modGlobal = new CGlobalAddress(externModule(m.symbol.enclClass))
              val body = if (m.returnType == UNIT) {
                Seq(
                  new call_void(moduleInitFun(m.symbol.owner), Seq.empty),
                  new call_void(methodFun, Seq(modGlobal) ++ args),
                  retvoid
                )
              } else {
                val result = new LocalVariable(".result", typeKindType(m.returnType))
                Seq(
                  new call_void(moduleInitFun(m.symbol.owner), Seq.empty),
                  new call(result, methodFun, Seq(modGlobal) ++ args),
                  new ret(result)
                )
              }
              Seq(exportedFun.define(Seq(LMBlock(None, body))))
            }
          }
        }
      }

      def genForeignFun(m: IMethod): Seq[ModuleComp] = {
        m.symbol.getAnnotation(ForeignAnnotSym) match {
          case Some(AnnotationInfo(_,List(Literal(Constant(foreignSymbol: String))),_)) => {
            val methType = m.symbol.info
            if (methType.paramSectionCount != 1) {
              error("Foreign functions must take exactly one parameter list"); Seq.empty
            } else if (!isMarshallableResult(methType.resultType)) {
              error("Foreign function must return marshallable type"); Seq.empty
            } else if (methType.paramTypes.exists(pt => !isMarshallable(pt))) {
              error("Foreign function must take marshallable parameters"); Seq.empty
            } else {
              val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
              val recvarg = if (m.symbol.isStaticMember) { Seq.empty } else { Seq(thisarg) }
              val args = m.params.map(p => new LocalVariable(llvmName(p.sym), marshalledType(p.sym.info)))
              val argSpec = args.map(lv => ArgSpec(lv))
              val marshalled = args.zip(methType.paramTypes).map((marshallToNative _).tupled)
              val targetFunction = new LMFunction(
                typeKindType(m.returnType), foreignSymbol, argSpec, false,
                Externally_visible, Default, Ccc,
                Seq.empty, Seq.empty, None, None, None)
              val method = functionForMethod(m)._1
              // Must marshall return value to scala
              val body = if (m.returnType == UNIT) {
                marshalled.map(_._2).flatten ++ Seq(new call_void(targetFunction, marshalled.map(_._1)), retvoid)
              } else {
                val res = new LocalVariable("res", typeKindType(m.returnType))
                marshalled.map(_._2).flatten ++ Seq(new call(res, targetFunction, args), new ret(res))
              }
              Seq(
                targetFunction.declare,
                method.define(Seq(LMBlock(None, body)))
              )
            }
          }
          case Some(_) => error("Invalid foreign annotation"); Seq.empty
          case None => error("Actually no foreign annotation; impossible"); Seq.empty
        }
      }

      def genForeignVal(m: IMethod): Seq[ModuleComp] = {
        m.symbol.getAnnotation(ForeignValueAnnotSym) match {
          case Some(AnnotationInfo(_,List(Literal(Constant(foreignSymbol: String))),_)) => {
            if (!m.returnType.isValueType || m.returnType == UNIT) {
              error("Foreign value must be non-unit primitive"); Seq.empty
            } else if (!m.params.isEmpty) {
              error("Foreign value must not take parameters"); Seq.empty
            } else {
              val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
              val args = if (m.symbol.isStaticMember) { Seq.empty } else { Seq(thisarg) }
              val targetGlobal = new LMGlobalVariable(foreignSymbol, typeKindType(m.returnType), Externally_visible, Default, false)
              val method = functionForMethod(m)._1
              val res = new LocalVariable("res", typeKindType(m.returnType))
              val body = Seq(new load(res, new CGlobalAddress(targetGlobal)), new ret(res))
              Seq(
                targetGlobal.declare,
                method.define(Seq(LMBlock(None, body)))
              )
            }
          }
          case Some(_) => error("Invalid foreign annotation"); Seq.empty
          case None => error("Actually no foreign annotation; impossible"); Seq.empty
        }
      }


      def genNativeFun(m: IMethod) = {
        val thisarg = ArgSpec(new LocalVariable(".this", symType(c.symbol)))
        val args = thisarg +: m.params.map(p => ArgSpec(new LocalVariable(llvmName(p.sym), localType(p))))
        val fun = functionForMethod(m)._1
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
        val thisCell = new LocalVariable("__this", rtObject.pointer.pointer)
        val (fun,args) = functionForMethod(m)
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
          implicit val insns: mutable.ListBuffer[LMInstruction] = new mutable.ListBuffer
          val pass = Label("__PASS__")
          val excpreds = bb.code.blocks.filter(pb => pb.exceptionSuccessors.contains(bb))
          val dirpreds = bb.code.blocks.filter(_.directSuccessors contains bb)
          val preds = (excpreds ++ dirpreds).filter(reachable)
          insns.append(new icomment("predecessors: "+preds.map(_.fullString).mkString(",")+" successors: "+bb.successors.map(_.fullString).mkString(" ")))
          def cast(src: LMValue[_<:ConcreteType], srctk: TypeKind, targettk: TypeKind)(implicit _insns: mutable.ListBuffer[LMInstruction]): LMValue[_<:ConcreteType] = {
            _insns.append(new icomment("cast "+src.rep+" from "+srctk+" to "+targettk))
            if (srctk == targettk) {
              src
            } else if (srctk.isRefOrArrayType && targettk.isRefOrArrayType) {
              recordType(typeKindType(targettk))
              if (targettk.isInterfaceType) {
                if (srctk.isInterfaceType) {
                  cast(cast(src, srctk, ObjectReference)(_insns), ObjectReference, targettk)(_insns)
                } else {
                  srctk.toType.typeSymbol.info.baseClasses.filter(_.isTrait).indexOf(targettk.toType.typeSymbol) match {
                    case -1 => {
                      val iface = nextvar(rtIfaceRef)
                      val obj = nextvar(rtObject.pointer)
                      _insns.append(new bitcast(obj, src))
                      _insns.append(new call(iface, rtIfaceCast, Seq(obj, externClassP(targettk.toType.typeSymbol))))
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
                      _insns.append(new bitcast(asobj, src))
                      _insns.append(new getelementptr(clsptrptr, asobj, Seq[LMConstant[LMInt]](0, 0)))
                      _insns.append(new load(clsptr, clsptrptr))
                      _insns.append(new getelementptr(vtblptr, clsptr, Seq[LMConstant[LMInt]](0, 7, tgtidx, 1)))
                      _insns.append(new load(vtbl, vtblptr))
                      _insns.append(new insertvalue(iref0, new CUndef(rtIfaceRef), asobj, Seq[LMConstant[LMInt]](0)))
                      _insns.append(new insertvalue(iref1, iref0, vtbl, Seq[LMConstant[LMInt]](1)))
                      iref1
                    }
                  }
                }
              } else {
                if (srctk.isInterfaceType) {
                  if (targettk == ObjectReference) {
                    val obj = nextvar(rtObject.pointer)
                    _insns.append(new extractvalue(obj, src.asInstanceOf[LMValue[rtIfaceRef.type]], Seq[CInt](0)))
                    obj
                  } else {
                    cast(cast(src, srctk, ObjectReference)(_insns), ObjectReference, targettk)(_insns)
                  }
                } else {
                  val v = nextvar(typeKindType(targettk))
                  _insns.append(new bitcast(v, src))
                  v
                }
              }
            } else if (srctk.isValueType && targettk.isValueType) {
              if (srctk.isIntegralType && targettk.isIntegralType) {
                val r = nextvar(typeKindType(targettk))
                val srci = src.asInstanceOf[LMValue[LMInt]]
                val ri = r.asInstanceOf[LocalVariable[LMInt]]
                if (ri.tpe.bits > srci.tpe.bits) {
                  if (srctk == CHAR) {
                    _insns.append(new zext(ri,srci))
                  } else {
                    _insns.append(new sext(ri,srci))
                  }
                  ri
                } else if (ri.tpe.bits < srci.tpe.bits) {
                  _insns.append(new trunc(ri, srci))
                  ri
                } else {
                  /* no-op */
                  srci
                }
              } else if (srctk.isRealType && targettk.isRealType) {
                var res = nextvar(typeKindType(targettk)).asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]]
                (srctk,targettk) match {
                  case (DOUBLE,FLOAT) =>
                    _insns.append(new fptrunc(res, src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                    res
                  case (FLOAT,DOUBLE) =>
                    _insns.append(new fpext(res, src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                    res
                  case (FLOAT,FLOAT) | (DOUBLE,DOUBLE) =>
                    src
                }
              } else if (srctk.isRealType && targettk.isIntegralType) {
                val res = nextvar(typeKindType(targettk))
                if (targettk == CHAR) {
                  _insns.append(new fptoui(res.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                } else {
                  _insns.append(new fptosi(res.asInstanceOf[LocalVariable[LMInt]], src.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                }
                res
              } else if (srctk.isIntegralType && targettk.isRealType) {
                val res = nextvar(typeKindType(targettk))
                if (srctk == CHAR) {
                  _insns.append(new uitofp(res.asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]], src.asInstanceOf[LMValue[LMInt]]))
                } else {
                  _insns.append(new sitofp(res.asInstanceOf[LocalVariable[LMFloatingPointType with ConcreteType]], src.asInstanceOf[LMValue[LMInt]]))
                }
                res
              } else {
                sys.error("Source (%s) and target (%s) not compatible".format(srctk,targettk))
              }
            } else {
              sys.error("Source type kind (%s) and target type kind (%s) are not castable".format(srctk,targettk))
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
          //println("Entering " + blockName(bb))
          //println("My consumed types are:\n"+consumedTypes.zipWithIndex.map(x => x._2 + " => " + x._1).mkString("\n"))
          /*
          dirpreds.filter(reachable).foreach { dp =>
            println(blockName(dp) + " produced types are:\n"+stackUsage(dp)._2.zipWithIndex.map(x => x._2 + " => " + x._1).mkString("\n"))
          }
          excpreds.filter(reachable).foreach { dp =>
            println(blockName(dp) + " produced types are:\n"+stackUsage(dp)._2.zipWithIndex.map(x => x._2 + " => " + x._1).mkString("\n"))
          }
          */
          if (!bb.exceptionHandlerStart) {
            consumedTypes.zipWithIndex.foreach { case (tpe,n) =>
              //println("Handling incoming stack slot " + n + " with type " + tpe)
              val lt = typeKindType(tpe)
              recordType(lt)
              if (tpe.isValueType || tpe == ConcatClass) {
                val reg = new LocalVariable(blockName(bb)+".in."+n.toString,lt)
                val dirsources = dirpreds.map(pred => (blockLabel(pred,-1), new LocalVariable(blockName(pred)+".out."+n.toString,lt)))
                val excsources = excpreds.filter(reachable).map(pred => (blockExSelLabel(pred,pred.method.exh.filter(_.covers(pred)).findIndexOf(_.startBlock == bb)), new LocalVariable(blockName(pred)+".out."+n.toString,lt)))
                val sources = dirsources ++ excsources
                insns.append(new phi(reg, sources))
                stack.push((reg, tpe))
              } else {
                val asobj = nextvar(rtObject.pointer)
                val dirsources = dirpreds.filter(reachable).map(pred => (blockLabel(pred,-1), new LocalVariable(blockName(pred)+".out."+n.toString,rtObject.pointer)))
                val excsources = excpreds.filter(reachable).map(pred => (blockExSelLabel(pred,pred.method.exh.filter(_.covers(pred)).findIndexOf(_.startBlock == bb)), new LocalVariable(blockName(pred)+".out."+n.toString,rtObject.pointer)))
                val sources = dirsources ++ excsources
                insns.append(new phi(asobj, sources))
                stack.push((cast(asobj, ObjectReference, tpe)(predcasts), tpe))
              }
            }
          }


          insns.appendAll(predcasts)
          val headinsns = if (bb.exceptionHandlerStart) Seq.empty else insns.toList
          insns.clear()
          bb.foreach { i => try {
            insns.append(new icomment(i.toString))
            insns.append(new icomment("stack before: "+stack.map{case (v,s) => v.rep + " " + s}.mkString(", ")))
            i match {
              case THIS(clasz) => {
                val v = nextvar(rtObject.pointer)
                val thisTk = if (clasz == definitions.ArrayClass) ARRAY(NothingReference) else REFERENCE(clasz)
                insns.append(new load(v, thisCell))
                stack.push((cast(v, ObjectReference, thisTk), thisTk))
              }
              case STORE_THIS(_) => {
                val (t,tk) = stack.pop
                if (m.symbol.isStaticMember) {
                  insns.append(new store(cast(t,tk,m.params.head.kind), localCell(m.params.head)))
                } else {
                  insns.append(new store(cast(t,tk,ObjectReference), thisCell))
                }
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
                stack.push((new CGlobalAddress(rtBoxedUnit), BoxedUnitReference))
              }
              case i@LOAD_FIELD(field, false) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                instFields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    assume(fieldidx >= 0)
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
                    stack.push((new CUndef(v.tpe), toTypeKind(field.tpe)))
                }
              }
              case LOAD_FIELD(field, true) => {
                val v = nextvar(symType(field))
                val fieldptr = nextvar(v.tpe.pointer)
                staticFields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    assume(fieldidx >= 0)
                    insns.append(new getelementptr(fieldptr, externStaticP(field.owner), Seq(new CInt(LMInt.i8, 0),new CInt(LMInt.i32,fieldidx))))
                    insns.append(new load(v, fieldptr))
                    stack.push((v,toTypeKind(field.tpe)))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                    m.dump
                    stack.push((new CUndef(v.tpe), ObjectReference))
                }
              }
              case LOAD_MODULE(module) => {
                insns.append(new call_void(moduleInitFun(module), Seq.empty))
                stack.push((new CGlobalAddress(externModule(module)), REFERENCE(module)))
              }
              case STORE_ARRAY_ITEM(kind) => {
                val arraylmtype = arrayType(kind.toType.typeSymbol)
                val (item, itemtk) = stack.pop
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
                insns.append(new store(cast(item, itemtk, kind), itemptr))
              }
              case STORE_LOCAL(local) => {
                val (v,s) = stack.pop
                insns.append(new store(cast(v,s,local.kind), localCell(local)))
              }
              case STORE_FIELD(field, false) => {
                val fieldptr = nextvar(symType(field).pointer)
                instFields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    assume(fieldidx >= 0)
                    val (value,valuesym) = stack.pop
                    val (iv,ik) = stack.pop
                    val instance = cast(iv,ik,toTypeKind(field.owner.tpe))
                    val asobj = nextvar(rtObject.pointer)
                    insns.append(new bitcast(asobj, instance))
                    insns.append(new invoke_void(rtAssertNotNull, Seq(asobj), pass, blockExSelLabel(bb,-2)))
                    insns.append(new getelementptr(fieldptr, instance.asInstanceOf[LMValue[LMPointer]], Seq(new CInt(LMInt.i8,0),new CInt(LMInt.i32,fieldidx+1))))
                    insns.append(new store(cast(value, valuesym, toTypeKind(field.tpe)), fieldptr))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                }
              }
              case STORE_FIELD(field, true) => {
                val fieldptr = nextvar(symType(field).pointer)
                staticFields(field.owner) match {
                  case Some(fi) =>
                    val fieldidx = fi.indexWhere(f => f.symbol == field)
                    assume(fieldidx >= 0)
                    val (value,valuesym) = stack.pop
                    insns.append(new getelementptr(fieldptr, externStaticP(field.owner), Seq(new CInt(LMInt.i8, 0),new CInt(LMInt.i32,fieldidx))))
                    insns.append(new store(cast(value, valuesym, toTypeKind(field.tpe)), fieldptr))
                  case None =>
                    error("No field info for "+field.owner+" needed to lookup position of "+field)
                }
              }
              case CALL_PRIMITIVE(primitive) => {
                primitive match {
                  case Negation(k) => {
                    val (arg,argtk) = stack.pop
                    val v = nextvar(typeKindType(k))
                    arg.tpe match {
                      case i:LMInt => insns.append(
                        new mul(v.asInstanceOf[LocalVariable[LMInt]],
                                cast(arg,argtk,k).asInstanceOf[LMValue[LMInt]],
                                new CInt(i,-1)))
                      case f:LMFloat => insns.append(
                        new fmul(v.asInstanceOf[LocalVariable[LMFloat]],
                                 cast(arg,argtk,k).asInstanceOf[LMValue[LMFloat]],
                                 new CFloat(-1)))
                      case d:LMDouble => insns.append(
                        new fmul(v.asInstanceOf[LocalVariable[LMDouble]],
                                 cast(arg,argtk,k).asInstanceOf[LMValue[LMDouble]],
                                 new CDouble(-1)))
                    }
                    stack.push((v,argtk))
                  }
                  case Test(op, k, z) => {
                    val (cmpto,cmptok) = if (z) (new CZeroInit(typeKindType(k)),k) else stack.pop
                    val (arg,argtk) = stack.pop
                    val result = nextvar(LMInt.i1)
                    def cmp(intop: ICond, floatop: FCond) = {
                      arg.tpe match {
                        case _:LMInt => insns.append(
                          new icmp(result, intop, cast(arg,argtk,k).asInstanceOf[LMValue[LMInt]], cast(cmpto,cmptok,k).asInstanceOf[LMValue[LMInt]]))
                        case _:LMFloat => insns.append(
                          new fcmp(result, floatop, cast(arg,argtk,k).asInstanceOf[LMValue[LMFloat]], cast(cmpto,cmptok,k).asInstanceOf[LMValue[LMFloat]]))
                        case _:LMDouble => insns.append(new fcmp(result, floatop, cast(arg,argtk,k).asInstanceOf[LMValue[LMDouble]], cast(cmpto,cmptok,k).asInstanceOf[LMValue[LMDouble]]))
                        case _:LMPointer => insns.append(new icmp_p(result, intop, cast(arg,argtk,k).asInstanceOf[LMValue[LMPointer]], cast(cmpto,cmptok,k).asInstanceOf[LMValue[LMPointer]]))
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
                  case Comparison(op, k) => {
                    val (v2,v2k) = stack.pop
                    val (v1,v1k) = stack.pop
                    val v1x = cast(v1,v1k,k)
                    val v2x = cast(v2,v2k,k)
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
                        insns.append(new fcmp(equal, FCond.oeq, v1x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        insns.append(new fcmp(v1greater, FCond.ogt, v1x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        insns.append(new fcmp(unordered, FCond.uno, v1x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]], v2x.asInstanceOf[LMValue[LMFloatingPointType with ConcreteType]]))
                        val temp1 = nextvar(LMInt.i32)
                        val temp2 = nextvar(LMInt.i32)
                        insns.append(new select(temp1, v1greater, new CInt(LMInt.i32, 1), new CInt(LMInt.i32, -1)))
                        insns.append(new select(temp2, equal, new CInt(LMInt.i32, 0), temp1))
                        insns.append(new select(result, unordered, unorderedval, temp2))
                      }
                      case it:LMInt => {
                        val v1greater = nextvar(LMInt.i1)
                        val equal = nextvar(LMInt.i1)
                        insns.append(new icmp(equal, ICond.ieq, v1x.asInstanceOf[LMValue[LMInt]], v2x.asInstanceOf[LMValue[LMInt]]))
                        insns.append(new icmp(v1greater, ICond.sgt, v1x.asInstanceOf[LMValue[LMInt]], v2x.asInstanceOf[LMValue[LMInt]]))
                        val temp = nextvar(LMInt.i32)
                        insns.append(new select(temp, v1greater, new CInt(LMInt.i32, 1), new CInt(LMInt.i32, -1)))
                        insns.append(new select(result, equal, new CInt(LMInt.i32, 0), temp))
                      }
                    }
                    stack.push((result, INT))
                  }
                  case Arithmetic(NOT, k) => {
                    val (argt, s) = stack.pop
                    val kt = typeKindType(k).asInstanceOf[LMInt]
                    val arg = cast(argt,s,k).asInstanceOf[LMValue[LMInt]]
                    val res = nextvar(kt)
                    insns.append(new xor(res, arg, new CInt(kt, -1)))
                    stack.push((res,k))
                  }
                  /* FIXME - CHARs are unsigned */
                  case Arithmetic(op, k) => {
                    val (v2t,v2k) = stack.pop
                    val (v1t,v1k) = stack.pop
                    val v1 = cast(v1t,v1k,k)
                    val v2 = cast(v2t,v2k,k)
                    val result = nextvar(typeKindType(k))
                    def cv[T <: ConcreteType](x: LocalVariable[_]) = x.asInstanceOf[LocalVariable[T]]
                    def c[T <: ConcreteType](x: LMValue[_]) = x.asInstanceOf[LMValue[T]]
                    insns.append((op,k) match {
                      case (ADD,k) if k.isIntegralType => new add(cv(result), c(v1), c(v2))
                      case (ADD,k) if k.isRealType => new fadd(cv(result), c(v1), c(v2))
                      case (SUB,k) if k.isIntegralType => new sub(cv(result), c(v1), c(v2))
                      case (SUB,k) if k.isRealType => new fsub(cv(result), c(v1), c(v2))
                      case (MUL,k) if k.isIntegralType => new mul(cv(result), c(v1), c(v2))
                      case (MUL,k) if k.isRealType => new fmul(cv(result), c(v1), c(v2))
                      case (DIV,CHAR) => new udiv(cv(result), c(v1), c(v2))
                      case (DIV,k) if k.isIntegralType => new sdiv(cv(result), c(v1), c(v2))
                      case (DIV,k) if k.isRealType => new fdiv(cv(result), c(v1), c(v2))
                      case (REM,CHAR) => new urem(cv(result), c(v1), c(v2))
                      case (REM,k) if k.isIntegralType => new srem(cv(result), c(v1), c(v2))
                      case (REM,k) if k.isRealType => new frem(cv(result), c(v1), c(v2))
                    })
                    stack.push((result, k))
                  }
                  case Logical(op, k) => {
                    val (v2t,v2k) = stack.pop
                    val (v1t,v1k) = stack.pop
                    val v2 = cast(v2t,v2k,k).asInstanceOf[LMValue[LMInt]]
                    val v1 = cast(v1t,v1k,k).asInstanceOf[LMValue[LMInt]]
                    val result = nextvar(typeKindType(k)).asInstanceOf[LocalVariable[LMInt]]
                    val insn = op match {
                      case AND => new and(result, v1, v2)
                      case OR => new or(result, v1, v2)
                      case XOR => new xor(result, v1, v2)
                    }
                    insns.append(insn)
                    stack.push((result, k))
                  }
                  case Shift(op, tk) => {
                    val (shiftt,shiftk) = stack.pop
                    val (argt,argk) = stack.pop
                    val arg = cast(argt,argk,tk).asInstanceOf[LMValue[LMInt]]
                    val shiftamt = cast(shiftt,shiftk,tk).asInstanceOf[LMValue[LMInt]]
                    val result = nextvar(typeKindType(tk)).asInstanceOf[LocalVariable[LMInt]]
                    val insn = op match {
                      case LSL => new shl(result, arg, shiftamt)
                      case ASR => new ashr(result, arg, shiftamt)
                      case LSR => new lshr(result, arg, shiftamt)
                    }
                    insns.append(insn)
                    stack.push((result, tk))
                  }
                  case Conversion(_, dk) => {
                    val dt = typeKindType(dk)
                    val (src,srck) = stack.pop
                    val result = cast(src,srck,dk)
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
                    stack.push((sp, ConcatClass))
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
                    if (vs.isInterfaceType) {
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
                    stack.push((v, StringReference))
                  }
                  case _ => warning("unsupported primitive op " + primitive)
                }
              }
              case CALL_METHOD(method, style) => {
                val funtype = symType(method).asInstanceOf[LMFunctionType]
                val contextargs = style match {
                  case Static(true) | Dynamic | SuperCall(_) =>
                    if (method.owner.isTrait) Seq(ObjectReference) else Seq(toTypeKind(method.owner.tpe))
                  case Static(false) => Seq.empty
                }
                val argsyms = contextargs ++ method.tpe.paramTypes.map(toTypeKind)
                val args = stack.take(funtype.argTypes.size).reverse.toBuffer
                style match {
                  case Static(true) | Dynamic | SuperCall(_) =>
                    insns.append(new invoke_void(rtAssertNotNull, Seq(cast(args(0)._1, args(0)._2, ObjectReference)), pass, blockExSelLabel(bb,-2)))
                  case _ => ()
                }
                val fun = style match {
                  case Dynamic if method.isEffectivelyFinal && !method.isDeferred =>
                    new CFunctionAddress(externFun(method))
                  case Dynamic => {
                    val vtbl = nextvar(rtVtable)
                    /* TODO - don't cast to trait if method present in receiver vtable */
                    if (method.owner.isTrait) {
                      args(0) = (cast(args(0)._1, args(0)._2, toTypeKind(method.owner.tpe)), toTypeKind(method.owner.tpe))
                    }
                    val mnum = virtualMethods(method.owner).indexWhere(vm => vm.name == method.name && vm.info <:< method.info)
                    assert(mnum >= 0)
                    if (args.head._2.toType.typeSymbol.isTrait) {
                      val ifaceinfo = args.head._1.asInstanceOf[LMValue[rtIfaceRef.type]]
                      val receiverobj = nextvar(rtObject.pointer)
                      args(0) = (receiverobj, ObjectReference)
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
                insns.append(new call(v, fun, Seq(cast(ref, sym, ObjectReference), cls)))
                stack.push((v, BOOL))
              }
              case CHECK_CAST(tpe) => {
                val (ref,sym) = stack.pop()
                val asobj = cast(ref, sym, ObjectReference)
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
                val (cmptov,cmptosym) = stack.pop
                val (argv,argsym) = stack.pop
                val cmpto = cast(cmptov,cmptosym,kind)
                val arg = cast(argv,argsym,kind)
                val result = nextvar(LMInt.i1)
                def cmp(intop: ICond, uintop: ICond, floatop: FCond) = {
                  argsym match {
                    case CHAR =>
                      insns.append(new icmp(result, uintop, arg.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case BOOL | BYTE | SHORT | INT | LONG =>
                      insns.append(new icmp(result, intop, arg.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case FLOAT =>
                      insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMFloat]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case DOUBLE =>
                      insns.append(new fcmp(result, floatop, arg.asInstanceOf[LMValue[LMDouble]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case REFERENCE(_) | ARRAY(_) =>
                      val argasobj = cast(arg, kind, ObjectReference).asInstanceOf[LMValue[LMPointer]]
                      val cmptoasobj = cast(cmpto, kind, ObjectReference).asInstanceOf[LMValue[LMPointer]]
                      insns.append(new icmp_p(result, intop, argasobj, cmptoasobj))
                  }
                }
                val cond = op match {
                  case EQ => cmp(ICond.ieq, ICond.ieq, FCond.oeq)
                  case NE => cmp(ICond.ine, ICond.ine, FCond.one)
                  case LT => cmp(ICond.slt, ICond.ult, FCond.olt)
                  case GE => cmp(ICond.sge, ICond.uge, FCond.oge)
                  case LE => cmp(ICond.sle, ICond.ule, FCond.ole)
                  case GT => cmp(ICond.sgt, ICond.ugt, FCond.ogt)
                }
                insns.append(new br_cond(result, blockLabel(success), blockLabel(failure)))
              }
              case CZJUMP(success, failure, op, kind) => {
                val arg = stack.pop
                val cmpto = new CZeroInit(arg._1.tpe)
                val result = nextvar(LMInt.i1)
                def cmp(intop: ICond, uintop: ICond, floatop: FCond) = {
                  arg match {
                    case (a,CHAR) =>
                      insns.append(new icmp(result, uintop, a.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case (a,BOOL | BYTE | SHORT | INT | LONG) =>
                      insns.append(new icmp(result, intop, a.asInstanceOf[LMValue[LMInt]], cmpto.asInstanceOf[LMValue[LMInt]]))
                    case (a,FLOAT) =>
                      insns.append(new fcmp(result, floatop, a.asInstanceOf[LMValue[LMFloat]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case (a,DOUBLE) =>
                      insns.append(new fcmp(result, floatop, a.asInstanceOf[LMValue[LMDouble]], cmpto.asInstanceOf[LMValue[LMFloat]]))
                    case (a,tk@(REFERENCE(_) | ARRAY(_))) =>
                      insns.append(new icmp_p(result, intop, cast(a,tk,ObjectReference).asInstanceOf[LMValue[LMPointer]], new CNull(rtObject.pointer)))
                  }
                }
                val cond = op match {
                  case EQ => cmp(ICond.ieq, ICond.ieq, FCond.oeq)
                  case NE => cmp(ICond.ine, ICond.ine, FCond.one)
                  case LT => cmp(ICond.slt, ICond.ult, FCond.olt)
                  case GE => cmp(ICond.sge, ICond.uge, FCond.oge)
                  case LE => cmp(ICond.sle, ICond.ule, FCond.ole)
                  case GT => cmp(ICond.sgt, ICond.ugt, FCond.ogt)
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
                insns.append(new call(uwx, createOurException, Seq(cast(exception, esym, ObjectReference))))
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
                stack.push((cast(exception,ObjectReference,REFERENCE(klass)), REFERENCE(klass)))
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
          } catch { case e => println("Exception processing " + i); throw e } }

          // minus 2 to account for comment after
          val terminator = insns.remove(insns.length-2)

          val tailinsns = new mutable.ListBuffer[LMInstruction]
          //println("Mapping outgoing stack for " + blockName(bb))
          //println("producedTypes = " + producedTypes)
          //println("stack is "  + stack)
          producedTypes.zip(stack.reverse).zipWithIndex.foreach { case ((sym,(src,ssym)),n) =>
            //println("Handling outgoing stack slot %d sym = %s ssym = %s src = %s".format(n,sym,ssym,src))
            val tpe = typeKindType(sym)
            if (sym.isValueType || sym == ConcatClass) {
              tailinsns.append(new select(new LocalVariable(blockName(bb)+".out."+n.toString, tpe), CTrue, src, new CUndef(tpe)))
            } else {
              val temp = cast(src, ssym, ObjectReference)
              val asobj = new LocalVariable(blockName(bb)+".out."+n.toString, rtObject.pointer)
              tailinsns.append(new select(asobj, CTrue, temp, temp))
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
        val handleThis = if (m.symbol.isStaticMember) {
          Seq.empty
        } else {
          val a = args.head
          val thisObj = new LocalVariable(".this.object", rtObject.pointer)
          Seq(
            new alloca(thisCell, rtObject.pointer),
            new bitcast(thisObj, a._1.lmvar),
            new store(thisObj, thisCell)
          )
        }
        blocks.insert(0,LMBlock(Some(Label(".entry.")), allocaLocals ++ handleThis ++ storeArgs ++ Seq(new alloca(currentException, rtObject.pointer), new br(blockLabel(m.code.startBlock)))))
        fun.define(blocks)
      }

      val name = llvmName(c.symbol)
      val outfile = getFile(c.symbol, ".ll")
      val outstream = new OutputStreamWriter(outfile.bufferedOutput,"US-ASCII")
      val header_comment = new Comment("Module for " + c.symbol.fullName('.'))
      val concreteMethods = c.methods.filter(!_.symbol.isDeferred)
      val methods = concreteMethods.filter(m => !CodegenAnnotations.exists(a => m.symbol.hasAnnotation(a)))
      val llvmmethods = concreteMethods.filter(_.symbol.hasAnnotation(LlvmimplAnnotSym))
      val methodFuns = methods.filter(_.code != null).map(m => try { genFun(m) } catch { case e => println(e); m.dump; throw e } )
      val llvmmethodFuns = llvmmethods.map(genNativeFun)
      val foreignFuns = c.methods.filter(_.symbol.hasAnnotation(ForeignAnnotSym)).flatMap(genForeignFun)
      val foreignVals = c.methods.filter(_.symbol.hasAnnotation(ForeignValueAnnotSym)).flatMap(genForeignVal)
      c.methods.filter(_.native).map(_.symbol).foreach(externFun)
      val externDecls = externFuns.filterKeys(!internalFuns.contains(_)).values.map(_.declare)++externClasses.values.map(_.declare)++externStatics.values.map(_.declare)
      val otherModules = externModules.filterKeys(_.moduleClass!=c.symbol)
      val exportedFunctions = c.methods.filter(_.symbol.hasAnnotation(ForeignExportAnnotSym)).flatMap(exportFunction)
      val module = new Module(Seq.concat(
        Seq(header_comment),
        externTypes.values.map {
          case t:LMOpaque with TypeAlias => new TypeAlias(rtObject.aliased(t.name))
          case at => new TypeAlias(at)
        },
        rtHeader,
        externDecls,
        otherModules.values.map(_.declare),
        otherModules.keys.map(mod => moduleInitFun(mod).declare),
        classInfo,
        methodFuns,
        llvmmethodFuns,
        exportedFunctions,
        foreignFuns,
        foreignVals,
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
      val fieldTypes = instFields(c).map(fieldType _)
      val typelist = if (s.superClass != NoSymbol) classType(s.superClass)+:fieldTypes else fieldTypes
      new LMStructure(typelist).aliased(llvmName(s))
    }

    def staticsType(s: Symbol): LMStructure with AliasedType = {
      classes.get(s).map(staticsType _).getOrElse(new LMStructure(Seq.empty).aliased(".static."+llvmName(s)))
    }

    def staticsType(c: IClass): LMStructure with AliasedType = {
      val s = c.symbol
      val fieldTypes = staticFields(c).map(fieldType _)
      new LMStructure(fieldTypes).aliased(".static."+llvmName(s))
    }

    def fieldType(f: IField) = {
      symType(f.symbol)
    }

    def localType(l: Local) = {
      typeKindType(l.kind)
    }

    def typeKindType(tk: TypeKind) = {
      tk match {
        case ConcatClass => LMInt.i8.pointer.pointer
        case _ => typeType(tk.toType)
      }
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

    def staticsName(s: Symbol) = {
      "statics_"+llvmName(s)
    }

    def moduleInstanceName(s: Symbol) = {
      "module_"+llvmName(s)
    }

    def reachable(b: BasicBlock): Boolean = reachable(b, immutable.BitSet.empty)

    def reachable(b: BasicBlock, memo: immutable.BitSet): Boolean = {
      val newmemo = memo + b.label
      (b.code.startBlock == b) || !b.predecessors.filter(p => !newmemo(p.label) && reachable(p,newmemo)).isEmpty
    }

    def stackUsage(bb: BasicBlock): (Seq[TypeKind],Seq[TypeKind]) = {
      stackUsage(bb, immutable.BitSet.empty)
    }

    def stackUsage(bb: BasicBlock, memo: immutable.BitSet): (Seq[TypeKind],Seq[TypeKind]) = {
      val (ict,ipt) = internalUsage(bb)
      val predextras = bb.predecessors.filter(pb => reachable(pb) && !memo(pb.label)).headOption match {
        case Some(p) => stackUsage(p, memo+bb.label)._2.dropRight(ict.length)
        case None => Seq.empty
      }
      //println("stackUsage(%s) = (%s++%s,%s++%s)".format(blockName(bb),predextras,ict,predextras,ipt))
      (predextras++ict,predextras++ipt)
    }

    def internalUsage(bb: BasicBlock) = {
      //println("  internalUsage(%s):".format(blockName(bb)))
      /* first is deepest in produced stack */
      val produced = new mutable.ListBuffer[TypeKind]
      val needed = new mutable.ListBuffer[TypeKind]
      bb foreach { instruction =>
        val consumedTypes = instruction match {
          case CALL_METHOD(m,SuperCall(_)) => {
            toTypeKind(m.owner.tpe) +: instruction.consumedTypes.drop(1)
          }
          case LOAD_EXCEPTION(_) => Seq.empty
          case i => i.consumedTypes
        }
        val producedTypes = instruction match {
          case i => i.producedTypes
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
        needed.prependAll(consumedTypes.dropRight(takenInternally))
        produced.appendAll(producedTypes)
        /*
        println("    after " + instruction)
        println("    needed = " + needed)
        println("    produced = " + produced)
        */
      }
      (needed.toList,produced.toList)
    }

  }

}
