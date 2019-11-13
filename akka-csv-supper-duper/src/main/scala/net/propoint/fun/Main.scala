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
  //  implicit val cs = IO.contextShift(ExecutionContext.global)

  // only load config once, and pass around what you need
  val appConfig = AppConfig.loadFromEnvironment()
  
  def runConsumersProgram(appConfig: AppConfig, inventoryDao: InventoryDao[IO], inboundOrderItemDao: InboundOrderItemDao[IO]): IO[_] = {
    JsonConsumer(appConfig, inventoryDao, inboundOrderItemDao).run()
  }

  val program: IO[Unit] = for {
    catalogDBWrite <- IO(MysqlConfig.writeConnection[IO](appConfig.catalogDBWriteConfig))
    ecommDBWrite <- IO(MysqlConfig.writeConnection[IO](appConfig.ecommDBWriteConfig))
    
    inventoryDao = new InventoryDaoImpl(ecommDBWrite)
    inboundOrderItemDao = new InboundOrderItemDaoImpl(catalogDBWrite)
    _ <- JsonConsumer(appConfig, inventoryDao, inboundOrderItemDao).run()
  
  } yield {}

  program.unsafeRunSync()
  
//  private val reply = Await.result(results, 10 seconds)
//  println(s"Received $reply")
  Await.ready(system.terminate(), 10 seconds)
}
