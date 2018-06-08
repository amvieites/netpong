package game

/**
  * Created by alex on 02-Oct-16.
  */
trait GameState {
  def name: String

  override def toString: String = this.name
}

object AWAITING_PLAYERS extends GameState {
  override def name: String = "AWAITING_PLAYERS"
}

object PLAYING extends GameState {
  override def name: String = "PLAYING"
}

object COUNTDOWN extends GameState {
  override def name: String = "COUNTDOWN"
}

object SCORED extends GameState {
  override def name: String = "SCORED"
}

object FINAL_STAGE extends GameState {
  override def name: String = "FINAL_STAGE"
}
