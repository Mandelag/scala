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
  case class CharCount(map: List[(Char, Int)])
}

object CharCounterActor {
  import LineReaderActor._

  def props(reader: ActorRef) = Props(new CharCounterActor(reader))

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
  val result = scala.collection.mutable.Map[Char, Int]()

  val workerRefs = workerAddress.map( address => {
    for (i <- 1 to NUMBER_OF_WORKER_ACTORS) yield {
      val actorRef = context.actorOf(CharCounterActor.props(self).withDeploy(Deploy(scope = RemoteScope(address))))
      actorRef
    }
  }).flatten


  import LineReaderActor._

  override def receive = {
    case ReadMore(value) => {
      if (!rows.hasNext) {
        workerRefs.foreach(_ ! PoisonPill)
        // self ! PoisonPill
      } else {
        sender() ! Lines(rows.take(value).toList)
        waitCounter += 1
      }
    }
    case CharCount(counts) =>   {
      counts.foreach(count => {
        val updatedValue: Int = result.get(count._1).getOrElse(0) + count._2
        result.update(count._1, updatedValue)
      })
      waitCounter -= 1
//      println(waitCounter)
       checkIfDone
    }
  }

  override def postStop = {
    val elapsed = System.currentTimeMillis() - t0
    println(result)
    log.info(s"Done in $elapsed ms.")
  }

  private def checkIfDone = {
    if (waitCounter <= 0) {
        self ! PoisonPill
    }
  }
}

class CharCounterActor(source: ActorRef) extends Actor with ActorLogging {
  import LineReaderActor._
  import CharCounterActor._

  final val DEFAULT_BATCH_SIZE = 1024

  override def preStart = {
    self ! Ready
  }

  override def receive = {
    // Count character in a batch of lines
    case Lines(rows) => {
      log.info("Received new row!  Processing...")
      val reply = processRow(rows)
      source ! reply
      self ! Ready // requeue
    }
    case Ready => {
      source ! ReadMore(DEFAULT_BATCH_SIZE)
    }
    case _ => {
      println("Dapet surat")
    }
  }

}
