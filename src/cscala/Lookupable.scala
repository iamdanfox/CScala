package cscala

trait Lookupable[T] {
  def lookup(name: String): Option[T];
}