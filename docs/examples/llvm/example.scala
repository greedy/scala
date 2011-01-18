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

  def nullchk(x: Object) = Predef.assert(x != null)

  def sayhello() = Console.println("hello"+who)
  def check(s: String) = Console.println("is who "+s+" "+(who == s))
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
    Console.println("Saying hello")
    sayhello()
    Console.println("Testing string equality, who is " + who)
    check("world")
    check("nurse")
    arraytests()
    Console.println("Testing inherited values, s+i+l, should be 6")
    printdouble(s+i+l)
    Console.println("Testing overridden def, x, should be 15")
    printdouble(x)
    Console.println("Testing inherited def that called overriden def, z, should be 30")
    printdouble(z)
    Console.println("Testing loops, should print numbers 0 to 9")
    var q = 0
    while (q < 10) {
      printdouble(q)
      q = q + 1
    }
    Console.println("Testing trait method shazam, should be 30")
    printdouble(this.shazam)
    Console.println("Testing abstract trait method xyzzy, should be 15")
    printdouble(this.xyzzy)
    Console.println("Testing passing as trait, should print 45, 30, 15, 45")
    printdouble(cast(this))
    printdouble(toMagic(this).shazam)
    printdouble(toMagic(this).xyzzy)
    printdouble(cast(toMagic(this)))
    Console.println("Testing calling function with exception handler that won't throw, should print 5 and 'In finally block'")
    try {
      printdouble(whee(0))
    } catch {
      case e: Exception => Console.println("Caught exception")
    } finally {
      Console.println("In finally block")
    }
    Console.println("Testing calling function with exception handler that will throw, should print 'Caught exception'")
    try {
      printdouble(whee(4))
    } catch {
      case e: Exception => Console.println("Caught exception")
    }
    Console.println("Testing throw and finally, should print 'Caught exception' followed by 'In finally'")
    try {
      try {
        throw new Exception
        printdouble(0)
      } catch {
        case e: E => Console.println("Caught E")
        case e: Exception => Console.println("Caught exception")
      } finally {
        Console.println("In finally")
      }
    } catch {
      case e: Exception => Console.println("Caught exception, but shouldn't have here")
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
    Console.println("Testing array stuff")
    Console.println("Creating 5 element double array")
    val darray1 = new Array[Double](5)
    var x = 0
    Console.println("Loading array with index*2")
    while (x < 5) {
      darray1(x) = x*2
      x = x + 1
    }
    x = 0
    Console.println("Printing array, should be 0 2 4 6 8")
    while (x < 5) {
      printdouble(darray1(x))
      x = x + 1
    }
  }
}
