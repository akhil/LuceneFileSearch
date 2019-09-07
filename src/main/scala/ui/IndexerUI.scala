package ui

import desktop._
import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Button, Label}
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.apache.lucene.index._

class IndexerUI extends Application {
  import scala.concurrent._
  import ExecutionContext.Implicits.global
  val fileIndexer: FileIndexer = new FileIndexer

  override def start(stage: Stage): Unit = {
    val btn = new Button
    btn.setText("Start Index")
    btn.setMinWidth(100)
    def enableButton: IndexStatus => Unit = {
      case Indexing(i) => btn.setText(s"Indexing count $i")
      case IndexComplete(i) =>btn.setText((s"Index complete count: $i"))
    }
    btn.setOnAction {_ =>
      fileIndexer.runIndexer(List(enableButton))
      btn.setDisable(true)
    }

    val vbox = new VBox
    vbox.setSpacing(10)
    vbox.setPadding(new Insets(25))
    vbox.getChildren.add(createLabel("SCAN Location"))
    SearchConfig.SCAN_PATH.map(createLabel).foreach(vbox.getChildren.add)
    vbox.getChildren.add(createLabel(s"INDEX Location: ${SearchConfig.INDEX_PATH}"))
    vbox.getChildren.add(btn)

    val scene = new Scene(vbox, 600, 300)
    stage.setTitle("Index Resumes USE WITH CAUTION!!!")
    stage.setScene(scene)
    stage.show()
  }

  def createLabel(text: String): Label = {
    val label = new Label
    label.setText(text)
    label
  }
}
