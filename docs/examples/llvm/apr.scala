object apr {
  import scala.ffi._
  initialize()
  /* library initialization and termination */
  type status_t = CInt
  def x[T] = error("foreign")
  @foreign("apr_initialize") def initialize(): status_t = x

  class APRException(s: status_t) extends Exception(s.toString)

  @foreign("apr_pool_create_ex") def _apr_pool_create_ex(newpool:CIntPtr, parent:CIntPtr, abortfn:CIntPtr, allocator:CIntPtr): status_t = x
  final class pool_t
  lazy val rootpool = alloc.alloca { pptr: Ptr[Ptr[pool_t]] => 
    val stat = _apr_pool_create_ex(Ptr.unwrap(pptr), Ptr.unwrap(Ptr.nullPtr), 0, 0)
    if (stat != 0) throw new APRException(stat.toInt)
    else pptr.peek()
  }

  /* File I/O */
  @foreign("apr_file_open_stdout") private def _apr_file_open_stdout(thefile:CIntPtr, pool:CIntPtr): status_t = x
  @foreign("apr_file_putc") private def _apr_file_putc(ch:CChar, thefile:CIntPtr): status_t = x
  object file_t {
    lazy val stdout = {
      alloc.alloca { pptr: Ptr[Ptr[file_t]] =>
        val stat = _apr_file_open_stdout(Ptr.unwrap(pptr), Ptr.unwrap(rootpool))
        if (stat != 0) throw new APRException(stat.toInt)
        else new file_t(pptr.peek())
      }
    }
  }
  class file_t(val self: Ptr[file_t]) {
    def putc(c: Byte): Unit = {
      val stat = _apr_file_putc(c:CChar, Ptr.unwrap(self))
      if (stat != 0) throw new APRException(stat.toInt)
      else ()
    }
  }
}
