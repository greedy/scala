object apr {
  import scala.ffi._

  @foreignExport("test_export")
  def exportedFunction(x: CInt, y: Ptr[CInt]): CInt = x * 2
  initialize()
  /* library initialization and termination */
  type status_t = CInt
  def x[T] = error("foreign")
  @foreign("apr_initialize") def initialize(): status_t = x

  class APRException(s: status_t) extends Exception(s.toString)

  trait allocator_t
  type abortfunc_t = Ptr[CInt=>CInt]

  trait pool_t

  @foreign("apr_pool_create_ex") def _apr_pool_create_ex(newpool:Ptr[Ptr[pool_t]], parent:Ptr[pool_t], abortfn:abortfunc_t, allocator:Ptr[allocator_t]): status_t = x

  lazy val rootpool = alloc.alloca { pptr: Ptr[Ptr[pool_t]] => 
    val stat = _apr_pool_create_ex(pptr, Ptr.nullPtr, Ptr.nullPtr, Ptr.nullPtr)
    if (stat != 0) throw new APRException(stat.toInt)
    else pptr.peek()
  }

  /* File I/O */
  @foreign("apr_file_open_stdout") private def _apr_file_open_stdout(thefile:Ptr[Ptr[file_t]], pool:Ptr[pool_t]): status_t = x
  @foreign("apr_file_putc") private def _apr_file_putc(ch:CChar, thefile:Ptr[file_t]): status_t = x
  object file_t {
    lazy val stdout = {
      alloc.alloca { pptr: Ptr[Ptr[file_t]] =>
        val stat = _apr_file_open_stdout(pptr, rootpool)
        if (stat != 0) throw new APRException(stat.toInt)
        else new file_t(pptr.peek())
      }
    }
  }
  class file_t(val self: Ptr[file_t]) {
    def putc(c: Byte): Unit = {
      val stat = _apr_file_putc(c:CChar, self)
      if (stat != 0) throw new APRException(stat.toInt)
      else ()
    }
  }
}
