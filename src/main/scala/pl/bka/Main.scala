package pl.bka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import pl.bka.displays.{PrintlnDisplay, WebsocketDisplay}
import pl.bka.filters.{AddTimestamp, Distinct, HeartBeatMerge, WarmUpWindow}
import pl.bka.model.{Message, WindowWordCounts}
import pl.bka.sources.{DebugSource, TextFileSource, TwitterSource}
import pl.bka.windows.Top

import scala.concurrent.duration._

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val windowSize = if(args.length >= 3) args(2).toInt else 5000
    def minWordLength(default: Int) = if(args.length >= 4) args(3).toInt else default
    val source =
      args(0) match {
        case "debug" =>
          DebugSource(6, 10.millis, throughPublisher = true)
            .via(HeartBeatMerge.flow)
        case "text" =>
          WarmUpWindow.fakeWords(windowSize)
            .concat(TextFileSource.words("input3.txt", 1.millis))
            .via(AddTimestamp.flow)
            .via(Top.nwordsSliding(windowSize, 6, minWordLength(5)))
            .via(Distinct.window)
            .via(HeartBeatMerge.flow)
        case "twitter" =>
          WarmUpWindow.fakeWords(windowSize)
            .concat(TwitterSource.source(Config(ConfigFactory.load())))
            .via(AddTimestamp.flow)
            .via(Top.nwordsSliding(windowSize, 6, minWordLength(1)))
            .via(Distinct.window)
            .via(HeartBeatMerge.flow)
      }
    args(1) match {
      case "web" =>
        WebsocketDisplay(source.map(Message.toJson)).bind()
      case "stdout" =>
        PrintlnDisplay(source.map(Message.toJson)).display()
    }
  }
}

