package learnakka

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem

import scala.io.StdIn

object Main {
  import CsvMaster.ProcessCsv

  def main(args: Array[String]): Unit = {

    val system = ActorSystem("csv-processing")

    val master = system.actorOf(CsvMaster.props())

    for ( i <- 1 to 16 ) {
      system.actorOf(CsvWorker.props(master))
    }

    master ! ProcessCsv("crime.csv")

    try StdIn.readLine() finally {
      system.terminate()
    }
  }
}
