package scala.ffi

object Storable extends ctypes.StorableCtypes
{
  def get[T:Storable] = implicitly[Storable[T]]
  def size[T:Storable] = get[T].size
  def alignment[T:Storable] = get[T].alignment
  def peekElemOff[T:Storable](base: Ptr[T], n: Int): T = get[T].peekElemOff(base, n)
  def pokeElemOff[T:Storable](base: Ptr[T], n: Int, x: T): Unit = get[T].pokeElemOff(base, n, x)
  def peekByteOff[T:Storable](base: Ptr[_], n: Int): T = get[T].peekByteOff(base, n)
  def pokeByteOff[T:Storable](base: Ptr[_], n: Int, x: T): Unit = get[T].pokeByteOff(base, n, x)
  def peek[T:Storable](ptr: Ptr[T]): T = get[T].peek(ptr)
  def poke[T:Storable](ptr: Ptr[T], x: T): Unit = get[T].poke(ptr, x)
}

trait Storable[T] {
  def size: CSizeT
  def alignment: Int
  def peekElemOff(base: Ptr[T], n: Int): T = peek(base plus (n * size))
  def pokeElemOff(base: Ptr[T], n: Int, x: T): Unit = poke(base plus (n * size), x)
  def peekByteOff(base: Ptr[_], n: Int): T = peek((base plus n).cast[T])
  def pokeByteOff(base: Ptr[_], n: Int, x: T): Unit = poke((base plus n).cast[T], x)
  def peek(ptr: Ptr[T]): T
  def poke(ptr: Ptr[T], x: T): Unit
}
