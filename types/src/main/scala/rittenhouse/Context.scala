package rittenhouse

import akka.actor.ActorSystem

object Context {

  implicit lazy val system = ActorSystem("RittenhouseSystem")

}
