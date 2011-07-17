object bounds {
  def main(args: Array[String]) {
    val aa = new Array[Int](5)
    boundsCheckOk(aa, 0)
    boundsCheckOk(aa, 1)
    boundsCheckOk(aa, 4)
    boundsCheckExn(aa, 5)
    boundsCheckExn(aa, 6)
    boundsCheckExn(aa, -1)
  }

  def boundsCheckOk(a: Array[Int], i: Int) {
    try {
      System.out.println(a(i))
    }
    catch {
      case _ =>
        System.out.println("ERROR: caught exception on valid array bounds check.")
    }
  }

  def boundsCheckExn(a: Array[Int], i: Int) {
    var exn = false
    try {
      System.out.println(a(i))
    }
    catch {
      case e : ArrayIndexOutOfBoundsException =>
        System.out.println("GOOD: Caught array bounds exception, as expected.")
        exn = true
      case e: Exception =>
        System.out.println("ERROR: Caught some OTHER exception: " + e)
        exn = true
    }
    finally {
      if(!exn)
        System.out.println("ERROR: No array bounds exception on invalid array access.")
    }
  }
}
