package physics

import play.api.libs.json.{JsObject, Json}

/**
  * Created by alex on 30-Sep-16.
  */
class Ball(val x: Double, val y: Double, val mov: Movement) extends Geometry {

  def x(newX: Int): Ball = Ball(newX, y, mov)

  def y(newY: Int): Ball = Ball(x, newY, mov)

  override def toString: String = s"x=$x, y=$y, movement=$mov"

  def mov(newMov: Movement): Ball = Ball(x, y, newMov)

  def stop() = Ball(this.x, this.y, Movement(0, 0, 0))

  def toJson = try {
    val obj: JsObject = Json.obj("x" -> x, "y" -> y, "mov" -> mov.toJson)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object Ball {
  def apply(x: Double, y: Double, mov: Movement): Ball = new Ball(x, y, mov)
}


