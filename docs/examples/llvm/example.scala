object example {
  val s: Short = 1
  val i: Int = 2
  val l: Long = 3
  val f: Float = 4
  val d: Double = 5
  val b: Byte = 6
  val c: Char = 7
  def main() {
    printdouble((s+i)/l+f*d-b+c)
  }

  @native def printdouble(d: Double): Unit = ()
}
