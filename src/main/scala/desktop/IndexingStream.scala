package desktop

import java.nio.file.FileSystems

import akka.Done
import akka.actor._
import akka.stream.scaladsl._
import desktop.FileIndexer.Resume
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, FSDirectory}

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

  import SearchConfig._

  private val directory: Directory = FSDirectory.open(FileSystems.getDefault.getPath(INDEX_PATH))
  private val analyzer = new StandardAnalyzer()

  def createIndexWriter(): IndexWriter = {
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    indexWriter.deleteAll()
    indexWriter
  }

  private def resume2Doc(resume: Resume): Document = {
    val document = new Document()
    resume.email.foreach(email => document.add(new Field("email", email, TextField.TYPE_STORED)))
    document.add(new Field("content", resume.content.toOption.get, TextField.TYPE_STORED))
    document.add(new Field("fileName", resume.fileName, TextField.TYPE_STORED))
    document.add(new Field("filePath", resume.filePath, TextField.TYPE_STORED))
    document
  }

  def index(): Unit = {
    import FileIndexer._
    implicit val system: ActorSystem = ActorSystem("QuickStart")
    import system.dispatcher
    val monitor = system.actorOf(Props[Monitor], "monitor")
    val batchCount = 1000
    val filesSource = Source.fromIterator(() => generateFiles).async
    val indexWriter = createIndexWriter()

    filesSource.mapAsync(5)(path => Future(createResume(path)))
      .filter(resume => resume.content.isSuccess)
      .map(resume2Doc).map(indexWriter.addDocument)
      .grouped(batchCount)
      .map { _ =>
        monitor ! Batch(batchCount)
        indexWriter.flush()
        indexWriter.commit()
      }
      .runWith(Sink.foreach(() => _))
      .onComplete {
        case Success(_) =>
          indexWriter.flush()
          indexWriter.commit()
          println("success")
          system.terminate()
        case Failure(ex) =>
          println(ex)
          ex.printStackTrace()
          system.terminate()
      }
  }
}