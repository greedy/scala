package java {
  package lang {
    class Throwable(message: String, cause: Throwable) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
      def getStackTrace(): scala.Array[StackTraceElement] = null
      def getMessage(): String = null
      def printStackTrace(): Unit = ()
    }
    class Exception(message: String, cause: Throwable) extends Throwable(message, cause) {
      def this() = this(null, null)
      def this(message: String) = this(message, null)
      def this(cause: Throwable) = this(null, cause)
    }
    class NullPointerException(message: String) extends Exception(message) {
      def this() = this(null)
    }
    object Boolean {
      val TYPE = null
      def valueOf(b: scala.Boolean) = {
        if (b) TRUE else FALSE
      }
      val TRUE = new Boolean(true)
      val FALSE = new Boolean(false)
    }
    class Boolean(value: scala.Boolean) {
      def booleanValue() = value
      override def toString() = if (value) "true" else "false"
    }
    object Byte {
      val TYPE = null
      def valueOf(i: scala.Byte) = new Byte(i)
      @native def parseByte(s: String): scala.Byte
      val MIN_VALUE: scala.Byte = -128
      val MAX_VALUE: scala.Byte = 127
    }
    class Byte(value: scala.Byte) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
    object Short {
      val TYPE = null
      def valueOf(i: scala.Short) = new Short(i)
      @native def parseShort(s: String): scala.Short
    }
    class Short(value: scala.Short) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
    object Integer {
      val TYPE = null
      def valueOf(i: scala.Int) = new Integer(i)
      def bitCount(i: scala.Int): scala.Int = {
        var x = i;
        var c = 0;
        while (x != 0) {
          c = c + (x & 1);
          x = x >> 1;
        }
        c
      }
      @native def parseInt(s: String): scala.Int
      def reverseBytes(i: scala.Int): scala.Int = {
        val b0 = i & 0xff;
        val b1 = (i >> 8) & 0xff;
        val b2 = (i >> 16) & 0xff;
        val b3 = (i >> 24) & 0xff;
        (b0 << 24) | (b1 << 16) | (b2 << 8) | b3
      }
    }
    class Integer(value: scala.Int) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
    object Character {
      val TYPE = null
      def valueOf(i: scala.Char) = new Character(i)
      def getType(c: scala.Char): scala.Int = 0
      def getType(c: scala.Int): scala.Int = 0
    }
    class Character(value: scala.Char) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
      def charValue(): scala.Char = value
    }
    object Long {
      val TYPE = null
      def valueOf(i: scala.Long) = new Long(i)
      @native def parseLong(s: String): scala.Long
    }
    class Long(value: scala.Long) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
    object Float {
      val TYPE = null
      def valueOf(i: scala.Float) = new Float(i)
      @native def parseFloat(s: String): scala.Float
      def compare(a: scala.Float, b: scala.Float): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
    }
    class Float(value: scala.Float) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
    object Double {
      val TYPE = null
      def valueOf(i: scala.Double) = new Double(i)
      @native def parseDouble(s: String): scala.Double
      def toString(d: scala.Double): String = null
      def compare(a: scala.Double, b: scala.Double): scala.Int = {
        if (a == b) 0
        else if (a < b) -1
        else 1
      }
    }
    class Double(value: scala.Double) {
      def byteValue(): scala.Byte = value.toByte
      def shortValue(): scala.Short = value.toShort
      def intValue(): scala.Int = value.toInt
      def longValue(): scala.Long = value.toLong
      def floatValue(): scala.Float = value.toFloat
      def doubleValue(): scala.Double = value.toDouble
    }
  }
  package io {
    trait Serializable
  }
}

package scala {
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
}
