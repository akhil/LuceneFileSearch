package ui

import java.io.FileWriter

import desktop.FileSearchRead
import javafx.application.{Application, Platform}
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.layout.{HBox, VBox}
import javafx.stage.FileChooser.ExtensionFilter
import javafx.stage.{FileChooser, Stage}
import org.apache.lucene.document._
import org.apache.lucene.index._

object DesktopApplication {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[DesktopApplication], args: _*)
  }
}

class DesktopApplication extends Application {

  override def start(stage: Stage): Unit = {
    val quitButton = new Button
    quitButton.setText("Quit")
    quitButton.setOnAction(_ => Platform.exit())

    val labelCount = new Label
    labelCount.setText("--")
    val root = new VBox
    root.setSpacing(10)
    root.setPadding(new Insets(25))
    root.getChildren.add(searchBox(labelCount, stage))
    root.getChildren.add(labelCount)
    root.getChildren.add(quitButton)

    val scene = new Scene(root, 600, 300)
    stage.setTitle("Search Resumes")
    stage.setScene(scene)
    stage.show()
  }

  def downloadFileBox(stage: Stage, queryText: TextField): Button = {
    val downloadEmailButton = new Button
    downloadEmailButton.setText("Download Email")
    downloadEmailButton.setOnAction{ _ =>
      val fileChooser = new FileChooser
      fileChooser.setTitle("Download Emails")
      fileChooser.setSelectedExtensionFilter(new ExtensionFilter("CSV", "*.csv"))
      val file = fileChooser.showSaveDialog(stage)
      val fw = new FileWriter(file)
      val emails = fileSearch(queryText.getText.trim).map(_.getField("email"))
        .withFilter(_ != null).map(_.stringValue()).toSet

      emails.foreach(email => fw.write(email + "\n"))

      fw.flush()
      fw.close()
    }
    downloadEmailButton
  }

  def fileSearch: String => LazyList[Document] = FileSearchRead.getScoreDocs(_, Int.MaxValue)

  def searchBox(resultCount: Label, stage: Stage): HBox = {
    val label = new Label("Query:")
    val textField = new TextField()
    val searchButton = new Button
    searchButton.setText("Search")
    searchButton.setOnAction(_ => resultCount.setText(fileSearch(textField.getText.trim).size.toString))
    val hBox = new HBox
    hBox.setSpacing(10)
    hBox.getChildren.addAll(label, textField, searchButton, downloadFileBox(stage, textField))
    hBox
  }
}
