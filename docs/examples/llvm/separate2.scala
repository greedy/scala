object separate2 {
  def main(args: Array[String]) {
    separate1.go
    System.out.println(separate1.answer)
    System.out.println((new separate1).q)
  }
}
