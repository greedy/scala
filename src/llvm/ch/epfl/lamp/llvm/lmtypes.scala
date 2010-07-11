package ch.epfl.lamp.llvm

import scala.util.JenkinsHash
import scala.text.Document

trait ConcreteType extends LMType {
  def pointer = new LMPointer(this)
}
abstract class LMType {
  def rep: String
  def realrep = rep
  def aliased(n: String): ConcreteType
}
trait AliasedType extends ConcreteType {
  val name: String
  abstract override def rep = "%\""+name+"\""
  override def realrep = super.rep
}
abstract class LMPrimitiveType extends LMType
class LMInt(val bits: Int) extends LMPrimitiveType with ConcreteType {
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMInt => this.bits == ot.bits
    case _ => false
  }
  override def hashCode = bits.hashCode
  def rep = "i"+bits.toString
  def aliased(n: String) = new LMInt(bits) with AliasedType { val name = n }
}
object LMInt {
  val i1 = new LMInt(1)
  val i8 = new LMInt(8)
  val i16 = new LMInt(16)
  val i32 = new LMInt(32)
  val i64 = new LMInt(64)
}
abstract class LMFloatingPointType(val bits: Int, val syntax: String) extends LMPrimitiveType {
  def rep = syntax
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMFloatingPointType => this.syntax == ot.syntax
    case _ => false
  }
  override def hashCode = syntax.hashCode
}
class LMFloat extends LMFloatingPointType(32,"float") with ConcreteType {
  def aliased(n: String) = new LMFloat with AliasedType { val name = n }
}
object LMFloat extends LMFloat
class LMDouble extends LMFloatingPointType(64,"double") with ConcreteType {
  def aliased(n: String) = new LMDouble with AliasedType { val name = n }
}
object LMDouble extends LMDouble
class LMFP128 extends LMFloatingPointType(128,"fp128") with ConcreteType {
  def aliased(n: String) = new LMFP128 with AliasedType { val name = n }
}
object LMFP128 extends LMFP128
class LMx86_fp80 extends LMFloatingPointType(80,"x86_fp80") with ConcreteType {
  def aliased(n: String) = new LMx86_fp80 with AliasedType { val name = n }
}
object LMx86_fp80 extends LMx86_fp80
class LMppc_fp128 extends LMFloatingPointType(128,"ppc_fp128") with ConcreteType {
  def aliased(n: String) = new LMppc_fp128 with AliasedType { val name = n }
}
object LMppc_fp128
class LMVoid extends LMPrimitiveType with ConcreteType {
  def rep = "void"
  def aliased(n: String) = new LMVoid with AliasedType { val name = n }
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case _:LMVoid => true
    case _ => false
  }
  override def hashCode = 0
}
object LMVoid extends LMVoid
class LMLabel extends LMPrimitiveType with ConcreteType {
  def rep = "label"
  def aliased(n: String) = new LMLabel with AliasedType { val name = n }
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case _:LMLabel => true
    case _ => false
  }
  override def hashCode = 0
}
object LMLabel extends LMLabel
class LMMetadata extends LMPrimitiveType with ConcreteType {
  def rep = "metadata"
  def aliased(n: String) = new LMMetadata with AliasedType { val name = n }
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case _:LMMetadata => true
    case _ => false
  }
  override def hashCode = 0
}
object LMMetadata extends LMMetadata
abstract class LMDerivedType extends LMType
abstract class LMAggregateType extends LMDerivedType
class LMArray(val num: Int, _elementtype: =>ConcreteType) extends LMAggregateType with ConcreteType {
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMArray => this.num == ot.num && this.elementtype == ot.elementtype
    case _ => false
  }
  override def hashCode = elementtype.hashCode ^ num
  lazy val elementtype = _elementtype
  def rep = "[ "+num.toString+" x "+elementtype.rep+" ]"
  def aliased(n: String) = new LMArray(num, _elementtype) with AliasedType { val name = n }
}
class LMFunctionType(_returnType: =>ConcreteType, _argTypes: =>Seq[ConcreteType], varargs: Boolean) extends LMDerivedType with ConcreteType {
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMFunctionType => this.returnType == ot.returnType && this.argTypes.sameElements(ot.argTypes)
    case _ => false
  }
  override def hashCode = JenkinsHash.hashSeq(returnType +: argTypes)
  lazy val returnType = _returnType
  lazy val argTypes = _argTypes
  def rep = {
    val args = if (varargs) argTypes.map(_.rep) :+ "..." else argTypes.map(_.rep)
    returnType.rep+args.mkString("(",", ",")")
  }
  def aliased(n: String) = new LMFunctionType(_returnType, _argTypes, varargs) with AliasedType { val name = n }
}
class LMStructure(_types: =>Seq[ConcreteType]) extends LMAggregateType with ConcreteType {
  lazy val types = _types
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMStructure => this.types.sameElements(ot.types)
    case _ => false
  }
  override def hashCode = types.length
  def rep = types.map(_.rep).mkString("{ ",", "," }")
  def aliased(n: String) = new LMStructure(_types) with AliasedType { val name = n }
}
class LMPackedStructure(_types: =>Seq[ConcreteType]) extends LMAggregateType with ConcreteType {
  lazy val types = _types
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMPackedStructure => this.types.sameElements(ot.types)
    case _ => false
  }
  override def hashCode = JenkinsHash.hashSeq(types)
  def rep = types.map(_.rep).mkString("<{ ",", "," }>")
  def aliased(n: String) = new LMPackedStructure(_types) with AliasedType { val name = n }
}
class LMUnion(_types: =>Seq[ConcreteType]) extends LMAggregateType with ConcreteType {
  lazy val types = _types
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMUnion => this.types.sameElements(ot.types)
    case _ => false
  }
  override def hashCode = JenkinsHash.hashSeq(types)
  def rep = types.map(_.rep).mkString("union { ",", "," }")
  def aliased(n: String) = new LMUnion(_types) with AliasedType { val name = n }
}
class LMPointer(_target: =>ConcreteType) extends LMDerivedType with ConcreteType {
  lazy val target = _target
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMPointer => this.target == ot.target
    case _ => false
  }
  override def hashCode = target.hashCode
  def rep = target.rep+"*"
  def aliased(n: String) = new LMPointer(_target) with AliasedType { val name = n }
}
class LMVector(val n: Int, val elementtype: LMPrimitiveType with ConcreteType) extends LMAggregateType with ConcreteType {
  override def equals(ot: Any) = ot match {
    case ar:AnyRef if (ar eq this) => true
    case ot:LMVector => this.n == ot.n && this.elementtype == ot.elementtype
    case _ => false
  }
  override def hashCode = elementtype.hashCode ^ n
  def rep = "< "+n.toString+" x "+elementtype.rep+" >"
  def aliased(nme: String) = new LMVector(n, elementtype) with AliasedType { val name = nme }
}
class LMOpaque extends LMType with ConcreteType {
  def rep = "opaque"
  def aliased(n: String) = new LMOpaque with AliasedType { val name = n }
}
object LMOpaque extends LMOpaque
case class LMUpreference(n: Int) extends LMType with ConcreteType {
  def rep = "\\"+n.toString
  def aliased(n: String) = error("cannot alias up references")
}
