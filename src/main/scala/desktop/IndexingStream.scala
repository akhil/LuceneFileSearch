package desktop

import akka.Done
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class Batch(count: Int)
class Monitor extends Actor {
  var i: Int = 0
  override def postStop(): Unit = {
    println("...Stopping Indexer")
    super.postStop()
  }
  override def receive: Receive = {
    case Batch(count) =>
      i+= count
      println(s"Indexed Count: ${i}")
    case Done => println("stream completed")
    case Status.Failure(e) => println(s"stream failed: ${e.getMessage}")
  }
}

object IndexingStream {
  def main(args: Array[String]): Unit = index()

  def index(): Unit = {
    import FileIndexer._
    implicit val system = ActorSystem("QuickStart")
    import system.dispatcher
    val monitor = system.actorOf(Props[Monitor], "monitor")
    val batchCount = 100
    val filesSource = Source.fromIterator(() => generateFiles)
    val indexWriter = createIndexWriter()

    filesSource.mapAsync(5)(path => Future(createDocument(path)))
      .map(indexWriter.addDocument)
      .grouped(batchCount)
      .map { _ =>
        monitor ! Batch(batchCount)
        indexWriter.flush()
        indexWriter.commit()
      }
      .runWith(Sink.foreach(() => _))
      .onComplete {
        case Success(_) =>
          println("success")
          system.terminate()
        case Failure(ex) =>
          println(ex)
          ex.printStackTrace()
          system.terminate()
      }
  }
}