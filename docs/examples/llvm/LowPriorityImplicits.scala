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
} 
