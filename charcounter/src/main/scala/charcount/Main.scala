package charcount

import akka.actor.{Actor, ActorSystem, ActorRef, Props, PoisonPill }
import scala.io.StdIn
import scala.io.Source

object Main {
  def main(args: Array[String]) {
    val system = ActorSystem("char-counter")

    val source = Source.fromFile("test.csv")
    val reader = system.actorOf(LineReaderActor.props(source), "csv-reader")     
    
    try StdIn.readLine() finally {
      source.close()
      system.terminate()
    }
  }
}

