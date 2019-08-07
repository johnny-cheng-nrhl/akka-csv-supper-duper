package net.propoint.fun

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, RunnableGraph, Sink}
import akka.util.ByteString
import akka.stream.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object CsvConsumer extends App {
  // implicit actor system
  implicit val system = ActorSystem("Sys")

  // implicit actor materializer
  implicit val materializer = ActorMaterializer()
  
  val path = Paths.get("src/main/resources/backfill_events1.csv")
  println(path)

  private val lineByLineSource = FileIO.fromPath(path)
//    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
//    .map(_.utf8String).drop(1)
  
  val source = FileIO.fromPath(path)
  val flow1: Flow[ByteString, List[ByteString], NotUsed] = 
    CsvParsing.lineScanner()
  val flow2: Flow[List[ByteString], List[String], NotUsed] = 
    Flow[List[ByteString]].map(list => list.map(_.utf8String))

  // val flow3 = Flow[List[String]].map(list => CountryCapital(list(0), list(1)))

  val future: Future[Done] = lineByLineSource
    .via(flow1)
    .via(flow2)
    .drop(1)
    .runForeach(println)
  
  private val reply = Await.result(future, 10 seconds)
  println(s"Received $reply")
  Await.ready(system.terminate(), 10 seconds)

//  val sink: Sink[String] = Sink.foreach { x => println(x.split(",").size) }
//  val sink = Sink[String] = Sink.foreach(println)
  
//  Source.single(source)
//    .via(CsvParsing.lineScanner())
//    .via(CsvToMap.toMap())
//    .map(_.mapValues(_.utf8String))
//    .runForeach(printlin)
  
//  val graph: RunnableGraph[_] = RunnableGraph.fromGraph(GraphDSL.create(sink) { 
//    implicit builder =>
//      s => 
//        implicit GraphDSL.Implicits._
//        source ~> flow1 ~> flow2 ~> s.in
//        ClosedShape
//  })
//
//  val future = graph.run()
//  future.onComplete { _ =>
//    system.terminate()
//  }
//  Await.result(system.whenTerminated, Duration.Inf)
}
