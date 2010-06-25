class example {
  val s: Short = 1
  val i: Int = 2
  val l: Long = 3
  def x: Double = s+i+l
  def z = x*2
}
object example extends example {
  val f: Float = 4
  val d: Double = 5
  val b: Byte = 6
  val c: Char = 7
  override def x: Double = f+d+b
  def main() {
    printdouble(s+i+l)
    printdouble(x)
    printdouble(z)
  }

  @native def printdouble(d: Double): Unit = ()
}
