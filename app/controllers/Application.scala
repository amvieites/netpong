package controllers

import actors.PlayerActor
import akka.actor.ActorSystem
import akka.stream.Materializer
import game.{Game, Games}
import javax.inject.Inject
import play.api.libs.streams.ActorFlow
import play.api.mvc._

class Application @Inject()(implicit system: ActorSystem,
                            action: DefaultActionBuilder,
                            parse: PlayBodyParsers, materializer: Materializer) extends ControllerHelpers {

  def index(gameId: String, bestOf: Option[String]) = action(parse.text) {
    val game: Game = Games.findOrCreate(gameId, bestOf.getOrElse("7").toShort)
    Ok(views.html.index(game.id, game.config.width, game.config.height, game.config.ballSpeed, game.config.playerSpeed))
  }

  def wsPositions(gameId: String) = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef(out => PlayerActor.props(Games.find(gameId).get, out))
  }
}