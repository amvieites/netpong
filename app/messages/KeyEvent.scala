package messages

/**
  * Created by alex on 29-Aug-16.
  */
abstract class KeyEvent

case object KeyUp extends KeyEvent

case object KeyDown extends KeyEvent

object KeyEvent extends KeyEvent {
  def unapply(event: String) = event match {
    case "keyup" => Some(KeyUp)
    case "keydown" => Some(KeyDown)
    case _ => None
  }
}
