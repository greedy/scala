object inner {

  class Foo {
    var z = 8
    val q = new {
      def frob {
        System.out.println(z)
        z += 1
      }
    }
  }

  def main(args: Array[String]) {
    val f = new Foo
    f.q.frob
    f.q.frob
    f.q.frob
    System.out.println(f.z)
  }


}
