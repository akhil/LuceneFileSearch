package stream

import akka.event.Logging
import akka.stream.ActorAttributes.SupervisionStrategy
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.compat.java8.FunctionConverters._
import scala.util.control.NonFatal

object Deduplicate {

  def apply[T, U](key: T => U, duplicateCount: Long) =
    new Deduplicate[T, U](key, duplicateCount, new java.util.HashMap[U, MutableLong]())

  def apply[T, U](key: T => U, duplicateCount: Long, registry: java.util.Map[U, MutableLong]) =
    new Deduplicate[T, U](key, duplicateCount, registry)

  def apply[T](duplicateCount: Long = Long.MaxValue,
               registry: java.util.Map[T, MutableLong] = new java.util.HashMap[T, MutableLong]()): Deduplicate[T, T] =
    Deduplicate(t => t, duplicateCount, registry)
}

/**
 * Only pass on those elements that have not been seen so far.
 *
 * '''Emits when''' the element is not a duplicate
 *
 * '''Backpressures when''' the element is not a duplicate and downstream backpressures
 *
 * '''Completes when''' upstream completes
 *
 * '''Cancels when''' downstream cancels
 */
final class Deduplicate[T, U](key: T => U, duplicateCount: Long = Long.MaxValue,
                              registry: java.util.Map[U, MutableLong] = new java.util.HashMap[U, MutableLong]())
  extends GraphStage[FlowShape[T, T]] {

  require(duplicateCount >= 2)

  val in = Inlet[T](Logging.simpleName(this) + ".in")
  val out = Outlet[T](Logging.simpleName(this) + ".out")
  override val shape = FlowShape(in, out)

  override def toString: String = "Deduplicate"

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) with OutHandler with InHandler {
      def decider = inheritedAttributes.get[SupervisionStrategy].map(_.decider).getOrElse(Supervision.stoppingDecider)

      override def onPush(): Unit = {
        try {
          val elem = grab(in)
          val counter = registry.merge(key(elem), MutableLong(1), asJavaBiFunction((old, _) => {
            pull(in)
            if(old.increment() == duplicateCount) null else old
          }))
          if(counter != null && counter.value == 1) {
            push(out, elem)
          }
        } catch {
          case NonFatal(ex) => decider(ex) match {
            case Supervision.Stop => failStage(ex)
            case _                => pull(in)
          }
        }
      }

      override def onPull(): Unit = pull(in)

      setHandlers(in, out, this)
    }
}

/**
 * [[MutableLong]] is used to avoid boxing/unboxing and also
 * to avoid [[java.util.Map#put]] operation to increment the counters in the registry.
 *
 * @param value
 */
case class MutableLong(var value: Long = 0L) {
  def increment() = {
    value += 1
    value
  }
}