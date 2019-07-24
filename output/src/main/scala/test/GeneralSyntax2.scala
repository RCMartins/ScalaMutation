package test

object GeneralSyntax2 {

  def str1: String = (if (sys.props.contains("SCALA_MUTATION_1")) 1 - 2 else 1 + 2) + ""

  def str2: String = (if (sys.props.contains("SCALA_MUTATION_2")) 1 - 2 else 1 + 2).toString

  val bool1 = !(if (sys.props.contains("SCALA_MUTATION_3")) false else true)

  def functionWithBlock: Boolean = {
    val bool = if (sys.props.contains("SCALA_MUTATION_4")) false else true
    def fun(param: Int = if (sys.props.contains("SCALA_MUTATION_5")) 5 - 3 else 5 + 3): Int = if (sys.props.contains("SCALA_MUTATION_6")) param - 1 else param + 1
    !bool
  }

  val if1: Int = if (if (sys.props.contains("SCALA_MUTATION_7")) false else true) if (sys.props.contains("SCALA_MUTATION_8")) 1 - 1 else 1 + 1 else if (sys.props.contains("SCALA_MUTATION_9")) 6 * 2 else 6 / 2

}
