import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory


val conf = ConfigFactory.parseString(
  """
    |akka.remote.netty.tcp.port=2666
  """.stripMargin
)
val system = ActorSystem("csv-processing", conf)


