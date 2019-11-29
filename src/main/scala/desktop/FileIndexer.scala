package desktop

import java.io.FileInputStream
import java.nio.file.{FileSystems, Files, Path}

import org.apache.tika.metadata.Metadata

import scala.collection.JavaConverters._
import scala.util.Try

object FileIndexer {
  case class Resume(content: Try[String], email: Option[String], fileName: String, filePath: String)

  import SearchConfig.SCAN_PATH

  private def dirs: List[Path] = SCAN_PATH.map(FileSystems.getDefault.getPath(_))

  def generateFiles: Iterator[Path] =
    dirs.flatMap(Files.walk(_).iterator.asScala).iterator.filter(Files.isRegularFile(_))

  def createResume(file: Path): Resume = {
    val fileBody = file2String(file)
    Resume(content = fileBody,
      email = fileBody.map(extractEmail).toOption.flatten,
      fileName = file.getFileName.toString,
      filePath = file.toAbsolutePath.toString
    )
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
}