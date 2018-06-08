package actors

import akka.actor._
import game.Game
import messages._
import play.api.Logger
import play.api.libs.json.Json


/**
  * Created by alex on 27-Aug-16.
  */
object PlayerActor {
  def props(game: Game, out: ActorRef) = Props(new PlayerActor(System.currentTimeMillis(), game, out))
}

class PlayerActor(val id: Long, game: Game, out: ActorRef) extends Actor {

  override def preStart(): Unit = game.gameManager ! NewClient(id, self)

  override def receive = {
    case "ready" => sendReadyFlag(true)
    case "notready" => sendReadyFlag(false)
    case textMsg: String => new MessageResolver().parse(textMsg) match {
      case Some(mmsg) => mmsg match {
        case ksu: KeyStrokeUpdate => game.gameManager ! PlayerKeyStrokeUpdate(id, ksu.key, ksu.event)
        case _ => Unit
      }
      case None => Logger.error("Couldn't parse " + textMsg)
    }
    case pp: GameUpdate => pp.toJson match {
      case Some(jsonpp) => out ! Json.toJson(jsonpp).toString
      case None => Unit
    }
    case ps: PointStart => ps.toJson match {
      case Some(jsonps) => out ! Json.toJson(jsonps).toString
      case None =>
    }
    case gsm: GameStateMessage => gsm.toJson match {
      case Some(jsongsm) => out ! Json.toJson(jsongsm).toString
      case None =>
    }
    case cd: Countdown => cd.toJson match {
      case Some(jsoncd) => out ! Json.toJson(jsoncd).toString
      case None =>
    }
    case w: Welcome => w.toJson match {
      case Some(jsonw) => out ! Json.toJson(jsonw).toString
      case None => Unit
    }
    case pScored: PlayerScored => pScored.toJson match {
      case Some(jsonpScored) => out ! Json.toJson(jsonpScored).toString()
      case None => Unit
    }
    case _ => Logger.error("I only handle json messages!")
  }

  def sendReadyFlag(f: Boolean) = game.gameManager ! PlayerReady(id, ready = f)

  override def aroundPostStop(): Unit = game.gameManager ! ClientLeft(id)
}
