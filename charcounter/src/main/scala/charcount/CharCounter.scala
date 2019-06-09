package charcount

import akka.actor._
import akka.event.Logging
import scala.io.BufferedSource


object LineReaderActor {
  def props(fileStream: BufferedSource) = Props(new LineReaderActor(fileStream))
  // Tells the LineReaderActor to read the next N lines
  case class ReadMore(lines: Int)
  // Send batches of rows, containing columns.
  case class Line(line: String)
  // Data structure to save the resulting letter count. 
  case class CharCount(map: Map[Char, Int])
}

object CharCounterActor {
  case object Done
}

class LineReaderActor(fileStream: BufferedSource) extends Actor {
  val log = Logging(context.system, this)
  val rows = fileStream.getLines.map(processLine _)

  import LineReaderActor._

  override def receive = {
    case ReadMore(value) => {
      rows.take(value).toList
      if (!rows.hasNext) {
        self ! PoisonPill
      }
    }
  }

  override def postStop = {
    println("Done!")
  }

  private def processLine(row: String): Seq[String] = {
    row.split(",").map(_.trim)
  }
}

class CharCounterActor extends Actor {
  import LineReaderActor._
  import CharCounterActor._

  override def receive = {
    case Line(rows) => {
      println(rows)
    }
    case Done => {
      self ! PoisonPill
    }
  }
}
