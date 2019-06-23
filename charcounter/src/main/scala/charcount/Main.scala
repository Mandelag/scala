package charcount

import akka.actor.{Actor, ActorSystem, ActorRef, Props, PoisonPill }
import scala.io.StdIn
import scala.io.Source
import com.typesafe.config._
import scala.collection.JavaConverters._

object Main {
  def main(args: Array[String]) {
    val opt = if (args.length > 0) args(0) else "driver"
    if ( opt == "worker") {
      startWorker(args(1), Integer.parseInt(args(2)))
    } else  if ( opt == "driver") {
      startDriver()
    }
  }

  def startWorker(listenAddress: String, listenPort: Int) {
    
    val workerConf = ConfigFactory.parseString(s"""
    akka {
      actor {
        provider = remote
      }
      remote {
        maximum-payload-bytes = 30000000 bytes
        netty.tcp {
          hostname = "$listenAddress"
          port = $listenPort

          // https://stackoverflow.com/questions/36685326/max-allowed-size-128000-bytes-actual-size-of-encoded-class-scala-error-in-akk
          message-frame-size =  30000000b
          send-buffer-size =  30000000b
          receive-buffer-size =  30000000b
          maximum-frame-size = 30000000b
        }
      }
    }
    """)
    
    val system = ActorSystem("char-counter", ConfigFactory.load(workerConf))

    try StdIn.readLine() finally {
      system.terminate()
    }
  }

  def startDriver() {
    import akka.actor.Address

    val driverConf = ConfigFactory.parseString(s"""
    akka {
      actor {
        provider = remote
      }
      remote {
        maximum-payload-bytes = 30000000 bytes
        netty.tcp {
          hostname = "0.0.0.0"
          port = 2552

          message-frame-size =  30000000b
          send-buffer-size =  30000000b
          receive-buffer-size =  30000000b
          maximum-frame-size = 30000000b
        }
      }
    }
    """)

    val system = ActorSystem("char-counter", ConfigFactory.load(driverConf))

    val config = ConfigFactory.load()

    val workers = config.getConfigList("workers")

    val workersConfig = workers.asScala.map( worker => {
      Address(
        worker.getString("transport"), 
        worker.getString("actor-system"),
        worker.getString("host"),
        worker.getInt("port"))
    }).toList

    val source = Source.fromFile("test.csv")
    val reader = system.actorOf(LineReaderActor.props(source, workersConfig), "csv-reader")     
    
    try StdIn.readLine() finally {
      source.close()
      system.terminate()
    }
  }
}

