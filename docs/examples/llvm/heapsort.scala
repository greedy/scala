/* The Computer Language Shootout 
   http://shootout.alioth.debian.org/
   contributed by Isaac Gouy (Scala novice)
*/

object heapsort {
  val MAX = 50

   def main(args: Array[String]) = {
     // For some reason, this heapsort was coded with
     // a 1-based array.
     val numbers = new Array[Double](MAX+1);
     var i = 1
     while(i <= MAX) {
       numbers(i) = generate(100.0)
       i += 1
     }

     heapsort(MAX, numbers);
     i = 1
     while(i <= MAX) {
       System.out.println(numbers(i))
       i += 1
     }
   }


   def heapsort(n: Int, ra: Array[Double]): Unit = {
      var l = 0; var j = 0; var ir = 0; var i = 0; 
      var rra = 0.0d;

      if (n < 2) return;
      l = (n >> 1) + 1;
      ir = n;
      while (true) {
         if (l > 1) { l = l-1; rra = ra(l); }
         else {
            rra = ra(ir);
            ra(ir) = ra(1);
            ir = ir-1;
            if (ir == 1) {
               ra(1) = rra;
               return;
            }
         }
         i = l;
         j = l << 1;
         while (j <= ir) {
            if (j < ir && ra(j) < ra(j+1)) { j = j+1; }
            if (rra < ra(j)) {
               ra(i) = ra(j);
               i = j;
               j = j + i;
            } 
            else j = ir + 1;
         }
         ra(i) = rra;
      }
   }


   private val IM = 139968;
   private val IA = 3877;
   private val IC = 29573;
   private var seed = 42;

   private def generate(max: Double) = {
      seed = (seed * IA + IC) % IM;
      max * seed / IM;
   }

}
