package desktop

import java.io.FileInputStream
import java.nio.file.{FileSystems, Files, Path}
import java.time.LocalDateTime

import desktop.SearchConfig.SCAN_PATH
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.{Document, Field, TextField}
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.{Directory, FSDirectory}
import org.apache.tika.metadata.Metadata

import scala.collection.{View, WithFilter}
import scala.jdk.StreamConverters._
import scala.util.Try

sealed trait IndexStatus
case class Indexing(i: Int) extends IndexStatus
case class IndexComplete(i: Int) extends IndexStatus

class FileIndexer {
  import FileIndexer._
  def runIndexer(listeners: List[IndexStatus => Unit]): Unit = {
    val indexWriter = createIndexWriter()
    var i = 0
    generateFiles
      .map(createDocument)
      .map(indexWriter.addDocument)
      .map { _ =>
        i += 1
        if(i % 100 == 0) {
          indexWriter.flush()
          indexWriter.commit()
        }
        Indexing(i)
      }
      .foreach(i => listeners.foreach(_ => i))
    indexWriter.flush()
    indexWriter.commit()
    indexWriter.close()
    listeners.foreach(_ (IndexComplete(i)))
  }


}

object FileIndexer {
  import SearchConfig._

  private val directory: Directory = FSDirectory.open(FileSystems.getDefault.getPath(INDEX_PATH))
  private val analyzer = new StandardAnalyzer()

  private def dirs: List[Path] = SCAN_PATH.map(FileSystems.getDefault.getPath(_))

  def createIndexWriter(): IndexWriter = {
    val indexWriterConfig = new IndexWriterConfig(analyzer)
    val indexWriter = new IndexWriter(directory, indexWriterConfig)
    indexWriter.deleteAll()
    indexWriter
  }

  def generateFiles: Iterator[Path] =
    dirs.map(Files.walk(_).toScala(LazyList)).flatten.iterator.filter(Files.isRegularFile(_))

  def createDocument(file: Path): Document = {
    val document = new Document()
    val fileBody = file2String(file)
    fileBody.map(fb => document.add(new Field("content", fb, TextField.TYPE_STORED)))
      .recover {
        case ex: Exception =>
          println("===>" + file.toString)
          ex.printStackTrace()
      }
    fileBody.toOption.flatMap(extractEmail).foreach ( email =>
      document.add(new Field("email", email, TextField.TYPE_STORED))
     )
    document.add(new Field("fileName", file.getFileName.toString, TextField.TYPE_STORED))
    document.add(new Field("filePath", file.toAbsolutePath.toString, TextField.TYPE_STORED))
    document.add(new Field("updateTime", file.toFile.lastModified.toString, TextField.TYPE_STORED))
    document
  }

  def extractEmail(content: String): Option[String] = {
    val emailRegex = """[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*""".r
    //val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    //val emailRegex = """\A[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\z""".r
    emailRegex.findFirstIn(content)
      .map(_.trim.stripMargin).filter(_.split("[@.]").length >= 3)
  }

  def file2String(file: Path): Try[String] = {
    import org.apache.tika.parser.AutoDetectParser
    import org.apache.tika.sax.BodyContentHandler
    val handler = new BodyContentHandler
    val parser = new AutoDetectParser
    val metadata = new Metadata

    val stream = new FileInputStream(file.toFile)
    Try(parser.parse(stream, handler, metadata)).map(_ => handler.toString)
  }

  def main(args: Array[String]): Unit = {
    val fileIndexer = new FileIndexer
    def log: IndexStatus => Unit = {
      case Indexing(i) => if (i % 100 == 0) println(s"$i")
      case IndexComplete(i) =>println((s"Index complete count: $i"))
    }
    fileIndexer.runIndexer(List(log))
  }
}