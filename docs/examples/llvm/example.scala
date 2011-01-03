trait Magic {
  def xyzzy: Double
  def shazam = 2 * xyzzy
  def whee(z: Int) = if (z == 0) 5 else throw new Exception
}
abstract class examplec {
  val s: Short = 1
  val i: Int = 2
  val l: Long = 3
  def who: String
  def x: Double = s+i+l
  def z = x*2

  def cast(x: Magic) = x.xyzzy + x.shazam

  def sayhello() = String.print("hello"+who)
  def check(s: String) = String.print("is who "+s+" "+(who == s))
}

class E extends java.lang.Exception

@llvmdefs("""
declare default ccc i32 @printf(i8*, ...)
@fmt = internal constant [4 x i8] c"%g\0A\00"
""")
object example extends examplec with Magic {
  val f: Float = 4
  val d: Double = 5
  val b: Byte = 6
  val c: Char = 7
  val who = "world"
  override def x: Double = f+d+b
  def main() {
    sayhello()
    check("world")
    check("nurse")
    arraytests()
    printdouble(0)
    printdouble(0)
    printdouble(0)
    printdouble(0)
    printdouble(s+i+l)
    printdouble(x)
    printdouble(z)
    var q = 0
    while (q < 10) {
      printdouble(q)
      q = q + 1
    }
    printdouble(-9)
    printdouble(this.shazam)
    printdouble(this.xyzzy)
    printdouble(cast(this))
    printdouble(-8)
    printdouble(toMagic(this).shazam)
    printdouble(toMagic(this).xyzzy)
    printdouble(cast(toMagic(this)))
    try {
      printdouble(whee(0))
    } catch {
      case e: Exception => printdouble(-10)
    }
    try {
      printdouble(whee(4))
    } catch {
      case e: Exception => printdouble(-12)
    }
    printdouble(cast(this))
    try {
      try {
        throw new Exception
        printdouble(0)
      } catch {
        case e: E => { printdouble(-1) }
        case e: Exception => { printdouble(-3) }
      } finally {
        printdouble(-2)
      }
    } catch {
      case e: Exception => { printdouble(-4) }
    }
  }
  def main(args: Array[String]) { main() }

  def toMagic(m: Magic) = m

  //@native def printdouble(d: Double): Unit = ()
  @llvmimpl("""
  %fs = getelementptr [4 x i8]* @fmt, i32 0, i32 0
  %1 = call ccc i32 (i8*,...)* @printf(i8* %fs, double %d)
  ret void
""")
  def printdouble(d: Double): Unit = { Console.printf("%g\n", d) }

  def xyzzy = x


  def arraytests() {
    val darray1 = new Array[Double](5)
    var x = 0
    while (x < 5) {
      darray1(x) = x*2
      x = x + 1
    }
    x = 0
    while (x < 5) {
      printdouble(darray1(x))
      x = x + 1
    }
  }
}
