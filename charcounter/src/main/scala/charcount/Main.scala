package charcount

import akka.actor.{Actor, ActorSystem, ActorRef, Props, PoisonPill }
import scala.io.StdIn
import scala.io.Source

object Main {
  def main(args: Array[String]) {
    val system = ActorSystem("WordCounter")

    val source = Source.fromFile("test.csv")
    val reader = system.actorOf(LineReaderActor.props(source), "csv-reader")     
    
    import LineReaderActor._
    reader ! ReadMore(109)
    
    try StdIn.readLine() finally {
      source.close()
      system.terminate()
    }
  }
}

