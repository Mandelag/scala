package learnakka

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


object Main {

  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
      args(0) match {
        case "worker" => worker()
        case "driver" => driver()
      }
    } else {
      println("Usage: useservice worker | driver")
    }
  }


  def worker() = {
    val system = ActorSystem("csv-processing")

    val config = ConfigFactory.load()

    val master = if (!config.hasPath("app.csv-master-address")) {
      Future {
        system.actorOf(CsvMaster.props(), "csv-master")
      }
    } else {
      val masterPath = config.getString("app.csv-master-address")
      system.actorSelection(masterPath).resolveOne(1 seconds)
    }

    val result = Await.result(master, 1 seconds)

    for ( i <- 1 to 9 ) {
      system.actorOf(CsvWorker.props(result))
    }

    try StdIn.readLine() finally {
      system.terminate()
    }
  }

  def driver() = {
    val system = ActorSystem("csv-processing")

    val config = ConfigFactory.load()

    val masterPath = config.getString("app.csv-master-address")
    val master = system.actorSelection(masterPath).resolveOne(10 seconds)

    val result = Await.result(master, 10 seconds)

    import CsvMaster.ProcessCsv
    result ! ProcessCsv("crime.csv")

    var exit = false
    while (!exit) {
      val in = StdIn.readLine()
      if (in == "exit") {
        system.terminate()
        exit = true
      }
      if (in == "test") {
        result ! ProcessCsv("crime.csv")
      }
    }
  }
}
