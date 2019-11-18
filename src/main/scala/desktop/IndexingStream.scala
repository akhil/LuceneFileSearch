package desktop

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object IndexingStream extends App {
  import FileIndexer._
  implicit val system = ActorSystem("QuickStart")
  import system.dispatcher
  val filesSource = Source.fromIterator(() => generateFiles)
  val indexWriter = createIndexWriter()
  val start = System.currentTimeMillis()
  filesSource. mapAsync(5)(path => Future(createDocument(path)))
    .map(indexWriter.addDocument)
    .grouped(100)
    .map { _ =>
      println(s"${System.currentTimeMillis() - start}" )
      indexWriter.flush()
      indexWriter.commit()
    }
    .runWith(Sink.foreach(() => _))
    .onComplete{
      case Success(_) =>
        println("success")
        system.terminate()
      case Failure(ex) =>
        println(ex)
        ex.printStackTrace()
        system.terminate()
  }
}