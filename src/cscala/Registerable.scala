package cscala

trait Registerable[T] {
  def register[Req,Resp](name: String, v: T, ttl: Long): Boolean
}