package messages

/**
  * Created by alex on 28-Aug-16.
  */
abstract class Key

case object UP extends Key

case object DOWN extends Key

object Key extends Key {
  def unapply(key: String): Option[Key] = key match {
    case "up" => Some(UP)
    case "down" => Some(DOWN)
    case _ => None
  }
}