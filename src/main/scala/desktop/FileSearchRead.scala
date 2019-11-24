package desktop

import java.nio.file.FileSystems

import akka.NotUsed
import akka.actor.ActorSystem
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.highlight.{Highlighter, QueryScorer, SimpleHTMLFormatter, SimpleSpanFragmenter}
import org.apache.lucene.search.{BooleanClause, IndexSearcher, ScoreDoc}
import org.apache.lucene.store.FSDirectory
import stream.Deduplicate

import scala.concurrent.Future

object FileSearchRead {

  //val keyWord = "java AND spring"
  private val default = FileSystems.getDefault
  private val path = default.getPath(SearchConfig.INDEX_PATH)
  private val directory = FSDirectory.open(path)
  private val directoryReader = DirectoryReader.open(directory)
  private val indexSearcher = new IndexSearcher(directoryReader)
  private val analyzer = new StandardAnalyzer()

  private val fields = Array("fileName", "content")
  private val clauses = Array(BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD)

  def getScoreDocs(keyWord: String, n: Int): Iterator[ScoreDoc] = {
    val multiFieldQuery = MultiFieldQueryParser.parse(keyWord, fields, clauses, analyzer)
    val topDocs = indexSearcher.search(multiFieldQuery, n)
    val scoreDocs = topDocs.scoreDocs.iterator
    println("ScoreDocs: " + scoreDocs.length)
    scoreDocs
    /*
    val scorer = new QueryScorer(multiFieldQuery, "content")
    val htmlFormatter = new SimpleHTMLFormatter("<span style=\"backgroud:red\">", "</span>")
    val highlighter = new Highlighter(htmlFormatter, scorer)
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer))
    */
    // val documents = scoreDocs.to(LazyList).map(sd => indexSearcher.doc(sd.doc, Set("email").asJava))
    //documents
  }

  import scala.jdk.CollectionConverters._
  def getDoc(docId: Int): Document = indexSearcher.doc(docId, Set("email").asJava)

  import akka.stream.scaladsl._
  def getEmails(keyWord: String, n: Int)(implicit system: ActorSystem): Source[String, NotUsed] = {
    import system.dispatcher
    Source.fromIterator(() => getScoreDocs(keyWord, n))
      .mapAsync(5)(doc => Future(getDoc(doc.doc).get("email")))
      .via(Deduplicate())
  }
  //println(getScoreDocs("a").map(_.get("email")).toSet.size)


}