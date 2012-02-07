object inner {

  class Foo {
    var z = 8
    class Bar {
      def frob {
        System.out.println(z)
        z += 1
      }
    }
    val q = new Bar
  }

  def main(args: Array[String]) {
    val f = new Foo
    f.q.frob
    f.q.frob
    f.q.frob
    System.out.println(f.z)
  }


}
