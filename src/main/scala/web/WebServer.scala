package web

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import desktop.FileSearchRead

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import scala.io.StdIn

object WebServer {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("my-system")
    implicit val materializer: Materializer = Materializer.createMaterializer(system)
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    val route = concat (
      path("hello") {
        get {
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
        }
      },
      pathSingleSlash {
        val searchHtml  = scala.io.Source.fromResource("search.html").mkString
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, searchHtml))
      },
      path("search.csv") {
        parameter(Symbol("query")) { (query) =>
          val start = System.currentTimeMillis()
          val emails = FileSearchRead.getEmails(query, 1000000).filter(_.nonEmpty).map(_.get)
          val f = emails.runFold(Set.empty[String])(_  + _)
          val emailSet = Await.result(f, Duration.Inf)
          println("====> " + emailSet.size)
          println(System.currentTimeMillis() - start)
          complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, emailSet.mkString("\n")))
        }
      }
    )

    val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)

    println(s"Server online at http://localhost:8080/")
    /*StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
      */
  }
}
