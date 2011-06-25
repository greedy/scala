package scala

object Predef extends LowPriorityImplicits {
  def classOf[T]: Class[T] = null
  type String        = java.lang.String
  type Class[T]      = java.lang.Class[T]
  def assert(assertion: Boolean) {
    if (!assertion)
      throw new java.lang.AssertionError("assertion failed")
  }
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
}
