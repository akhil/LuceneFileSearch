package desktop

import java.nio.file.FileSystems

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.highlight.{Highlighter, QueryScorer, SimpleHTMLFormatter, SimpleSpanFragmenter}
import org.apache.lucene.search.{BooleanClause, IndexSearcher}
import org.apache.lucene.store.FSDirectory

object FileSearchRead {

  //val keyWord = "java AND spring"

  private val default = FileSystems.getDefault
  private val path = default.getPath("/lucene")
  private val directory = FSDirectory.open(path)
  private val directoryReader = DirectoryReader.open(directory)
  private val indexSearcher = new IndexSearcher(directoryReader)
  private val analyzer = new StandardAnalyzer()

  private val fields = Array("fileName", "content")
  private val clauses = Array(BooleanClause.Occur.SHOULD, BooleanClause.Occur.SHOULD)

  def getScoreDocs(keyWord: String, n: Int): LazyList[Document] = {
    val multiFieldQuery = MultiFieldQueryParser.parse(keyWord, fields, clauses, analyzer)
    val topDocs = indexSearcher.search(multiFieldQuery, n)
    val scoreDocs = topDocs.scoreDocs
    println("ScoreDocs: " + scoreDocs.length)
    val scorer = new QueryScorer(multiFieldQuery, "content")
    val htmlFormatter = new SimpleHTMLFormatter("<span style=\"backgroud:red\">", "</span>")
    val highlighter = new Highlighter(htmlFormatter, scorer)
    highlighter.setTextFragmenter(new SimpleSpanFragmenter(scorer))
    val documents = scoreDocs.to(LazyList).map(sd => indexSearcher.doc(sd.doc))
    documents
  }

  //println(getScoreDocs("a").map(_.get("email")).toSet.size)


}