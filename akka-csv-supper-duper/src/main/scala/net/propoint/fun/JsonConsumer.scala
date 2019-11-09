package net.propoint.fun

import java.nio.file.Paths
import java.util.UUID

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.alpakka.json.scaladsl.JsonReader
import akka.stream.{ActorMaterializer, Graph, IOResult, SinkShape, Supervision}
import akka.stream.scaladsl.{FileIO, Flow, JsonFraming, Keep, Sink, Source}
import akka.util.ByteString
import io.circe
import net.propoint.fun.definition.PurchaseOrderDefinition.Person
import net.propoint.fun.parser.PurchaseOrderParser


import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

object JsonConsumer extends App {
  import io.circe._
  import io.circe.generic.auto._
  
  // implicit actor system
  implicit val system = ActorSystem("Sys")

  // implicit actor materializer
  implicit val materializer = ActorMaterializer()

  val path = Paths.get("src/main/resources/po_backfill_1.json")
  println(path)

  private val jsonFileSource = FileIO.fromPath(path)
  type PurchaseOrderFlowElement = Either[circe.Error, Person]
  
  val input =
    """
      |[
      | { "name" : "john" },
      | { "name" : "Ég get etið gler án þess að meiða mig" },
      | { "name" : "jack" },
      |]
      |""".stripMargin // also should complete once notices end of array

//  val results =
//    jsonFileSource
//      .via(JsonFraming.objectScanner(Int.MaxValue))
//      .runFold(Seq.empty[String]) 
//      {
//        case (acc, entry) => acc ++ Seq(entry.utf8String)
//      }

  val flow1: Flow[ByteString, ByteString, NotUsed] =
    JsonReader.select("$.rows[*].doc")

  val flow2: Flow[ByteString, String, NotUsed] =
    Flow[ByteString].map(_.utf8String)

  val flow3: Flow[String, PurchaseOrderFlowElement , NotUsed] =
    Flow[String].map(PurchaseOrderParser.decodePurchaseOrder(_))

  val results =
    jsonFileSource
      .via(flow1)
      .via(flow2)
      .via(flow3)
      .toMat(Sink.fold(Seq.empty[String]) {
        case (acc, entry) => acc ++ Seq(entry.toString())
      })(Keep.right)
      .withAttributes(supervisionStrategy(Supervision.resumingDecider))
      .run()
  
  private val reply = Await.result(results, 10 seconds)
  println(s"Received $reply")
  Await.ready(system.terminate(), 10 seconds)
}
