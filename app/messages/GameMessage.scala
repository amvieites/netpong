package messages

import akka.actor.ActorRef
import game.GameState
import physics.{Ball, Player}
import play.api.libs.json.{JsObject, Json}

/**
  * Created by alex on 28-Aug-16.
  */
trait GameMessage

case class InitGame(p1Id: Long, p2Id: Long)

case class Countdown(msgType: String = "COUNTDOWN", secsToStart: Short) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "seconds" -> secsToStart)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

case class GameStart(msgType: String = "GAME_START", msg: String) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "msg" -> msg)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

case class GameEnd(msgType: String = "GAME_END", msg: String) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "msg" -> msg)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

case class GameStateMessage(msgType: String = "GAME_STATE", state: GameState) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "state" -> state.toString)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

case class PlayerReady(id: Long, ready: Boolean) extends GameMessage

case class ClientLeft(id: Long) extends GameMessage

case class NewClient(id: Long, playerActor: ActorRef) extends GameMessage

case class KeyStrokeUpdate(key: Key, event: KeyEvent, millis: Long) extends GameMessage

object KeyStrokeUpdate extends GameMessage {
  def fromMap(values: Map[String, String]) = try {
    Some(KeyStrokeUpdate(Key.unapply(values("key")).get, KeyEvent.unapply(values("event")).get, values("millis").toLong))
  } catch {
    case ex: Exception => None
  }
}

case class PlayerKeyStrokeUpdate(pId: Long, key: Key, event: KeyEvent) extends GameMessage

case class GameUpdate(msgType: String = "GAME_UPDATE", frameId: Long, time: Long, me: Player, him: Player, ball: Ball) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "frameId" -> frameId, "time" -> time, "me" -> me.toJson, "him" -> him.toJson, "ball" -> ball.toJson)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object GameUpdate extends GameMessage {
  def apply(frameId: Long, time: Long, me: Player, him: Player, ball: Ball) = new GameUpdate("GAME_UPDATE", frameId, time, me, him, ball)
}

/** Starts a point to be played. Each time a player scores, the point ends and begins another one. */
case class PointStart(msgType: String = "POINT_START", pointNumber: Short) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "pointNumber" -> pointNumber)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object PointStart extends GameMessage {
  def apply(pointNumber: Short) = new PointStart(pointNumber = pointNumber)
}

case class PlayerScored(msgType: String = "PLAYER_SCORED", me: Int, him: Int) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> msgType, "me" -> me, "him" -> him)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object PlayerScored extends GameMessage {
  def apply(me: Short, him: Short) = new PlayerScored(me = me, him = him)
}

case class Welcome(playerNumber: Int, shit: String) extends GameMessage {
  def toJson = try {
    val obj: JsObject = Json.obj("msgType" -> "WELCOME", "player_no" -> playerNumber)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object Welcome extends GameMessage {
  def apply(playerNumber: Int) = new Welcome(playerNumber, "")
}