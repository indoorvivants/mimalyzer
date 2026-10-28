package test

object Greeter {
  def greet(name: String, greeting: String = "Hello"): String =
    s"$greeting, $name"
}
