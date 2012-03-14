package scala.ffi

object alloc {
  private def x[T] = error("foreign")
  @foreign("malloc") private def _malloc(n: CSizeT): CIntPtr = x
  @foreign("free") private def _free(p: CIntPtr): Unit = x
  def malloc[T:Storable]: Ptr[T] = { mallocBytes(Storable.get[Ptr[T]].size) }
  def mallocBytes[T](n: CSizeT): Ptr[T] = Ptr.wrap(_malloc(n))
  def free(p: Ptr[_]) = _free(Ptr.unwrap(p))
  def alloca[A:Storable,B](f: Ptr[A] => B): B = {
    val ptr: Ptr[A] = malloc
    if (ptr.isNull) throw new Exception()
    else try { f(ptr) } finally { free(ptr) }
  }
  def allocaBytes[A,B](n: CSizeT)(f: Ptr[A] => B): B = {
    val ptr: Ptr[A] = mallocBytes(n)
    if (ptr.isNull) throw new Exception()
    else try { f(ptr) } finally { free(ptr) }
  }
}
