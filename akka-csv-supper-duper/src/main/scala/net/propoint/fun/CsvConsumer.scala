package net.propoint.fun

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.alpakka.csv.scaladsl.{CsvParsing, CsvToMap}
import akka.stream.scaladsl.{FileIO, Flow, GraphDSL, RunnableGraph, Sink}
import akka.util.ByteString
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object CsvConsumer extends App {
  // implicit actor system
  implicit val system = ActorSystem("Sys")

  // implicit actor materializer
  implicit val materializer = ActorMaterializer()
  
  //  val path = Paths.get("src/main/resources/backfill_events.csv")
  val path = Paths.get("src/main/resources/backfill_events4.csv")
  println(path)

  private def parseOptionalStr(str: String): Option[String] =
    Option(str).map(_.trim).filter(_.nonEmpty)

  val defaultDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy")
  val poEventDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yy HH:mm")
  val databaseDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("MM/dd/yyy HH:mm:ss")

  private def parseOptionalDateTimeStr(str: String): Option[DateTime] =
    parseOptionalStr(str).map(DateTime.parse(_, poEventDateTimeFormatter))

  private val lineByLineSource = FileIO.fromPath(path)
//    .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
//    .map(_.utf8String).drop(1)

  private def makeUpddateString(startDate: DateTime, endDate: DateTime, eventId: Long) =
    s"UPDATE events SET start_date = str_to_date(CONCAT(date('${startDate.getYear}-${startDate.getMonthOfYear}-${startDate.getDayOfMonth}'), ' ', time('08:00'))," +
      s" '%Y-%m-%d %H:%i:%s'), end_date = str_to_date(CONCAT(date('${endDate.getYear}-${endDate.getMonthOfYear()}-${endDate.getDayOfMonth}'), ' ', time('08:00')), '%Y-%m-%d %H:%i:%s') " +
      s"WHERE event_id = $eventId;"
  
  val source = FileIO.fromPath(path)
  val flow1: Flow[ByteString, List[ByteString], NotUsed] = 
    CsvParsing.lineScanner()
  val flow2: Flow[List[ByteString], List[String], NotUsed] = 
    Flow[List[ByteString]].map(list => list.map(_.utf8String))

//  val flow3: Flow[List[String], SalesEvent, NotUsed] =
//    Flow[List[String]].map(
//      l =>
//        SalesEvent(l(0).toLong,
//                  DateTime.parse(l(1), defaultDateTimeFormatter),
//                  DateTime.parse(l(2), defaultDateTimeFormatter),
//                  parseOptionalStr(l(3)).map(_.toLong),
//                  DateTime.parse(l(4), poEventDateTimeFormatter),
//                  DateTime.parse(l(5), poEventDateTimeFormatter))
//    )
  val flow3_1: Flow[List[String], EventInfo, NotUsed] =
    Flow[List[String]].map(
      l =>
        EventInfo(parseOptionalStr(l(0)).map(_.toLong),
          DateTime.parse(l(1), poEventDateTimeFormatter),
          DateTime.parse(l(2), poEventDateTimeFormatter)
        )
    )
//  val flow4: Flow[SalesEvent, SalesEvent, NotUsed] =
//    Flow[SalesEvent].filter(s => s.eventId.isDefined)

  val flow4_1: Flow[EventInfo, EventInfo, NotUsed] =
    Flow[EventInfo].filter(e => e.eventId.isDefined)

//  val flow5: Flow[SalesEvent, String, NotUsed] =
//    Flow[SalesEvent].map(s => makeUpddateString(s.eventStartDate,
//      s.eventEndDate, s.eventId.get))

  val flow5_1: Flow[EventInfo, String, NotUsed] =
    Flow[EventInfo].map(e => makeUpddateString(e.eventStartDate,
      e.eventEndDate, e.eventId.get))

  // val flow3 = Flow[List[String]].map(list => CountryCapital(list(0), list(1)))

  val future: Future[Done] = lineByLineSource
    .via(flow1)
    .via(flow2)
    .drop(1)
    .via(flow3_1)
    .via(flow4_1)
    .via(flow5_1)
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

case class SalesEvent(poId: Long, startShipDate: DateTime, endShipDate: DateTime,
                      eventId: Option[Long], eventStartDate: DateTime, eventEndDate: DateTime)

case class EventInfo(eventId: Option[Long], eventStartDate: DateTime, eventEndDate: DateTime)