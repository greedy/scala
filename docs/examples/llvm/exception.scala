object exception {
  def main() {
    try {
      throw new Exception
    } catch {
      case e => ()
    }
  }
}

// vim: set ts=4 sw=4 et:
