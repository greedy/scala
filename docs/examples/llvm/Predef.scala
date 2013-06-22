package scala

object Predef extends LowPriorityImplicits {
  def classOf[T]: Class[T] = null
  type String        = java.lang.String
  type Class[T]      = java.lang.Class[T]
  def identity[A](x: A): A         = x    // @see `conforms` for the implicit version
  def implicitly[T](implicit e: T) = e    // for summoning implicit values from the nether world
  def locally[T](x: T): T  = x    // to communicate intent and avoid unmoored statements
  // Deprecated

  def error(message: String): Nothing = sys.error(message)
  sealed abstract class <:<[-From, +To] extends (From => To)
  implicit def conforms[A]: A <:< A = new (A <:< A) { def apply(x: A) = x }
  /** A type for which there is always an implicit value.
   *  @see fallbackCanBuildFrom in Array.scala
   */
  class DummyImplicit
  
  object DummyImplicit {
  
    /** An implicit value yielding a DummyImplicit.
     *   @see fallbackCanBuildFrom in Array.scala
     */
    implicit def dummyImplicit: DummyImplicit = new DummyImplicit
  }
  type ClassManifest[T] = scala.reflect.ClassManifest[T]
  val AnyRef      = new SpecializableCompanion {}   // a dummy used by the specialization annotation
  def print(s: String) = java.lang.System.out.print(s)
  def println(s: String) = java.lang.System.out.println(s)

  /** Tests an expression, throwing an `IllegalArgumentException` if false.
   *  This method is similar to `assert`, but blames the caller of the method
   *  for violating the condition.
   *
   *  @param p   the expression to test
   */
  def require(requirement: Boolean) {
    if (!requirement)
      throw new IllegalArgumentException("requirement failed")
  }

  /** Tests an expression, throwing an `IllegalArgumentException` if false.
   *  This method is similar to `assert`, but blames the caller of the method
   *  for violating the condition.
   *
   *  @param p   the expression to test
   *  @param msg a String to include in the failure message
   */
  @inline final def require(requirement: Boolean, message: => Any) {
    if (!requirement)
      throw new IllegalArgumentException("requirement failed: "+ message)
  }

  /** Tests an expression, throwing an `AssertionError` if false.
   *  Calls to this method will not be generated if `-Xelide-below`
   *  is at least `ASSERTION`.
   *
   *  @see elidable
   *  @param p   the expression to test
   */
  def assert(assertion: Boolean) {
    if (!assertion)
      throw new java.lang.AssertionError("assertion failed")
  }

  /** Tests an expression, throwing an `AssertionError` if false.
   *  Calls to this method will not be generated if `-Xelide-below`
   *  is at least `ASSERTION`.
   *
   *  @see elidable
   *  @param p   the expression to test
   *  @param msg a String to include in the failure message
   */
  final def assert(assertion: Boolean, message: => Any) {
    if (!assertion)
      throw new java.lang.AssertionError("assertion failed: "+ message)
  }

  /** Tests an expression, throwing an `AssertionError` if false.
   *  This method differs from assert only in the intent expressed:
   *  assert contains a predicate which needs to be proven, while
   *  assume contains an axiom for a static checker.  Calls to this method
   *  will not be generated if `-Xelide-below` is at least `ASSERTION`.
   *
   *  @see elidable
   *  @param p   the expression to test
   */
  def assume(assumption: Boolean) {
    if (!assumption)
      throw new java.lang.AssertionError("assumption failed")
  }

  /** Tests an expression, throwing an `AssertionError` if false.
   *  This method differs from assert only in the intent expressed:
   *  assert contains a predicate which needs to be proven, while
   *  assume contains an axiom for a static checker.  Calls to this method
   *  will not be generated if `-Xelide-below` is at least `ASSERTION`.
   *
   *  @see elidable
   *  @param p   the expression to test
   *  @param msg a String to include in the failure message
   */
  final def assume(assumption: Boolean, message: => Any) {
    if (!assumption)
      throw new java.lang.AssertionError("assumption failed: "+ message)
  }

  implicit def any2stringadd(x: Any) = new runtime.StringAdd(x)
}
