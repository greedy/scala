class examplec {
  val s: Short = 1
  val i: Int = 2
  val l: Long = 3
  def x: Double = s+i+l
  def z = x*2
}
object example extends examplec {
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
  }
  def main(args: Array[String]) { main() }

  @native def printdouble(d: Double): Unit = ()
  //def printdouble(d: Double): Unit = { Console.printf("%g\n", d) }
}
