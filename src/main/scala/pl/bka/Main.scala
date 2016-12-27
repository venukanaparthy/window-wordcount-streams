package pl.bka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import pl.bka.displays.{PrintlnDisplay, WebsocketDisplay}
import pl.bka.filters.{Distinct, WarmUpWindow}
import pl.bka.soruces.TextFileSource
import pl.bka.sources.TwitterSource
import pl.bka.windows.Top

import scala.concurrent.duration._
import spray.json._
import DefaultJsonProtocol._

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val windowSize = 15000
    val source =
      args(0) match {
        case "text" =>
          WarmUpWindow.fakeWords(TextFileSource.words("input3.txt", 1.millis), windowSize)
            .via(Top.nwordsSliding(windowSize, 6, 4))
            .via(Distinct.distinct(Seq((0, ""))))
        case "twitter" =>
          WarmUpWindow.fakeWords(TwitterSource.source(Config(ConfigFactory.load())), windowSize)
            .via(Top.nwordsSliding(windowSize, 6, 4))
            .via(Distinct.distinct(Seq((0, ""))))
      }
    args(1) match {
      case "web" =>
        WebsocketDisplay(source.map(_.toJson.toString)).bind()
      case "stdout" =>
        PrintlnDisplay(source.map(_.toJson.toString)).display()
    }
  }
}

