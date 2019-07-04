package learnakka

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.github.tototoshi.csv.CSVReader

import scala.collection.mutable

object CsvMaster {
  case class ProcessCsv(fileName: String)
  case class RegisterWorker(worker: ActorRef)
  case class Row(row: Seq[String])
  case class BatchRow(rows: Seq[Seq[String]])
  case class JobsGiven(jobs: Int)
  case object WorkerAvailable
  case object TaskAvailable
  case object CurrentlyBusy

  def props() = Props(new CsvMaster())
}

class CsvMaster extends Actor with ActorLogging {
  import CsvMaster._

  val workers = mutable.Set.empty[ActorRef]

  val BATCH_SIZE = 10000

  // each task is a group of csv rows
  var currentTask: Option[Iterator[Seq[Seq[String]]]] = None

  var resultHandler: Option[ActorRef] = None
  var jobCount = 0

  def receive() = {

    case ProcessCsv(fileName) => {
      if (currentTask.isDefined) {
        println(currentTask)
        sender() ! CurrentlyBusy
      } else {
        val iter = CSVReader.open(fileName).iterator
        iter.grouped(BATCH_SIZE)
        currentTask = Option(iter.grouped(BATCH_SIZE))
        resultHandler = Some(context.actorOf(ResultProcessor.props()))
        workers.foreach( worker => {
          worker ! TaskAvailable
        })
      }
    }

    case WorkerAvailable => currentTask match {
      case None => {
        log.info("Worker available but no task.")
      }
      case Some(iterator) => {
        if (iterator.hasNext && resultHandler.isDefined) {
          val rows = iterator.next()
          sender().tell(BatchRow(rows), resultHandler.get)
          jobCount = jobCount + 1
        } else {
          resultHandler.getOrElse(ActorRef.noSender) ! JobsGiven(jobCount)

          currentTask = None
          resultHandler = None
          jobCount = 0
        }
      }
    }

    case RegisterWorker(worker) => {
      context.watch(worker)
      workers += worker
    }

    case Terminated(worker) => {
      workers.remove(worker)
    }

  }
}
