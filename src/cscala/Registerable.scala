package cscala

trait Registerable[T] {
  def register(name: String, v: T, ttl: Long): Boolean
}