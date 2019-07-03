package learnakka

import akka.actor.{Actor, ActorRef, Props}

object CsvWorker {
  case object Available

  def props(csvMaster: ActorRef) = Props(new CsvWorker(csvMaster))
}

class CsvWorker(csvMaster: ActorRef) extends Actor {
  import CsvMaster._
  import CsvWorker._

  override def preStart(): Unit = {
    csvMaster ! RegisterWorker(self)
    csvMaster ! WorkerAvailable
  }

  def receive() = {
    case Row(row) => {
      sender() ! processRow(row)
      self ! Available
    }
    case Available => {
      csvMaster ! WorkerAvailable
    }
    case TaskAvailable => {
      csvMaster ! WorkerAvailable
    }
  }

  def processRow(row: Seq[String]) = {
    Row(row ++ Seq[String]("ahahaiii"))
  }
}
