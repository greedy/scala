package scala.ffi

object Storable extends ctypes.StorableCtypes
{
  def get[T:Storable] = implicitly[Storable[T]]
}

trait Storable[T] {
  def size: Int
  def alignment: Int
  def peekElemOff(base: Ptr[T], n: Int): T = peek(base plus (n * size))
  def pokeElemOff(base: Ptr[T], n: Int, x: T): Unit = poke(base plus (n * size), x)
  def peekByteOff(base: Ptr[_], n: Int): T = peek((base plus n).cast[T])
  def pokeByteOff(base: Ptr[_], n: Int, x: T): Unit = poke((base plus n).cast[T], x)
  def peek(ptr: Ptr[T]): T
  def poke(ptr: Ptr[T], x: T): Unit
}
