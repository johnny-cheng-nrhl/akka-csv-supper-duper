package net.propoint.fun.platform

import cats.Monad
import doobie.{Transactor, Update}
import cats.implicits._
import cats.temp.par._
import doobie.implicits._

object DoobieHelpers {

  /**
   * A helper method which allows running of a list of updates in smaller batches. The updates are NOT applied in the
   * same transaction.
   *
   * @param xa A doobie transactor; A thin wrapper around a source of database connections
   * @param input A list of update info objects (should be a tuple type)
   * @param query the doobie Update function
   * @param queryBatchSize a configured batch size, signifying the maximum number of updates per batch
   * @tparam F The type of the effect e.g IO or Task
   * @tparam A The update information type. This will probably be a tuple. See the doobie batch update documentation.
   * @return The number of updates applied per each batch
   */
  def batchUpdate[A, F[_]: Monad: Par](
                                        xa: Transactor[F],
                                        input: List[A],
                                        query: Update[A],
                                        queryBatchSize: Int = 500
                                      ): F[List[Int]] = {
    val batchOperations = for {
      batch <- input.grouped(queryBatchSize)
      res <- batch.toNel.map { updateBatch =>
        query.updateMany(updateBatch).transact(xa)
      }
    } yield res

    batchOperations.toList.parSequence
  }
}
