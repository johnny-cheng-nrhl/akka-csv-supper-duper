package net.propoint.fun

import akka.Done

import scala.concurrent.Future

// TODO allow batch acking
/**
 * A typeclass that adds the ability to "acknowledge" a message.
 */
trait Acker[A] {

  def ack(a: A): Future[Done]

}

object Acker {

  def apply[A](implicit instance: Acker[A]): Acker[A] = instance

  implicit class AckerOps[A: Acker](a: A) {
    def ack(): Future[Done] = Acker[A].ack(a)
  }

  implicit def ackOption[A](implicit c: Acker[A]): Acker[Option[A]] =
    _.map(c.ack).getOrElse(Future.successful(Done))

  implicit def ackEither[L, R](implicit lc: Acker[L], rc: Acker[R]): Acker[Either[L, R]] =
    _.fold(lc.ack, rc.ack)
  
}
