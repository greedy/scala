package java {
  package lang {
    class Error(message: String, cause: Throwable) extends Throwable(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class AssertionError private (message: String) extends Throwable(message) {
      def this(o: Object) = this(if (o == null) null else o.toString)
      def this(b: scala.Boolean) = this(b.toString)
      def this(c: scala.Char) = this(c.toString)
      def this(d: scala.Double) = this(d.toString)
      def this(f: scala.Float) = this(f.toString)
      def this(i: scala.Int) = this(i.toString)
      def this(l: scala.Long) = this(l.toString)
    }
    class Throwable(message: String, cause: Throwable) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
      def getStackTrace(): scala.Array[StackTraceElement] = null
      def getMessage(): String = null
      def printStackTrace(): Unit = ()
      def getCause: Throwable = cause
      def fillInStackTrace(): Throwable = this
    }
    class Exception(message: String, cause: Throwable) extends Throwable(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class RuntimeException(message: String, cause: Throwable) extends Exception(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class UnsupportedOperationException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class IndexOutOfBoundsException(s: String) extends RuntimeException(s) {
      def this() = this(null)
    }
    class ArrayIndexOutOfBoundsException(s: String) extends IndexOutOfBoundsException(s) {
      def this() = this(null)
    }
    class StringIndexOutOfBoundsException(s: String) extends IndexOutOfBoundsException(s) {
      def this() = this(null)
    }
    class NullPointerException(message: String) extends Exception(message) {
      def this() = this(null)
    }
    /*
    object Void {
      val TYPE = null
    }
    */
    abstract class Number {
      def byteValue(): scala.Byte 
      def shortValue(): scala.Short
      def intValue(): scala.Int
      def longValue(): scala.Long
      def floatValue(): scala.Float
      def doubleValue(): scala.Double
    }
    object Boolean {
      val TYPE = null
      def valueOf(b: scala.Boolean): java.lang.Boolean = {
        if (b) TRUE else FALSE
      }
      val TRUE: java.lang.Boolean = new Boolean(true)
      val FALSE: java.lang.Boolean = new Boolean(false)
      def toString(b: scala.Boolean) = valueOf(b).toString
    }
    class Boolean(value: scala.Boolean) {
      def booleanValue(): scala.Boolean = value
      override def toString(): String = if (value) "true" else "false"
    }
    object Byte {
      val TYPE = null
      def valueOf(i: scala.Byte): java.lang.Byte = new Byte(i)
      def parseByte(s: String): scala.Byte = system.error("unimplemented")
      val MIN_VALUE: scala.Byte = -128
      val MAX_VALUE: scala.Byte = 127
      def toString(b: scala.Byte) = valueOf(b).toString
    }
    class Byte(value: scala.Byte) extends Number {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      override def toString(): java.lang.String = "byte"
    }
    object Short {
      val TYPE = null
      def valueOf(i: scala.Short): java.lang.Short = new Short(i)
      def parseShort(s: String): scala.Short = system.error("unimplemented")
      val MIN_VALUE: scala.Short = (1<<15) - 1
      val MAX_VALUE: scala.Short = -(1<<15)
      def toString(s: scala.Short) = valueOf(s).toString
    }
    class Short(value: scala.Short) extends Number {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      override def toString(): java.lang.String = "short"
    }
    object Integer {
      val TYPE = null
      def valueOf(i: scala.Int): java.lang.Integer = new Integer(i)
      def bitCount(i: scala.Int): scala.Int = {
        var x = i;
        var c = 0;
        while (x != 0) {
          c = c + (x & 1);
          x = x >> 1;
        }
        c
      }
      def parseInt(s: String): scala.Int = system.error("unimplemented")
      def parseInt(s: String, radix: scala.Int): scala.Int = system.error("unimplemented")
      def reverseBytes(i: scala.Int): scala.Int = {
        val b0 = i & 0xff;
        val b1 = (i >> 8) & 0xff;
        val b2 = (i >> 16) & 0xff;
        val b3 = (i >> 24) & 0xff;
        (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
      }
      val MIN_VALUE: scala.Int = (1<<31) - 1
      val MAX_VALUE: scala.Int = -(1<<31)

      def toBinaryString(i: scala.Int): java.lang.String = system.error("unimplemented")
      def toHexString(i: scala.Int): java.lang.String = system.error("unimplemented")
      def toOctalString(i: scala.Int): java.lang.String = system.error("unimplemented")
      def toString(i: scala.Int) = valueOf(i).toString
    }
    class Integer(value: scala.Int) extends Number {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      @native override def toString(): java.lang.String
    }
    object Character {
      val TYPE = null
      def valueOf(i: scala.Char): java.lang.Character = new Character(i)
      def getType(c: scala.Char): scala.Int = system.error("unimplemented")
      def getType(c: scala.Int): scala.Int = system.error("unimplemented")
      val MIN_VALUE: scala.Char = 0
      val MAX_VALUE: scala.Char = 0xffffffffL
      val MAX_RADIX: scala.Int = 36

      def reverseBytes(c: scala.Char): scala.Char = system.error("unimplemented")

      val LOWERCASE_LETTER: scala.Byte = 0
      val UPPERCASE_LETTER: scala.Byte = 0
      val OTHER_LETTER: scala.Byte = 0
      val TITLECASE_LETTER: scala.Byte = 0
      val LETTER_NUMBER: scala.Byte = 0
      val COMBINING_SPACING_MARK: scala.Byte = 0
      val ENCLOSING_MARK: scala.Byte = 0
      val NON_SPACING_MARK: scala.Byte = 0
      val MODIFIER_LETTER: scala.Byte = 0
      val DECIMAL_DIGIT_NUMBER: scala.Byte = 0
      val SURROGATE: scala.Byte = 0

      /* Tests */
      def digit(c: scala.Char, radix: scala.Int): scala.Int = system.error("unimplemented")
      def isISOControl(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isDigit(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isLetter(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isLetterOrDigit(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isWhitespace(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isSpaceChar(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isHighSurrogate(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isLowSurrogate(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isUnicodeIdentifierStart(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isUnicodeIdentifierPart(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isIdentifierIgnorable(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isMirrored(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isLowerCase(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isUpperCase(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isTitleCase(c: scala.Char): scala.Boolean = system.error("unimplemented")
      def isJavaIdentifierPart(c: scala.Char): scala.Boolean = system.error("unimplemented")

      def getDirectionality(c: scala.Char): scala.Byte = system.error("unimplemented")

      /* Conversions */
      def toUpperCase(c: scala.Char): scala.Char = system.error("unimplemented")
      def toLowerCase(c: scala.Char): scala.Char = system.error("unimplemented")
      def toTitleCase(c: scala.Char): scala.Char = system.error("unimplemented")
      def getNumericValue(c: scala.Char): scala.Int = system.error("unimplemented")
      def toString(c: scala.Char) = valueOf(c).toString
    }
    class Character(value: scala.Char) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      def charValue(): scala.Char = value
      override def toString(): java.lang.String = "char"
    }
    object Long {
      val TYPE = null
      def valueOf(i: scala.Long) = new Long(i)
      def parseLong(s: String): scala.Long = system.error("unimplemented")
      val MIN_VALUE: scala.Long = (1L<<63) - 1
      val MAX_VALUE: scala.Long = -(1L<<63)

      def toBinaryString(i: scala.Long): java.lang.String = system.error("unimplemented")
      def toHexString(i: scala.Long): java.lang.String = system.error("unimplemented")
      def toOctalString(i: scala.Long): java.lang.String = system.error("unimplemented")
      def toString(i: scala.Long) = valueOf(i).toString
    }
    class Long(value: scala.Long) extends Number {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      override def toString(): java.lang.String = "long"
    }
    object Float {
      val TYPE = null
      def valueOf(i: scala.Float): java.lang.Float = new Float(i)
      def parseFloat(s: String): scala.Float = system.error("unimplemented")
      def compare(a: scala.Float, b: scala.Float): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
      def floatToRawIntBits(f: scala.Float): scala.Int = system.error("unimplemented")
      @native def intBitsToFloat(bits: scala.Int): scala.Float
      val MIN_VALUE           = intBitsToFloat(0x00000001)
      val MAX_VALUE           = intBitsToFloat(0x7f7fffff)
      val NaN                 = intBitsToFloat(0x7fc00000)
      val MIN_NORMAL          = intBitsToFloat(0x00800000)
      val NEGATIVE_INFINITY   = intBitsToFloat(0xff800000)
      val POSITIVE_INFINITY   = intBitsToFloat(0x7f800000)
      val SIZE                = 32
      val MAX_EXPONENT        = 127
      val MIN_EXPONENT        = -126

      @native def isNaN(v: scala.Float): scala.Boolean
      @native def isInfinite(v: scala.Float): scala.Boolean

      def toString(f: scala.Float) = valueOf(f).toString
    }
    class Float(value: scala.Float) extends Number {
      /* Conversions */
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      override def toString(): java.lang.String = "float"

      /* Tests */
      def isNaN: scala.Boolean = Float.isNaN(value)
    }
    object Double {
      val TYPE = null
      def valueOf(i: scala.Double): java.lang.Double = new Double(i)
      def parseDouble(s: String): scala.Double = system.error("unimplemented")
      def compare(a: scala.Double, b: scala.Double): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
      def doubleToRawLongBits(f: scala.Double): scala.Long = system.error("unimplemented")
      @native def longBitsToDouble(bits: scala.Long): scala.Double
      val MAX_VALUE           = longBitsToDouble(0x7fefffffffffffffL)
      val MIN_VALUE           = longBitsToDouble(0x0000000000000001L)
      val MIN_NORMAL          = longBitsToDouble(0x0010000000000000L)
      val NaN                 = longBitsToDouble(0x7ff8000000000000L)
      val NEGATIVE_INFINITY   = longBitsToDouble(0xfff0000000000000L)
      val POSITIVE_INFINITY   = longBitsToDouble(0x7ff0000000000000L)
      val MAX_EXPONENT        = 1023
      val MIN_EXPONENT        = -1022
      val SIZE                = 64

      @native def isNaN(v: scala.Double): scala.Boolean
      @native def isInfinite(v: scala.Double): scala.Boolean
      def toString(d: scala.Double) = valueOf(d).toString
    }
    class Double(value: scala.Double) extends Number {
      /* Conversions */
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      override def toString(): java.lang.String = "double"

      /* Tests */
      def isNaN: scala.Boolean = Double.isNaN(value)
    }
    object System {
      var err: java.io.PrintStream = null
      var out: java.io.PrintStream = null
      var in: java.io.InputStream = null
      def getProperty(key: String): String = system.error("unimplemented")
      def getProperty(key: String, default: String): String = system.error("unimplemented")
      def currentTimeMillis(): scala.Long = system.error("unimplemented")
      def exit(status: scala.Int): Unit = system.error("unimplemented")
      def getenv(): java.util.Map[String,String] = system.error("unimplemented")
      def getenv(name: String): String = system.error("unimplemented")
      def getProperties(): java.util.Properties = system.error("unimplemented")
      def clearProperty(key: String): String = system.error("unimplemented")
      def setProperty(key: String, value: String): String = system.error("unimplemented")
      def arraycopy(src: Object, srcPos: scala.Int, dest: Object, destPos: scala.Int, length: scala.Int): Unit = system.error("unimplemented")
      def identityHashCode(x: Object): scala.Int = system.error("unimplemented")
      def gc(): Unit = {}
    }
    object ThreadLocal {
      class ThreadLocalMap
    }
    class ThreadLocal {
      var v: Object = _
      def get: Object = v
      def remove {}
      def set(o: Object) { v = o }
    }
    class InheritableThreadLocal {
      def childValue(o: Object) = o
      def createMap(t: Thread, o: Object) {}
      def getMap(t: Thread): ThreadLocal.ThreadLocalMap = null
    }
  }
  package util {
    class NoSuchElementException(s: String) extends RuntimeException(s) {
      def this() = this(null)
    }
  }
  package io {
    trait Serializable
    trait Closeable {
      def close(): Unit
    }
    trait Flushable {
      def flush(): Unit
    }
    /*
    abstract class OutputStream extends Object with Closeable with Flushable {
      def close() {}
      def flush() {}
      def write(b: Array[Byte]) {
        write(b, 0, b.length)
      }
      def write(b: Array[Byte], off: Int, len: Int) {
        var n = off;
        val stop = off+len
        while (n < stop) {
          write(b(n))
        }
      }
      def write(b: Int): Unit
    }
    */
    class Reader
    class BufferedReader(r: Reader)
    class InputStreamReader(is: InputStream)
    class InputStream
  }
}

package scala {
  package runtime {
      /*
    object BoxesRunTime {
      def boxToBoolean(b: Boolean) = java.lang.Boolean.valueOf(b)
      def boxToCharacter(c: Char) = java.lang.Character.valueOf(c)
      def boxToByte(b: Byte) = java.lang.Byte.valueOf(b)
      def boxToShort(s: Short) = java.lang.Short.valueOf(s)
      def boxToInteger(i: Int) = java.lang.Integer.valueOf(i)
      def boxToLong(l: Long) = java.lang.Long.valueOf(l)
      def boxToFloat(f: Float) = java.lang.Float.valueOf(f)
      def boxToDouble(d: Double) = java.lang.Double.valueOf(d)
      def unboxToBoolean(b: Object): Boolean = if (b == null) false else b.asInstanceOf[java.lang.Boolean].booleanValue()
      def unboxToChar(c: Object): Char = if (c == null) 0 else c.asInstanceOf[java.lang.Character].charValue()
      def unboxToByte(b: Object): Byte = if (b == null) 0 else b.asInstanceOf[java.lang.Byte].byteValue()
      def unboxToShort(s: Object): Short = if (s == null) 0 else s.asInstanceOf[java.lang.Short].shortValue()
      def unboxToInt(i: Object): Int = if (i == null) 0 else i.asInstanceOf[java.lang.Integer].intValue()
      def unboxToLong(l: Object): Long = if (l == null) 0 else l.asInstanceOf[java.lang.Long].longValue()
      def unboxToFloat(f: Object): Float = if (f == null) 0 else f.asInstanceOf[java.lang.Float].floatValue()
      def unboxToDouble(d: Object): Double = if (d == null) 0 else d.asInstanceOf[java.lang.Double].doubleValue()
      def equals(x: Object, y: Object): Boolean = if (x == y) true else x.equals(y)
      def equals2(x: Object, y: Object): Boolean = if (x == null) y == null else x.equals(y)
      def equalsNumObject(xn: Number, y: Object): Boolean = equals2(xn,y)
      def equalsNumNum(xn: Number, yn: Number): Boolean = equals2(xn,yn)
      def equalsCharObject(xc: Character, y: Object): Boolean = equals2(xc,y)
      def equalsCharChar(xc: Character, yc: Character): Boolean = equals2(xc,yc)
      def equalsNumChar(xn: Number, yc: Character): Boolean = equals2(xn,yc)
      def hashFromNumber(n: Number) = n.intValue
    }
      */
  }
  /*
  trait Serializable
  object Console {
    @native def print(s: String): Unit
    def println(s: String): Unit = {
      print(s)
      print("\n")
    }
    def println(d: Double): Unit = {
      println("" + d)
    }
    def printf(fmt: String, args: Any*): Unit = { }
  }
  */
  /*
  package runtime {
    class ObjectRef(var elem: Object) extends Object with java.io.Serializable {
      override def toString() = "" + elem
    }
    class BooleanRef(var elem: scala.Boolean) extends Object with java.io.Serializable {
      override def toString() = "" + elem
    }
    class ByteRef(var elem: scala.Byte) extends Object with java.io.Serializable {
      override def toString() = java.lang.Byte.toString(elem)
    }
    class CharRef(var elem: scala.Char) extends Object with java.io.Serializable {
      override def toString() = java.lang.Character.toString(elem)
    }
    class DoubleRef(var elem: scala.Double) extends Object with java.io.Serializable {
      override def toString() = java.lang.Double.toString(elem)
    }
    class FloatRef(var elem: scala.Float) extends Object with java.io.Serializable {
      override def toString() = java.lang.Float.toString(elem)
    }
    class IntRef(var elem: scala.Int) extends Object with java.io.Serializable {
      override def toString() = java.lang.Integer.toString(elem)
    }
    class LongRef(var elem: scala.Long) extends Object with java.io.Serializable {
      override def toString() = java.lang.Long.toString(elem)
    }
    class ShortRef(var elem: scala.Short) extends Object with java.io.Serializable {
      override def toString() = java.lang.Short.toString(elem)
    }
    class VolatileObjectRef(var elem: Object) extends Object with java.io.Serializable {
      override def toString() = "" + elem
    }
    class VolatileBooleanRef(var elem: scala.Boolean) extends Object with java.io.Serializable {
      override def toString() = "" + elem
    }
    class VolatileByteRef(var elem: scala.Byte) extends Object with java.io.Serializable {
      override def toString() = java.lang.Byte.toString(elem)
    }
    class VolatileCharRef(var elem: scala.Char) extends Object with java.io.Serializable {
      override def toString() = java.lang.Character.toString(elem)
    }
    class VolatileDoubleRef(var elem: scala.Double) extends Object with java.io.Serializable {
      override def toString() = java.lang.Double.toString(elem)
    }
    class VolatileFloatRef(var elem: scala.Float) extends Object with java.io.Serializable {
      override def toString() = java.lang.Float.toString(elem)
    }
    class VolatileIntRef(var elem: scala.Int) extends Object with java.io.Serializable {
      override def toString() = java.lang.Integer.toString(elem)
    }
    class VolatileLongRef(var elem: scala.Long) extends Object with java.io.Serializable {
      override def toString() = java.lang.Long.toString(elem)
    }
    class VolatileShortRef(var elem: scala.Short) extends Object with java.io.Serializable {
      override def toString() = java.lang.Short.toString(elem)
    }
  }
  */
}
