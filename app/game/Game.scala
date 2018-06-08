package game

import java.util.concurrent.TimeUnit.{MILLISECONDS, SECONDS}

import actors.GameActor
import akka.actor.{ActorSystem, Cancellable}
import messages._
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by alex on 28-Aug-16.
  */
class Game(val id: String, val config: GameConfig, actorSystem: ActorSystem) {

  val gameManager = actorSystem.actorOf(GameActor.props(this), "game" + id)

  var physicsCancelHook: Option[Cancellable] = None
  var clientUpdateCancelHook: Option[Cancellable] = None
  var pointNumber = 0

  def sendMessage(message: GameMessage): Unit = ???

  def playerScored() = {
    stopPoint()
    startPoint(2)
  }

  def startPoint(delaySecs: Short): Unit = {
    actorSystem.scheduler.scheduleOnce(Duration.create(0 + delaySecs, SECONDS), gameManager, Countdown(secsToStart = 3))
    actorSystem.scheduler.scheduleOnce(Duration.create(1 + delaySecs, SECONDS), gameManager, Countdown(secsToStart = 2))
    actorSystem.scheduler.scheduleOnce(Duration.create(2 + delaySecs, SECONDS), gameManager, Countdown(secsToStart = 1))
    actorSystem.scheduler.scheduleOnce(Duration.create(3 + delaySecs, SECONDS), gameManager, PointStart(0))

    if (physicsCancelHook.isEmpty) {
      Logger.debug("Starting physics loop...")
      physicsCancelHook = Some(actorSystem.scheduler.schedule(Duration.create(3 + delaySecs, SECONDS), Duration.create(15, MILLISECONDS), gameManager, "calculate"))
    }
    if (clientUpdateCancelHook.isEmpty) {
      Logger.debug("Starting clients update loop...")
      clientUpdateCancelHook = Some(actorSystem.scheduler.schedule(Duration.create(3045 + delaySecs * 1000, MILLISECONDS), Duration.create(45, MILLISECONDS), gameManager, "updateClient"))
    }
  }

  def stopPoint(): Unit = {
    if (physicsCancelHook.isDefined) {
      Logger.debug("Stopping Physics loop...")
      physicsCancelHook match {
        case Some(ch) => if (ch.cancel()) Logger.debug("...Physics loop stopped.")
        else Logger.error("...couldn't stop Physics loop.")
        case None => Logger.error("...no Physics Loop Cancellable found.")
      }
      physicsCancelHook = None
    }
    if (clientUpdateCancelHook.isDefined) {
      Logger.debug("Stopping Clients Update loop...")
      clientUpdateCancelHook match {
        case Some(ch) => if (ch.cancel()) Logger.debug("...Clients Update loop stopped.")
        else Logger.error("...couldn't stop Clients Update loop.")
        case None => Logger.error("...no Clients Update loop Cancellable found.")
      }
      clientUpdateCancelHook = None
    }
  }
}

object Game {
  def apply(id: String, config: GameConfig)(implicit actorSystem: ActorSystem) = new Game(id, config, actorSystem)
}

class GameConfig(val width: Int, val height: Int, val padHeight: Int, val padWidth: Int, val fps: Int, val ballSpeed: Int, val playerSpeed: Int, val bestOf: Short) {
}