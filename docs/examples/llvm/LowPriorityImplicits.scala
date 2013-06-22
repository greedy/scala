package scala

import collection.mutable
import mutable.WrappedArray
class LowPriorityImplicits {
  // Since the JVM thinks arrays are covariant, one 0-length Array[AnyRef]
  // is as good as another for all T <: AnyRef.  Instead of creating 100,000,000
  // unique ones by way of this implicit, let's share one.
  implicit def wrapRefArray[T <: AnyRef](xs: Array[T]): WrappedArray[T] = {
    if (xs eq null) null
    else if (xs.length == 0) WrappedArray.empty[T]
    else new WrappedArray.ofRef[T](xs)
  }

  implicit def wrapIntArray(xs: Array[Int]): WrappedArray[Int] = if (xs ne null) new WrappedArray.ofInt(xs) else null
  implicit def wrapDoubleArray(xs: Array[Double]): WrappedArray[Double] = if (xs ne null) new WrappedArray.ofDouble(xs) else null
  implicit def wrapLongArray(xs: Array[Long]): WrappedArray[Long] = if (xs ne null) new WrappedArray.ofLong(xs) else null
  implicit def wrapFloatArray(xs: Array[Float]): WrappedArray[Float] = if (xs ne null) new WrappedArray.ofFloat(xs) else null
  implicit def wrapCharArray(xs: Array[Char]): WrappedArray[Char] = if (xs ne null) new WrappedArray.ofChar(xs) else null
  implicit def wrapByteArray(xs: Array[Byte]): WrappedArray[Byte] = if (xs ne null) new WrappedArray.ofByte(xs) else null
  implicit def wrapShortArray(xs: Array[Short]): WrappedArray[Short] = if (xs ne null) new WrappedArray.ofShort(xs) else null
  implicit def wrapBooleanArray(xs: Array[Boolean]): WrappedArray[Boolean] = if (xs ne null) new WrappedArray.ofBoolean(xs) else null
  implicit def wrapUnitArray(xs: Array[Unit]): WrappedArray[Unit] = if (xs ne null) new WrappedArray.ofUnit(xs) else null
  implicit def intWrapper(x: Int)         = new runtime.RichInt(x)
  implicit def longWrapper(x: Int)         = new runtime.RichLong(x)
  /*
  implicit def wrapString(s: String): WrappedString = if (s ne null) new WrappedString(s) else null
   */
  implicit def genericWrapArray[T](xs: Array[T]): WrappedArray[T] =
    if (xs eq null) null
    else WrappedArray.make(xs)
} 
