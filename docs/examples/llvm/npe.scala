
object npe {
    def main(args: Array[String]) {
        try {
            (null:npe.type).main(null)
        } catch {
            case _ => System.out.println("caught")
        }
    }
}
