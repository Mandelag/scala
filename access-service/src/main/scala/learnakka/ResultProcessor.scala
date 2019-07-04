package learnakka

import akka.actor.{Actor, PoisonPill, Props}
import learnakka.CsvMaster.{BatchRow, JobsGiven, Row}


object ResultProcessor {
  def props() = Props(new ResultProcessor())

  case object CheckDone
  case class WaitCounter(increment: Int)
}

class ResultProcessor extends Actor {

  var messagesReceived = 0
  var totalMessages: Option[Int] = None
  val startTime = System.currentTimeMillis()

  def receive() = {
    case Row(row) => {
//      println(row)
      messagesReceived += 1
      checkDone()
    }
    case BatchRow(rows) => {
      messagesReceived += 1
      checkDone()
    }
    case JobsGiven(n) => {
      totalMessages = Some(n)
      checkDone()
    }
  }

  def checkDone() = {
    if (messagesReceived >= totalMessages.getOrElse(Int.MaxValue)) {
      println(s"So doone in ${System.currentTimeMillis() - startTime} ms")
      self ! PoisonPill
    }
  }
}
