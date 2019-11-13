package net.propoint.fun.consumers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO

trait FlowConsumer {
  def run(): IO[_]

  implicit def system: ActorSystem

  implicit def materializer: ActorMaterializer
}
