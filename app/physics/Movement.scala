package physics

import play.api.libs.json.{JsObject, Json}

/**
  * Created by alex on 22-Oct-16.
  * Describes a movement based oncomponents x and y
  */
class Movement(val x: Float, val y: Float, val speed: Double) {

  def speed(newSpeed: Double): Movement = Movement(x, y, newSpeed)

  def x(newX: Float): Movement = Movement(newX, y, speed)

  def y(newY: Float): Movement = Movement(x, newY, speed)

  def toJson = try {
    val obj: JsObject = Json.obj("x" -> x, "y" -> y, "speed" -> speed)
    Option(obj)
  } catch {
    case ex: Exception => None
  }
}

object Movement {
  def apply(x: Float, y: Float, speed: Double): Movement = new Movement(x, y, speed)
}