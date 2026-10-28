package test

trait Foo {
  private val one: Int = 1

  def bar(x: Int) = x + one
}
