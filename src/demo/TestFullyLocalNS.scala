package demo

import cscala._
import java.net._
import ox.cso.Components

object TestLocal {

  def main(args: Array[String]): Unit = {
      val ns = new FullyLocalNS()
      ns.register[String,Int]("lengthcalc", NameServer.DEFAULT_TTL, ((client) => {
        val str = client?;
        client!(str.length)
      }))
      
      val lc = ns.connect2[String,Int]("lengthcalc")
      lc!"Hello"
      val ret = lc?;
      println("1: "+wrap(ret==5))
      
      ns.terminate()
      println("done")
  }
  def wrap(b:Boolean) : String =  if (b) "pass" else "fail"
}