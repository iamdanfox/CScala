package cscala
import ox.cso.Connection.Server

trait Connectable {
  def connect[Req,Resp](name: String): Option[Server[Req,Resp]];
}