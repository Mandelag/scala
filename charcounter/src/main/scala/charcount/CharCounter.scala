package charcount

import akka.actor._
import akka.event.Logging
import scala.io.BufferedSource
import akka.actor.Address

object LineReaderActor {
  def props(fileStream: BufferedSource, workerConfig: List[Address]) = Props(new LineReaderActor(fileStream, workerConfig))
  // Tells the LineReaderActor to read the next N lines
  case class ReadMore(lines: Int)
  // Send batches of rows, containing columns.
  case class Lines(lines: List[String])
  // Data structure to save the resulting letter count. 
  case class CharCount(value: List[(Char, Int)])
}

object CharCounterActor {
  import LineReaderActor._

  def props() = Props(new CharCounterActor())

  object Ready

  val processRow: (List[String]) => CharCount = processRowsDefault
  
  private def processRowsDefault(rows: List[String]): CharCount = {
    CharCount(rows
        .flatMap(_.toString)
        .filter(_.isLetterOrDigit)
        .map(_.toLower)
        .groupBy(x => x)
        .mapValues(_.length)
        .toList)
  }

  private def processRowsWithFold(rows: List[String]): CharCount = {
    import scala.collection.mutable.Map

    CharCount(rows
        .flatMap(_.toString)
        .filter(_.isLetterOrDigit)
        .foldLeft(collection.mutable.Map[Char, Int]()){ (mutableMap, char) =>
          val prev = mutableMap.getOrElse(char, 0)
          mutableMap(char) = prev + 1
          mutableMap
        }
        .toList
        )
  }
}

class LineReaderActor(fileStream: BufferedSource, workerAddress: List[Address]) extends Actor with ActorLogging {
  import akka.actor.{ Address, AddressFromURIString, Deploy, Props }
  import akka.remote.RemoteScope
  
  var waitCounter = 0

  val rows = fileStream.getLines
  val NUMBER_OF_WORKER_ACTORS= 16

  val t0 = System.currentTimeMillis()

  val workerRefs = workerAddress.flatMap( address => {
    for (i <- 1 to NUMBER_OF_WORKER_ACTORS) yield {
      val actorRef = context.actorOf(CharCounterActor.props().withDeploy(Deploy(scope = RemoteScope(address))))
      waitCounter += 1
      actorRef
    }
  })


  import LineReaderActor._

  override def receive = {
    case ReadMore(value) => {
      if (!rows.hasNext) {
        workerRefs.foreach(_ ! PoisonPill)
      } else {
        sender() ! Lines(rows.take(value).toList)
      }
    }
    case CharCount(counts) =>   {
      if (!counts.isEmpty) {
        log.info(s"$counts")
      }
      waitCounter -= 1
//      println(waitCounter)
       checkIfDone
    }
  }

  override def postStop = {
    val elapsed = System.currentTimeMillis() - t0

    log.info(s"Done in $elapsed ms.")
  }

  private def checkIfDone = {
    if (waitCounter <= 0) {
        self ! PoisonPill
    }
  }
}

class CharCounterActor() extends Actor with ActorLogging {
  import LineReaderActor._
  import CharCounterActor._

  final val DEFAULT_BATCH_SIZE = 1024

  val result = scala.collection.mutable.Map[Char, Int]()

  override def preStart = {
    self ! Ready
  }

  override def receive = {
    // Count character in a batch of lines
    case Lines(rows) => {
      log.info("Received new row!  Processing...")
      val charCount = processRow(rows)

      charCount.value.map( count => {
        val updatedValue: Int = result.getOrElse(count._1, 0) + count._2
        result.update(count._1, updatedValue)
      })

      notifyReadyToWork()
    }
  }

  override def postStop() = {
    context.parent ! CharCount(result.toList)
  }

  private def notifyReadyToWork() {
    context.parent ! ReadMore(DEFAULT_BATCH_SIZE)
  }
}
