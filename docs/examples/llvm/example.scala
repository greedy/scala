trait Magic {
  def xyzzy: Double
}
class examplec {
  val s: Short = 1
  val i: Int = 2
  val l: Long = 3
  def x: Double = s+i+l
  def z = x*2

  def cast(x: Magic) = x.xyzzy
}
@llvmdefs("""
declare default ccc i32 @printf(i8*, ...)
@fmt = internal constant [4 x i8] c"%g\0A\00"
""")
object example extends examplec with Magic {
  val f: Float = 4
  val d: Double = 5
  val b: Byte = 6
  val c: Char = 7
  override def x: Double = f+d+b
  def main() {
    printdouble(s+i+l)
    printdouble(x)
    printdouble(z)
    var q = 0
    while (q < 1000) {
      printdouble(q)
      q = q + 1
    }
    printdouble(cast(this))
  }
  def main(args: Array[String]) { main() }

  //@native def printdouble(d: Double): Unit = ()
  @llvmimpl("""
  %fs = getelementptr [4 x i8]* @fmt, i32 0, i32 0
  %1 = call ccc i32 (i8*,...)* @printf(i8* %fs, double %d)
  ret void
""")
  def printdouble(d: Double): Unit = { Console.printf("%g\n", d) }

  def xyzzy = x
}
