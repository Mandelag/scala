package learnakka

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.github.tototoshi.csv.CSVReader

import scala.collection.mutable

object CsvMaster {
  case class ProcessCsv(fileName: String)
  case class RegisterWorker(worker: ActorRef)
  case class Row(row: Seq[String])
  case class JobsGiven(jobs: Int)
  object WorkerAvailable
  object TaskAvailable
  object CurrentlyBusy

  def props() = Props(new CsvMaster())
}

class CsvMaster extends Actor with ActorLogging {
  import CsvMaster._
  import ResultProcessor._

  val workers = mutable.Set.empty[ActorRef]

  var currentTask: Option[Iterator[Seq[String]]] = None
  var resultHandler: Option[ActorRef] = None
  var jobCount = 0

  def receive() = {

    case ProcessCsv(fileName) => {
      if (currentTask.isDefined) {
        sender() ! CurrentlyBusy
      } else {
        currentTask = Some(CSVReader.open(fileName).iterator)
        resultHandler = Some(context.actorOf(ResultProcessor.props()))
        workers.foreach( _ ! TaskAvailable)
      }
    }

    case WorkerAvailable => currentTask match {
      case None => {
        log.info("Worker available but no task.")
      }
      case Some(iterator) => {
        if (iterator.hasNext && resultHandler.isDefined) {
          sender().tell(Row(iterator.next()), resultHandler.get)
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
