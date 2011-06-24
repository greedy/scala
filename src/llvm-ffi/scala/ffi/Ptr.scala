package scala.ffi

object Ptr {
  def nullPtr[T]: Ptr[T] = new Ptr[T](0)
  def wrap[T](addr: CIntPtr): Ptr[T] = new Ptr[T](addr)
  def unwrap(ptr: Ptr[_]): CIntPtr = ptr.address
  /*
  implicit object StorablePtr extends Storable[Ptr[_]] {
    def size = implicitly[Storable[CIntPtr]].size
    def alignment = implicitly[Storable[CIntPtr]].alignment
    def peek(ptr: Ptr[Ptr[T]]): Ptr[T] = wrap(ptr.cast[CIntPtr].peek())
    def poke(ptr: Ptr[Ptr[T]], x: Ptr[T]): Unit = ptr.cast[CIntPtr].poke(unwrap(x))
  }
  */
  private class StorablePtr[T] extends Storable[Ptr[T]] {
    def size = implicitly[Storable[CIntPtr]].size
    def alignment = implicitly[Storable[CIntPtr]].alignment
    def peek(ptr: Ptr[Ptr[T]]): Ptr[T] = wrap(ptr.cast[CIntPtr].peek())
    def poke(ptr: Ptr[Ptr[T]], x: Ptr[T]): Unit = ptr.cast[CIntPtr].poke(unwrap(x))
  }
  implicit def ptrStorable[T]: Storable[Ptr[T]] = new StorablePtr[T]
}

class Ptr[T] private[ffi] (private[ffi] val address: CIntPtr) {
  def cast[U]: Ptr[U] = new Ptr[U](address)
  def plus(n: Int) = new Ptr[T](address + n)
  def align(n: Int) = new Ptr[T](address + (address % n))
  def minus(y: Ptr[_]) = address - y.address
  def peek()(implicit st: Storable[T]): T = st.peek(this)
  def poke(x: T)(implicit st: Storable[T]): Unit = st.poke(this, x)
  def peekElemOff(n: Int)(implicit st: Storable[T]): T = st.peekElemOff(this, n)
  def pokeElemOff(n: Int, x: T)(implicit st: Storable[T]): Unit = st.pokeElemOff(this, n, x)
  def peekByteOff[S:Storable](n: Int) = implicitly[Storable[S]].peekByteOff(this, n)
  def pokeByteOff[S:Storable](n: Int, x: S) = implicitly[Storable[S]].pokeByteOff(this, n, x)
  def isNull = address == 0
}
