package game

import akka.actor.ActorSystem

import scala.collection.mutable

/**
  * Created by alex on 28-Aug-16.
  */
object Games {

  var games = mutable.HashMap.empty[String, Game]

  def findOrCreate(id: String, bestOf: Short)(implicit actorSystem: ActorSystem): Game = {
    games synchronized {
      games getOrElseUpdate(id, new Game(id, new GameConfig(500, 400, 60, 10, 60, 350, 300, bestOf), actorSystem))
    }
  }

  def find(gameId: String) = games get gameId

  def remove(id: String) = games remove id
}
