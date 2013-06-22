import scala.ffi._
package java {
  package lang {
    object Math {
      private def TODO = error("unimplemented")
      @foreign("sqrt")
      def sqrt(d: scala.Double): scala.Double = error("foregin")
      def log10(x: scala.Double): scala.Double = TODO
      def cbrt(x: scala.Double): scala.Double = TODO
      def ulp(x: scala.Double): scala.Double = TODO
      def ulp(x: scala.Float): scala.Float = TODO
      def sinh(x: scala.Double): scala.Double = TODO
      def cosh(x: scala.Double): scala.Double = TODO
      def tanh(x: scala.Double): scala.Double = TODO
      def hypot(x: scala.Double, y: scala.Double): scala.Double = TODO
      def expm1(x: scala.Double): scala.Double = TODO
      def log1p(x: scala.Double): scala.Double = TODO
      final val E: scala.Double = 2.718281828459045
      final val PI: scala.Double = 3.141592653589793
      def random(): scala.Double = TODO
      def sin(x: scala.Double): scala.Double = TODO
      def cos(x: scala.Double): scala.Double = TODO
      def tan(x: scala.Double): scala.Double = TODO
      def asin(x: scala.Double): scala.Double = TODO
      def acos(x: scala.Double): scala.Double = TODO
      def atan(x: scala.Double): scala.Double = TODO
      def toRadians(x: scala.Double): scala.Double = TODO
      def toDegrees(x: scala.Double): scala.Double = TODO
      def exp(x: scala.Double): scala.Double = TODO
      def log(x: scala.Double): scala.Double = TODO
      def IEEEremainder(x: scala.Double, y: scala.Double): scala.Double = TODO
      def ceil(x: scala.Double): scala.Double = TODO
      def floor(x: scala.Double): scala.Double = TODO
      def rint(x: scala.Double): scala.Double = TODO
      def atan2(y: scala.Double, x: scala.Double): scala.Double = TODO
      def pow(x: scala.Double, y: scala.Double): scala.Double = TODO
      def round(x: scala.Float): scala.Int = TODO
      def round(x: scala.Double): scala.Long = TODO
      def abs(x: scala.Int): scala.Int = TODO
      def abs(x: scala.Long): scala.Long = TODO
      def abs(x: scala.Float): scala.Float = TODO
      def abs(x: scala.Double): scala.Double = TODO
      def max(x: scala.Int, y: scala.Int): scala.Int = if (x > y) x else y
      def max(x: scala.Long, y: scala.Long): scala.Long = if (x > y) x else y
      def max(x: scala.Float, y: scala.Float): scala.Float = if (x > y) x else y
      def max(x: scala.Double, y: scala.Double): scala.Double = if (x > y) x else y
      def min(x: scala.Int, y: scala.Int): scala.Int = if (x < y) x else y
      def min(x: scala.Long, y: scala.Long): scala.Long = if (x < y) x else y
      def min(x: scala.Float, y: scala.Float): scala.Float = if (x < y) x else y
      def min(x: scala.Double, y: scala.Double): scala.Double = if (x < y) x else y
    }
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
      def getMessage(): String = message
      def printStackTrace(): Unit = ()
      def getCause: Throwable = cause
      def fillInStackTrace(): Throwable = this
      override def toString = {
        getClass().getName() + ": " + (if (message ne null) message else "(null)")
      }
    }
    class Exception(message: String, cause: Throwable) extends Throwable(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class ArrayStoreException(message: String, cause: Throwable) extends RuntimeException(message, cause) {
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
    class IllegalArgumentException(message: String) extends Exception(message) {
      def this() = this(null)
    }
    trait Comparable[T] {
      def compareTo(o: T): Int
    }
    class Void private()
    object Void {
      val TYPE: Class[Void] = null
    }
    abstract class Number {
      def byteValue: scala.Byte = shortValue.toByte
      def shortValue: scala.Short = intValue.toShort
      def intValue: scala.Int
      def longValue: scala.Long
      def floatValue: scala.Float
      def doubleValue: scala.Double
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
      def parseByte(s: String): scala.Byte = sys.error("parseByte unimplemented")
      val MIN_VALUE: scala.Byte = -128
      val MAX_VALUE: scala.Byte = 127
      def toString(b: scala.Byte) = valueOf(b).toString
    }
    class Byte(value: scala.Byte) extends Number {
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String = "byte"
    }
    object Short {
      val TYPE = null
      def valueOf(i: scala.Short): java.lang.Short = new Short(i)
      def parseShort(s: String): scala.Short = sys.error("parseShort unimplemented")
      val MIN_VALUE: scala.Short = 32767
      val MAX_VALUE: scala.Short = -32768
      def toString(s: scala.Short) = valueOf(s).toString
    }
    class Short(value: scala.Short) extends Number {
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String = "short"
    }
    object Integer {
      val TYPE = null
      def valueOf(i: scala.Int): java.lang.Integer = new Integer(i)
      def bitCount(i: scala.Int): scala.Int = {
        var x = i;
        var c = 0;
        while (x != 0) {
          c = c + (x & 1);
          x = x >>> 1;
        }
        c
      }
      def parseInt(s: String): scala.Int = 10
      def parseInt(s: String, radix: scala.Int): scala.Int = 10
      def reverseBytes(i: scala.Int): scala.Int = {
        val b0 = i & 0xff;
        val b1 = (i >> 8) & 0xff;
        val b2 = (i >> 16) & 0xff;
        val b3 = (i >> 24) & 0xff;
        (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
      }
      val MIN_VALUE: scala.Int = (1<<31)
      val MAX_VALUE: scala.Int = (1<<31)-1

      def toBinaryString(i: scala.Int): java.lang.String = sys.error("inttoBinaryString unimplemented")
      def toHexString(i: scala.Int): java.lang.String = sys.error("inttoHexString unimplemented")
      def toOctalString(i: scala.Int): java.lang.String = sys.error("inttoOctalString unimplemented")
      def toString(i: scala.Int) = valueOf(i).toString

      def rotateLeft(i: scala.Int, amt: scala.Int) = (i << (32-amt)) | (i >>> amt)
      def signum(i: scala.Int) = if (i < 0) -1 else if (i == 0) 0 else 1

      def highestOneBit(i: scala.Int) = {
        var x = 1<<31
        while ((x != 0) && ((i & x) != 0)) {
          x = x>>>1;
        }
        x
      }
      def numberOfLeadingZeros(i: scala.Int) = {
        var x = 0
        while ((i & (1<<31)) != 0) {
          x += 1
          i << 1
        }
        x
      }
    }
    class Integer(value: scala.Int) extends Number {
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String
    }
    object Character {
      val TYPE = null
      def valueOf(i: scala.Char): java.lang.Character = new Character(i)
      def getType(c: scala.Char): scala.Int = sys.error("getType(char) unimplemented")
      def getType(c: scala.Int): scala.Int = sys.error("getType(int) unimplemented")
      val MIN_VALUE: scala.Char = 0
      val MAX_VALUE: scala.Char = 0xffff.toChar
      val MAX_RADIX: scala.Int = 36

      def reverseBytes(c: scala.Char): scala.Char = sys.error("charreverseBytes unimplemented")

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
      def digit(c: scala.Char, radix: scala.Int): scala.Int = sys.error("digit unimplemented")
      def isISOControl(c: scala.Char): scala.Boolean = sys.error("isiso unimplemented")
      def isDigit(c: scala.Char): scala.Boolean = sys.error("isdigit unimplemented")
      def isLetter(c: scala.Char): scala.Boolean = sys.error("isletter unimplemented")
      def isLetterOrDigit(c: scala.Char): scala.Boolean = sys.error("isletterdigit unimplemented")
      def isWhitespace(c: scala.Char): scala.Boolean = sys.error("iswhite unimplemented")
      def isSpaceChar(c: scala.Char): scala.Boolean = sys.error("isspacechar unimplemented")
      def isHighSurrogate(c: scala.Char): scala.Boolean = sys.error("ishigh unimplemented")
      def isLowSurrogate(c: scala.Char): scala.Boolean = sys.error("islow unimplemented")
      def isUnicodeIdentifierStart(c: scala.Char): scala.Boolean = sys.error("isidentstart unimplemented")
      def isUnicodeIdentifierPart(c: scala.Char): scala.Boolean = sys.error("isidentpart unimplemented")
      def isIdentifierIgnorable(c: scala.Char): scala.Boolean = sys.error("isidentingore unimplemented")
      def isMirrored(c: scala.Char): scala.Boolean = sys.error("ismirr unimplemented")
      def isLowerCase(c: scala.Char): scala.Boolean = sys.error("islow unimplemented")
      def isUpperCase(c: scala.Char): scala.Boolean = sys.error("isup unimplemented")
      def isTitleCase(c: scala.Char): scala.Boolean = sys.error("istitle unimplemented")
      def isJavaIdentifierPart(c: scala.Char): scala.Boolean = sys.error("isjavaidnetpart unimplemented")

      def getDirectionality(c: scala.Char): scala.Byte = sys.error("getdir unimplemented")

      /* Conversions */
      def toUpperCase(c: scala.Char): scala.Char = sys.error("toup unimplemented")
      def toLowerCase(c: scala.Char): scala.Char = sys.error("tolow unimplemented")
      def toTitleCase(c: scala.Char): scala.Char = sys.error("totitle unimplemented")
      def getNumericValue(c: scala.Char): scala.Int = sys.error("getnumval unimplemented")
      def toString(c: scala.Char) = valueOf(c).toString
    }
    class Character(value: scala.Char) {
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      def charValue: scala.Char = value
      override def toString(): java.lang.String = "char"
    }
    object Long {
      val TYPE = null
      def valueOf(i: scala.Long) = new Long(i)
      def parseLong(s: String): scala.Long = sys.error("parseLong unimplemented")
      val MIN_VALUE: scala.Long = (1L<<63)
      val MAX_VALUE: scala.Long = (1L<<63)-1

      def bitCount(i: scala.Long): scala.Int = {
        var x = i;
        var c = 0;
        while (x != 0) {
          c = c + (x & 1).toInt;
          x = x >>> 1;
        }
        c
      }

      def toBinaryString(i: scala.Long): java.lang.String = sys.error("longtobinstring unimplemented")
      def toHexString(i: scala.Long): java.lang.String = sys.error("longtohexstring unimplemented")
      def toOctalString(i: scala.Long): java.lang.String = sys.error("longtooctalstring unimplemented")
      def toString(i: scala.Long) = valueOf(i).toString
      def signum(l: scala.Long) = if (l < 0) -1 else if (l == 0) 0 else 1
    }
    class Long(value: scala.Long) extends Number {
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String = "long"
    }
    object Float {
      val TYPE = null
      def valueOf(i: scala.Float): java.lang.Float = new Float(i)
      def parseFloat(s: String): scala.Float = sys.error("parsefloat unimplemented")
      def compare(a: scala.Float, b: scala.Float): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
      def floatToRawIntBits(f: scala.Float): scala.Int = sys.error("floatrawintbits unimplemented")
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
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String = "float"

      /* Tests */
      def isNaN: scala.Boolean = Float.isNaN(value)
    }
    object Double {
      val TYPE = null
      def valueOf(i: scala.Double): java.lang.Double = new Double(i)
      def parseDouble(s: String): scala.Double = sys.error("parsedouble unimplemented")
      def compare(a: scala.Double, b: scala.Double): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
      def doubleToRawLongBits(f: scala.Double): scala.Long = sys.error("doublerawbits unimplemented")
      def doubleToLongBits(f: scala.Double): scala.Long = sys.error("doublebits unimplemented")
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
      // def byteValue: scala.Byte = value.toByte
      // def shortValue: scala.Short = value.toShort
      def intValue: scala.Int = value.toInt
      def longValue: scala.Long = value.toLong
      def floatValue: scala.Float = value.toFloat
      def doubleValue: scala.Double = value.toDouble
      @native override def toString(): java.lang.String = "double"

      /* Tests */
      def isNaN: scala.Boolean = Double.isNaN(value)
    }
    object StandardError extends io.OutputStream {
      @native def write(b: Int): Unit
      @native override def flush(): Unit
    }
    object StandardOut extends io.OutputStream {
      @native def write(b: Int): Unit
      @native override def flush(): Unit
    }
    object System {
      // def nanoTime(): Long = 0L
      var err: java.io.PrintStream = new io.PrintStream(StandardError, true)
      var out: java.io.PrintStream = new io.PrintStream(StandardOut, true)
      var in: java.io.InputStream = null
      def getProperty(key: String): String = sys.error("getproperty unimplemented")
      def getProperty(key: String, default: String): String = sys.error("getproperty default unimplemented")
      def currentTimeMillis(): scala.Long = sys.error("curtimemill unimplemented")
      def exit(status: scala.Int): Unit = sys.error("exit unimplemented")
      def getenv(): java.util.Map[String,String] = sys.error("getenv unimplemented")
      def getenv(name: String): String = sys.error("getenv name unimplemented")
      def getProperties(): java.util.Properties = sys.error("getproperties unimplemented")
      def clearProperty(key: String): String = sys.error("clearprop unimplemented")
      def setProperty(key: String, value: String): String = sys.error("setprop unimplemented")
      def arraycopy(src: Object, srcPos: scala.Int, dest: Object, destPos: scala.Int, length: scala.Int): Unit = {
        (src,dest) match {
          case (src:Array[Boolean],dest:Array[Boolean]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Char],dest:Array[Char]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Int],dest:Array[Int]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Long],dest:Array[Long]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Float],dest:Array[Float]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Double],dest:Array[Double]) => {
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case (src:Array[Object],dest:Array[Object]) => {
            /* FIXME - check that stores are type safe */
            if (srcPos < 0) throw new IndexOutOfBoundsException()
            if (destPos < 0) throw new IndexOutOfBoundsException()
            if (length < 0) throw new IndexOutOfBoundsException()
            if (srcPos+length > src.length) throw new IndexOutOfBoundsException()
            if (destPos+length > dest.length) throw new IndexOutOfBoundsException()
            var i = 0
            while (i < length) {
              dest(destPos+i) = src(srcPos+i)
              i = i + 1
            }
          }
          case _ => {
            throw new ArrayStoreException()
          }
        }
        val srcArray = if (src.isInstanceOf[Array[_]]) src.asInstanceOf[Array[_]] else throw new ArrayStoreException("source is not an array")
        val destArray = if (dest.isInstanceOf[Array[_]]) dest.asInstanceOf[Array[_]] else throw new ArrayStoreException("dest is not an array")
      }
      def identityHashCode(x: Object): scala.Int = sys.error("identityHashCode unimplemented")
      def gc(): Unit = {}
      @native def debugPointer(o: Object): Unit
      @native def debugString(o: String): Unit
    }
    object ThreadLocal {
      class ThreadLocalMap
    }
    class ThreadLocal[T] {
      final var hasValue = false
      final var i: T = _
      final var v: T = _
      final var m: ThreadLocal.ThreadLocalMap = new ThreadLocal.ThreadLocalMap()
      protected def initialValue: T = i
      def get(): T = {
        if (!hasValue) { set(initialValue) };
        v
      }
      def remove() { hasValue = false }
      def set(o: T) { v = o; hasValue = true; }
      def childValue(parentValue: T): T = parentValue
      def createMap(t: Thread, firstValue: T) {}
      def getMap(t: Thread) = m
    }
    class InheritableThreadLocal[T] extends ThreadLocal[T] {
    }
    object String {
      @native def utf8bytes(s: String): Array[scala.Byte]
      def valueOf(b: Boolean): String = b.toString
      def valueOf(c: Char): String = c.toString
      def valueOf(i: Int): String = i.toString
      def valueOf(l: Long): String = l.toString
      def valueOf(f: Float): String = f.toString
      def valueOf(d: Double): String = d.toString
      def valueOf(x: Any): String = x match {
        case b: Boolean => valueOf(b)
        case c: Char => valueOf(c)
        case i: Int => valueOf(i)
        case l: Long => valueOf(l)
        case f: Float => valueOf(f)
        case d: Double => valueOf(d)
        case s: Array[Char] => valueOf(s)
        case s: String => valueOf(s)
        case o: Object => if (o eq null) "null" else o.toString
      }
      def format(formatString: String, args: Any*) = "formatted string"
      def format(l: java.util.Locale, formatString: String, args: Any*) = "formatted string"
    }
  }
  package util {
    class NoSuchElementException(s: String) extends RuntimeException(s) {
      def this() = this(null)
    }
    class Locale
    trait Comparator[T] {
      def compare(o1: T, o2: T): Int
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
          n = n+1
        }
      }
      def write(b: Int): Unit
    }
    class FilterOutputStream(out: OutputStream) extends OutputStream {
      override def close() = out.close()
      override def flush() = out.flush()
      override def write(b: Int) = out.write(b)
    }
    trait Appendable {
      def append(c: Char): Appendable
      def append(csq: CharSequence): Appendable
      def append(csq: CharSequence, start: Int, end: Int): Appendable
    }
    class PrintStream(_out: OutputStream, autoFlush: Boolean, ecoding: String) extends FilterOutputStream(_out) with Appendable {
      //System.debugString("In PrintStream init _out is")
      //System.debugPointer(_out)
      import java.util.Locale
      def this(out: OutputStream) = this(out, false, "")
      def this(out: OutputStream, autoFlush: Boolean) = this(out, autoFlush, "")
      override def write(b: Int) = {
        //System.debugString("In PrintStream.write, _out is")
        //System.debugPointer(_out)
        _out.write(b)
        if (autoFlush && b == 10) flush()
      }
      def append(c: Char) = this
      def append(csq: CharSequence) = this
      def append(csq: CharSequence, start: Int, end: Int) = this
      var hasError = false
      def checkError() = hasError
      def setError() { hasError = true }
      def clearError() { hasError = false }
      def print(b: Boolean): Unit = print(b.toString)
      def print(c: Char): Unit = print(c.toString)
      def print(i: Int): Unit = print(i.toString)
      def print(l: Long): Unit = print(l.toString)
      def print(f: Float): Unit = print(f.toString)
      def print(d: Double): Unit = print(d.toString)
      def print(s: Array[Char]): Unit = print("character array")
      def print(s: String): Unit = if (s eq null) write(String.utf8bytes("null")) else { write(String.utf8bytes(s)) }
      def print(x: Any): Unit = x match {
        case b: Boolean => print(b)
        case c: Char => print(c)
        case i: Int => print(i)
        case l: Long => print(l)
        case f: Float => print(f)
        case d: Double => print(d)
        case s: Array[Char] => print(s)
        case s: String => print(s)
        case o: Object => if (o eq null) print("null") else print(o.toString)
      }
      def println(): Unit = write(10)
      def println(x: Boolean): Unit = { print(x); println() }
      def println(x: Char): Unit = { print(x); println() }
      def println(x: Int): Unit = { print(x); println() }
      def println(x: Long): Unit = { print(x); println() }
      def println(x: Float): Unit = { print(x); println() }
      def println(x: Double): Unit = { print(x); println() }
      def println(x: String): Unit = { print(x); println() }
      def println(x: Any): Unit = { print(x); println() }
      def printf(format: String, args: Object*): Unit = this.format(format, args:_*)
      def printf(l: Locale, format: String, args: Object*): Unit = this.format(l, format, args:_*)
      def format(format: String, args: Object*): Unit = {
        print(format)
        for (arg <- args) { print(" ");print(arg) }
        println()
      }
      def format(l: Locale, format: String, args: Object*): Unit = this.format(format, args:_*)
    }
  }
  package math {
    sealed trait RoundingMode
    object RoundingMode {
      case object CEILING extends RoundingMode
      case object DOWN extends RoundingMode
      case object FLOOR extends RoundingMode
      case object HALF_DOWN extends RoundingMode
      case object HALF_EVEN extends RoundingMode
      case object HALF_UP extends RoundingMode
      case object UNNECESSARY extends RoundingMode
      case object UP extends RoundingMode
      def valueOf(n: Int): RoundingMode = error("rmvalueofint unimplemented")
      def valueOf(name: String): RoundingMode = error("rmvalueofstr unimplemented")
      def values: Array[RoundingMode] = error("values unimplmented")
    }
    object MathContext {
      val DECIMAL128: MathContext = null
      val DECIMAL32: MathContext = null
      val DECIMAL64: MathContext = null
      val UNLIMITED: MathContext = null
    }
    class MathContext(setPrecision: Int, setRoundingMode: RoundingMode) {
      def this(setPrecision: Int) = this(setPrecision, RoundingMode.HALF_UP)
      def this(stringValue: String) = this(0, RoundingMode.HALF_UP)
      def getPrecision = setPrecision
      def getRoundingMode = setRoundingMode
    }
  }
}

package scala {
  package math {
    abstract class ScalaNumber extends java.lang.Number {
      protected def isWhole(): Boolean
      def underlying(): Object
    }
  }
  package runtime {
    object BoxesRunTime {
      def boxToBoolean(b: Boolean): java.lang.Boolean = java.lang.Boolean.valueOf(b)
      def boxToCharacter(c: Char): java.lang.Character = java.lang.Character.valueOf(c)
      def boxToByte(b: Byte): java.lang.Byte = java.lang.Byte.valueOf(b)
      def boxToShort(s: Short): java.lang.Short = java.lang.Short.valueOf(s)
      def boxToInteger(i: Int): java.lang.Integer = java.lang.Integer.valueOf(i)
      def boxToLong(l: Long): java.lang.Long = java.lang.Long.valueOf(l)
      def boxToFloat(f: Float): java.lang.Float = java.lang.Float.valueOf(f)
      def boxToDouble(d: Double): java.lang.Double = java.lang.Double.valueOf(d)
      def unboxToBoolean(b: Object): Boolean = if (b eq null) false else b.asInstanceOf[java.lang.Boolean].booleanValue()
      def unboxToChar(c: Object): Char = if (c eq null) 0 else c.asInstanceOf[java.lang.Character].charValue
      def unboxToByte(b: Object): Byte = if (b eq null) 0 else b.asInstanceOf[java.lang.Byte].byteValue
      def unboxToShort(s: Object): Short = if (s eq null) 0 else s.asInstanceOf[java.lang.Short].shortValue
      def unboxToInt(i: Object): Int = if (i eq null) 0 else i.asInstanceOf[java.lang.Integer].intValue
      def unboxToLong(l: Object): Long = if (l eq null) 0 else l.asInstanceOf[java.lang.Long].longValue
      def unboxToFloat(f: Object): Float = if (f eq null) 0 else f.asInstanceOf[java.lang.Float].floatValue
      def unboxToDouble(d: Object): Double = if (d eq null) 0 else d.asInstanceOf[java.lang.Double].doubleValue
      def equalsExternal(x: Object, y: Object): Boolean = if (x eq y) true else equals2(x,y)
      def equals2(x: Object, y: Object): Boolean = {
        x match {
          case xn: java.lang.Number => equalsNumObject(xn, y)
          case xc: java.lang.Character => equalsCharObject(xc, y)
          case x => if (x eq null) (y eq null) else x.equals(y)
        }
      }
      def equalsNumObject(xn: java.lang.Number, y: Object): Boolean = {
        y match {
          case yn: java.lang.Number => equalsNumNum(xn, yn)
          case yc: java.lang.Character => equalsNumChar(xn, yc)
          case y => if (xn eq null) (y eq null) else xn.equals(y)
        }
      }

      private object Codes {
        val CHAR = 0
        val BYTE = 1
        val SHORT = 2
        val INT = 3
        val LONG = 4
        val FLOAT = 5
        val DOUBLE = 6
        val OTHER = 7
      }

      private def typeCode(a: Object): Int = {
        import Codes._
        a match {
          case _:java.lang.Integer => INT
          case _:java.lang.Byte => BYTE
          case _:java.lang.Character => CHAR
          case _:java.lang.Long => LONG
          case _:java.lang.Double => DOUBLE
          case _:java.lang.Short => SHORT
          case _:java.lang.Float => FLOAT
          case _ => OTHER
        }
      }

      private def eqTypeCode(a: java.lang.Number): Int = {
        import Codes._
        a match {
          case _:java.lang.Integer => INT
          case _:java.lang.Byte => INT
          case _:java.lang.Long => LONG
          case _:java.lang.Double => DOUBLE
          case _:java.lang.Short => INT
          case _:java.lang.Float => FLOAT
          case _ => OTHER
        }
      }
      def equalsNumNum(xn: java.lang.Number, yn: java.lang.Number): Boolean = {
        import scala.math.ScalaNumber
        import Codes._
        val xcode = eqTypeCode(xn)
        val ycode = eqTypeCode(yn)
        val dcode = if (ycode > xcode) { ycode } else { xcode }
        dcode match {
          case c if c == INT => xn.intValue == yn.intValue
          case c if c == LONG => xn.longValue == yn.longValue
          case c if c == FLOAT => xn.floatValue == yn.floatValue
          case c if c == DOUBLE => xn.doubleValue == yn.doubleValue
          case _ => {
            if (yn.isInstanceOf[ScalaNumber] && (!xn.isInstanceOf[ScalaNumber])) {
              yn.equals(xn)
            } else if (xn eq null) {
              yn eq null
            } else {
              xn.equals(yn)
            }
          }
        }
      }
      def equalsCharObject(xc: java.lang.Character, y: Object): Boolean = {
        y match {
          case yc:java.lang.Character => xc.charValue == yc.charValue
          case yn:java.lang.Number => equalsNumChar(yn, xc)
          case _ => {
            if (xc eq null)
              y eq null
            else
              xc.equals(y)
          }
        }
      }
      private def equalsNumChar(xn: java.lang.Number, yc: java.lang.Character): Boolean = {
        import Codes._
        val ch = yc.charValue
        eqTypeCode(xn) match {
          case c if c == INT => xn.intValue == ch
          case c if c == LONG => xn.longValue == ch
          case c if c == FLOAT => xn.floatValue == ch
          case c if c == DOUBLE => xn.doubleValue == ch
          case _ => {
            if (xn eq null)
              yc eq null
            else
              xn.equals(yc)
          }
        }
      }

      def hashFromLong(n: java.lang.Long): Int = {
        val iv = n.intValue
        if (iv == n.longValue) iv
        else n.hashCode()
      }
      def hashFromDouble(n: java.lang.Double): Int = {
        val iv = n.intValue
        val dv = n.doubleValue
        val lv = n.longValue

        if (iv == dv) iv
        else if (lv == dv) java.lang.Long.valueOf(lv).hashCode()
        else n.hashCode()
      }
      def hashFromFloat(n: java.lang.Float): Int = {
        val iv = n.intValue
        val fv = n.floatValue
        val lv = n.longValue
        if (iv == fv) iv
        else if (lv == fv) java.lang.Long.valueOf(lv).hashCode()
        else n.hashCode()
      }
      def hashFromNumber(n: java.lang.Number): Int = {
        n match {
          case l:java.lang.Long => hashFromLong(l)
          case d:java.lang.Double => hashFromDouble(d)
          case f:java.lang.Float => hashFromFloat(f)
          case n => n.hashCode()
        }
      }
      def hashFromObject(a: Object): Int = {
        a match {
          case n:java.lang.Number => hashFromNumber(n)
          case a => a.hashCode()
        }
      }
    }
  }
  //trait Serializable
  /*
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
  package runtime {
    class ObjectRef(var elem: Object) extends Object with java.io.Serializable {
      //override def toString() = "" + elem
    }
    class BooleanRef(var elem: scala.Boolean) extends Object with java.io.Serializable {
      //override def toString() = "" + elem
    }
    class ByteRef(var elem: scala.Byte) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Byte.toString(elem)
    }
    class CharRef(var elem: scala.Char) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Character.toString(elem)
    }
    class DoubleRef(var elem: scala.Double) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Double.toString(elem)
    }
    class FloatRef(var elem: scala.Float) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Float.toString(elem)
    }
    class IntRef(var elem: scala.Int) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Integer.toString(elem)
    }
    class LongRef(var elem: scala.Long) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Long.toString(elem)
    }
    class ShortRef(var elem: scala.Short) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Short.toString(elem)
    }
    class VolatileObjectRef(var elem: Object) extends Object with java.io.Serializable {
      //override def toString() = "" + elem
    }
    class VolatileBooleanRef(var elem: scala.Boolean) extends Object with java.io.Serializable {
      //override def toString() = "" + elem
    }
    class VolatileByteRef(var elem: scala.Byte) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Byte.toString(elem)
    }
    class VolatileCharRef(var elem: scala.Char) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Character.toString(elem)
    }
    class VolatileDoubleRef(var elem: scala.Double) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Double.toString(elem)
    }
    class VolatileFloatRef(var elem: scala.Float) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Float.toString(elem)
    }
    class VolatileIntRef(var elem: scala.Int) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Integer.toString(elem)
    }
    class VolatileLongRef(var elem: scala.Long) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Long.toString(elem)
    }
    class VolatileShortRef(var elem: scala.Short) extends Object with java.io.Serializable {
      //override def toString() = java.lang.Short.toString(elem)
    }
  }
}
