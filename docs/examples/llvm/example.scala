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

  def sayhello() = System.out.println("hello"+who)
  def check(s: String) = System.out.println("is who "+s+" "+(who == s))
}

class E extends java.lang.Exception

object example extends examplec with Magic {
  val f: Float = 4
  val d: Double = 5
  val b: Byte = 6
  val c: Char = 7.toChar
  val who = "world"
  override def x: Double = f+d+b
  def main() {
    System.out.println("Saying hello")
    sayhello()
    System.out.println("Testing string equality, who is " + who)
    check("world")
    check("nurse")
    arraytests()
    System.out.println("Testing inherited values, s+i+l, should be 6")
    printdouble(s+i+l)
    System.out.println("Testing overridden def, x, should be 15")
    printdouble(x)
    System.out.println("Testing inherited def that called overriden def, z, should be 30")
    printdouble(z)
    System.out.println("Testing loops, should print numbers 0 to 9")
    var q = 0
    while (q < 10) {
      printdouble(q)
      q = q + 1
    }
    System.out.println("Testing trait method shazam, should be 30")
    printdouble(this.shazam)
    System.out.println("Testing abstract trait method xyzzy, should be 15")
    printdouble(this.xyzzy)
    System.out.println("Testing passing as trait, should print 45, 30, 15, 45")
    printdouble(cast(this))
    printdouble(toMagic(this).shazam)
    printdouble(toMagic(this).xyzzy)
    printdouble(cast(toMagic(this)))
    System.out.println("Testing calling function with exception handler that won't throw, should print 5 and 'In finally block'")
    try {
      printdouble(whee(0))
    } catch {
      case e: Exception =>System.out.println("Caught exception")
    } finally {
      System.out.println("In finally block")
    }
    System.out.println("Testing calling function with exception handler that will throw, should print 'Caught exception'")
    try {
      printdouble(whee(4))
    } catch {
      case e: Exception =>System.out.println("Caught exception")
    }
    System.out.println("Testing throw and finally, should print 'Caught exception' followed by 'In finally'")
    try {
      try {
        throw new Exception
        printdouble(0)
      } catch {
        case e: E =>System.out.println("Caught E")
        case e: Exception =>System.out.println("Caught exception")
      } finally {
        System.out.println("In finally")
      }
    } catch {
      case e: Exception =>System.out.println("Caught exception, but shouldn't have here")
    }
    aprtest()
    gctest()
  }
  def main(args: Array[String]) { main() }

  def aprtest() {
  /*
    apr.file_t.stdout.putc('a':Byte)
    apr.file_t.stdout.putc('p':Byte)
    apr.file_t.stdout.putc('r':Byte)
    apr.file_t.stdout.putc('\n':Byte)
    */
  }

  def gctest() {
    var x = 1E6;
    var a: String = ""
    var b: String = ""
    var c: String = ""
    while (x > 0) {
      c = b
      b = a
      a = x.toString
      x -= 1
    }
    System.out.println("a,b,c")
    System.out.println(a)
    System.out.println(b)
    System.out.println(c)
  }

  def toMagic(m: Magic) = m

  //@native def printdouble(d: Double): Unit = ()
  def printdouble(d: Double): Unit = { System.out.println(java.lang.Double.toString(d)) }

  def xyzzy = x


  def arraytests() {
    System.out.println("Testing array stuff")
    System.out.println("Creating 5 element double array")
    val darray1 = new Array[Double](5)
    var x = 0
    System.out.println("Loading array with index*2")
    while (x < 5) {
      darray1(x) = x*2
      x = x + 1
    }
    x = 0
    System.out.println("Printing array, should be 0 2 4 6 8")
    while (x < 5) {
      printdouble(darray1(x))
      x = x + 1
    }
  }
}
