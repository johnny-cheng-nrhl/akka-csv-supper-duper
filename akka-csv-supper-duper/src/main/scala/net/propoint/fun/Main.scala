package net.propoint.fun

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import cats.implicits._
import com.typesafe.scalalogging.LazyLogging
import net.propoint.fun.config.{AppConfig, MysqlConfig}
import net.propoint.fun.consumers.JsonConsumer
import net.propoint.fun.daos.{InboundOrderItemDao, InboundOrderItemDaoImpl, InventoryDao, InventoryDaoImpl}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

object Main extends App with LazyLogging {

  // TODO configure the ExecutionContext
  import scala.concurrent.ExecutionContext.Implicits.global
  
  // implicit actor system
  implicit val system = ActorSystem("Sys")

  // implicit actor materializer
  implicit val materializer = ActorMaterializer()
  
  // only load config once, and pass around what you need
  val appConfig = AppConfig.loadFromEnvironment()
  
  val program: IO[Unit] = for {
    catalogDBWrite <- IO(MysqlConfig.writeConnection[IO](appConfig.catalogDBWriteConfig))
    ecommDBWrite <- IO(MysqlConfig.writeConnection[IO](appConfig.ecommDBWriteConfig))
    
    inventoryDao = new InventoryDaoImpl(ecommDBWrite)
    inboundOrderItemDao = new InboundOrderItemDaoImpl(catalogDBWrite)
    result <- JsonConsumer(appConfig, inventoryDao, inboundOrderItemDao).run().attempt
  
  } yield {
    result.leftMap(e => logger.error("Stream failed:", e))
  }

  program.unsafeRunSync()
  
  Await.ready(system.terminate(), 10 seconds)
}
