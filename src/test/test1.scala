package test
import ox.CSO._
import java.net._
import ox.cso.ObjectIO

object test1 {

  def main(args: Array[String]): Unit = {

//    println("Starting...");
//    val clients = new OneOne[NetIO.Client[Int, Int]]
//    val myServer = NetIO.serverPort[Int, Int](3301, clients);
//    val myClient = ox.cso.NetIO.clientConnection[Int, Int](InetAddress.getByName("127.0.0.1"), 3301, false)
//    myClient!(1);
//    println(clients?)
    
    // code I would like to work:
    
//    val listener:InPort[String] = XCSO.netListener[String](3301);  // we can now listen to values by calling listener?
//    
//    
//    val foreignServer =  XCSO.netChan("127.0.0.1",3301) 
//    foreignServer ! "Hello"  // sends the "Hello" message tosome foreign server,  
//    
//    
//    println(listener?) // receives "Hello"
//    
    
    
  }
  type Serial = ox.cso.ObjectIO.Serial

  def NetListener[T <: Serial](port: Int): ?[T] = {
    val socket = new ServerSocket(port, 0)

    val receiverChan = OneOne[T]

    proc("NetListener: " + socket) {
      val client = socket.accept
      client.setTcpNoDelay(true) // enables/disables nagle's algorithm

      val in = client.getInputStream
      //val out = client.getOutputStream

      ObjectIO.StreamToPort(in, receiverChan).withName("NetListener").fork
      //        handle(new Client(client, req, rep))
    }

    return receiverChan;
  }

  class NetListener[T <: Serial](port: Int) extends InPort[T] {
    override def ?(): T = {
      return null.asInstanceOf[T];
    }

    /**
     * Block until a value <tt>t</tt> is available for
     * reading, then return the result of applying <tt>f</tt> to
     * it; synchronisation with the sender is at the end of
     * the computation of <tt>f(t)</tt> (this is sometimes
     * called an <i>extended rendezvous</i>)
     */
    override def ?[U](f: T => U): U = {
      return null.asInstanceOf[U];
    }

    /** Synonym for <code>?()</code> */
    override def read(): T = ?

    /** Signal that no further values will ever be transmitted via the channel */
    override def close: Unit = {

    }

    /** Signal that no further values will ever be read from the channel */
    override def closein: Unit = close

    /**
     * Return true iff a value is available now for reading.
     * If no value is available and <tt>whenReadable</tt> is non-null
     * then it will be invoked when next a value is available for
     * reading. (Used only by the implementation of <code>Alt</code>.)
     */
    // private [cso] def isReadable ( whenReadable: () => Unit ) : Boolean

    //  protected var _isOpen   = true
    /** Return false iff a read() could never again succeed */
    override def open: Boolean = synchronized { _isOpen }
    /** Return false iff a read() could never again succeed */
    override def isOpen(): Boolean = synchronized { _isOpen }

    /** Return false iff a read() could never again succeed */
    private def isPortOpen() = open

    /**
     * Return an unconditional event, for use in an <tt>Alt</tt>.
     * If the event is fired by an <code>Alt</code>, the given
     * command is invoked; it <i>must</i> read from this inport.
     * (Syntactic sugar for <tt>-?-&gt;</tt>)
     */
    //  def --> (cmd: => Unit)    = {return null.asInstanceOf[InPort.InPortEvent[T]];}

    /**
     * Return an unconditional event, for use in an <tt>Alt</tt>.
     * If the event is fired by an <code>Alt</code>, the given
     * command is invoked; it <i>must</i> read from this inport.
     */
    //  def -?-> (cmd: => Unit)    = new InPort.InPortEvent[T](this, ()=>cmd, isOpen)

    /**
     * Return an unconditional event, for use in an <tt>Alt</tt>.
     * If the event is fired by an <code>Alt</code>, the given
     * continuation function is applied to the
     * next value read from this inport.
     */
    //  def ==> (cont: T => Unit) = new InPort.InPortEvent[T](this, (()=>cont(this?)), isOpen)

    /**
     * Return an unconditional event, for use in an <tt>Alt</tt>.
     * If the event is fired by an <code>Alt</code>, the given
     * continuation function is invoked in an extended rendezvous
     * with the next value read from this inport.
     */
    //   def ==>> (cont: T => Unit) = new InPort.InPortEvent[T](this, ()=>this ? cont, isOpen)

    /**
     * Prepare to return an InPort event with a nontrivial guard.
     */
    //  @deprecated("Use the form guard &&& port", "2012") def apply (guard: => Boolean) = 
    //            new InPort.GuardedInPortEvent(this, ()=>(open&&guard))

    override def registerIn(a: ox.cso.Alt, n: Int): Int = throw new UnsupportedOperationException()
    override def deregisterIn(a: ox.cso.Alt, n: Int) = throw new UnsupportedOperationException()
  }
}