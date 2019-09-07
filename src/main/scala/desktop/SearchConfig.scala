package desktop

import com.typesafe.config.ConfigFactory
import scala.jdk.CollectionConverters._

object SearchConfig {
  private lazy val config = ConfigFactory.load()
  val INDEX_PATH: String  = config.getString("index.path")
  val SCAN_PATH: List[String] = config.getStringList("scan.path").asScala.toList
}
