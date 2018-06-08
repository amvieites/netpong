package physics

import play.api.libs.json.{JsObject, Json}

/**
  * Created by alex on 03-Sep-16.
  */
class Player(val id: Long, val x: Double, val y: Double, val mov: Movement) extends Geometry {

  def x(newX: Double): Player = Player(id, newX, y, mov)

  def y(newY: Double): Player = Player(id, x, newY, mov)

  override def toString: String = s"id=$id,x=$x, y=$y, movement=$mov"

  def mov(newMov: Movement): Player = Player(id, x, y, newMov)

  def toJson = try {
    val obj: JsObject = Json.obj("id" -> id, "x" -> x, "y" -> y, "mov" -> mov.toJson)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object Player {
  def apply(id: Long, x: Double, y: Double, mov: Movement) = new Player(id, x, y, mov)
}
