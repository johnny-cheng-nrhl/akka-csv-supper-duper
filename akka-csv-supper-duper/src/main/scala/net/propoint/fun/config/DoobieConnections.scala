package net.propoint.fun.config

import doobie.util.transactor.Transactor

object DoobieConnections {

  final case class Read[F[_]](xa: Transactor[F])

  final case class Write[F[_]](xa: Transactor[F])

}
