package learnakka

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object CsvWorker {
  case object Available

  def props(csvMaster: ActorRef) = Props(new CsvWorker(csvMaster))
}

class CsvWorker(csvMaster: ActorRef) extends Actor with ActorLogging {
  import CsvMaster._
  import CsvWorker._

  override def preStart(): Unit = {
    csvMaster ! RegisterWorker(self)
    csvMaster ! WorkerAvailable
  }

  def receive() = {
    case Row(row) => {
      sender() ! Row(processRow(row))
      self ! Available
    }
    case BatchRow(rows) => {
//      log.info("Processing rows of length " + rows.length)
      sender() ! BatchRow(processBatchRow(rows))
      self ! Available
    }
    case Available => {
      csvMaster ! WorkerAvailable
    }
    case TaskAvailable => {
      csvMaster ! WorkerAvailable
    }
  }

  def processBatchRow(rows: Seq[Seq[String]]) = {
    rows.map(processRow)
  }

  def processRow(row: Seq[String]) = {
    row ++ Seq[String]("ahahaiii")
  }
}
