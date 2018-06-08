package actors

import akka.actor.{Actor, ActorRef, Props}
import game._
import messages._
import physics.{Ball, Movement, Player}
import play.api.Logger

import scala.collection.mutable

/**
  * Created by alex on 29-Aug-16.
  */
object GameActor {
  def props(game: Game) = Props(new GameActor(game))
}

class GameActor(game: Game) extends Actor {

  val keys = mutable.Map[Long, mutable.Map[Key, Long]]()
  val playerFlows: mutable.Map[Long, ActorRef] = mutable.Map[Long, ActorRef]()
  val spectatorFlows = mutable.Map[Long, ActorRef]()
  var readyList = List[(Long, Boolean)]()
  var ball: Ball = _
  var p1: Player = _
  var p2: Player = _
  var score: Map[Long, Short] = _
  var lastUpdate: Long = 0
  var state: GameState = _
  var calculateTimes: Long = _
  var frameId: Long = _

  override def receive = {
    case "calculate" => if (state == PLAYING) calculate()
    case "updateClient" => if (state == PLAYING) sendModel()
    case gsm: GameStateMessage => playerFlows.values.foreach(_ ! gsm)
    case cd: Countdown => state = COUNTDOWN; playerFlows.values.foreach(_ ! cd)
    case ps: PointStart => pointStart(ps)
    case PlayerReady(pId, r) => playerReady(pId, r)
    case ClientLeft(pId) => clientLeft(pId)
    case NewClient(pId, playerActor) => clientJoin(pId, playerActor)
    case ksu: PlayerKeyStrokeUpdate => if (state == PLAYING) handle(ksu)
    case pScored: PlayerScored => playerFlows.values.foreach(_ ! pScored)
    case _ =>
  }

  def clientLeft(pId: Long): Unit = {
    if (p1 != null && pId == p1.id || p2 != null && pId == p2.id) {
      playerLeft(pId)
    } else {
      spectatorFlows remove pId
    }
  }

  def playerLeft(pId: Long): Unit = {
    playerFlows.remove(pId)

    if ((playerFlows.size + spectatorFlows.size) == 0) {
      Games.remove(game.id)
      context stop self
    } else {
      stopGame()
    }
  }

  def stopGame(): Unit = {
    state = AWAITING_PLAYERS
    game.stopPoint()
    (playerFlows.values ++ spectatorFlows.values) foreach (_ ! GameStateMessage(state = state))
    prepare()
  }

  def clientJoin(pId: Long, playerActor: ActorRef): Unit = {
    spectatorFlows update(pId, playerActor)
  }

  def playerReady(pId: Long, r: Boolean) = {
    if (state == AWAITING_PLAYERS) {
      ready(pId, r)
      spectatorFlows remove pId match {
        case Some(ref) =>
          playerFlows update(pId, ref)
          score = score updated (pId, 0.toShort)
          ref ! Welcome(playerFlows.size)
        case None => Logger.error("WTF, player " + pId + " not in spectators list" )
      }
    }
  }

  def ready(playerId: Long, ready: Boolean) = {
    Logger.debug("Player " + playerId + " is ready")
    readyList = (playerId, ready) :: readyList

    if (readyList.size == 2 && readyList.forall(c => c._2)) {
      score += (playerId -> 0)
      game.startPoint(0)
    }
  }

  def calculate(): Unit = {
    val now: Long = System.currentTimeMillis()
    val dt: Double = (now - lastUpdate) / 1000.000
    lastUpdate = now
    newModel(dt)
    calculateTimes += 1
  }

  def newModel(dt: Double): Unit = {
    frameId += 1
    p1 = updatePlayer(dt, processPlayerInput(p1))
    p2 = updatePlayer(dt, processPlayerInput(p2))
    ball = updateBall(dt, ball)
  }

  def updatePlayer(delta: Double, p: Player): Player = {
    limit(game.config, p.y(p.y + p.mov.y * p.mov.speed * delta))
  }

  def limit(config: GameConfig, player: Player): Player = {
    Player(player.id, Math.min(Math.max(0, player.x), config.width),
      Math.min(Math.max(0, player.y), config.height - config.padHeight), player.mov)
  }

  def processPlayerInput(p: Player): Player = {
    // Process movement input
    val playerKeys = keys(p.id)
    val movY = if (playerKeys.nonEmpty) {
      val key = playerKeys.foldLeft(playerKeys.head)((old, current) => if (current._2 > old._2) current else old)._1
      if (key == UP) -1 else 1
    } else {
      0
    }
    p.mov(p.mov.y(movY))
  }

  def updateBall(delta: Double, ball: Ball): Ball = {
    def reboundShift(ball: Ball): Ball = {
      if (ball.y <= 0) {
        Ball(ball.x, 0, ball.mov.y(1))
      } else if (ball.y + 10 > game.config.height) {
        Ball(ball.x, game.config.height - 10, ball.mov.y(-1))
      } else ball
    }

    def collide(ball: Ball, player: Player): Option[Ball] = {
      if (ball.y >= (player.y - 10) && ball.y <= (player.y + game.config.padHeight) && Math.abs(ball.x - player.x) < 10) {
        val normalized = (ball.y - player.y) / ((player.x + game.config.padHeight - 10) - player.x)
        val angle = 0.25 * Math.PI * (2 * normalized - 1)
        if (ball.mov.x > 0) {
          Some(Ball(game.config.width - 40, ball.y, Movement(-1 * Math.cos(angle).toFloat, Math.sin(angle).toFloat, ball.mov.speed)))
        } else {
          Some(Ball(30, ball.y, Movement(1 * Math.cos(angle).toFloat, Math.sin(angle).toFloat, ball.mov.speed)))
        }
      } else None
    }

    def playerScored(p: Player): Unit = {
      state = SCORED
      score = score updated(p.id, (score(p.id) + 1).toShort)
      playerFlows(p1.id) ! PlayerScored(score(p1.id), score(p2.id))
      playerFlows(p2.id) ! PlayerScored(score(p2.id), score(p1.id))
      game.playerScored()
    }

    def checkPoint(ball: Ball): Ball = {
      if (ball.x < 0) {
        playerScored(p2)

        ball.stop()
      } else if (ball.x > game.config.width) {
        playerScored(p1)

        ball.stop()
      } else {
        ball
      }
    }

    val ballAfterMove = reboundShift(Ball(ball.x + ball.mov.x * ball.mov.speed * delta,
      ball.y + ball.mov.y * ball.mov.speed * delta, ball.mov))

    collide(ballAfterMove, p1) match {
      case Some(collidedBall) => collidedBall
      case None => collide(ballAfterMove, p2) match {
        case Some(collidedBall) => collidedBall
        case None => checkPoint(ballAfterMove)
      }
    }
  }

  def pointStart(ps: PointStart): Unit = {
    frameId = 0
    state = PLAYING
    startPoint(ps.pointNumber)
    playerFlows.values.foreach(_ ! ps)
  }

  def startPoint(point: Short): Unit = {
    keys.clear()
    val yPos: Double = game.config.height / 2 - (60 / 2)
    p1 = Player(readyList.tail.head._1, 20, yPos, Movement(0, 0, game.config.playerSpeed))
    p2 = Player(readyList.head._1, game.config.width - 30, yPos, Movement(0, 0, game.config.playerSpeed))
    ball = Ball(game.config.width / 2 - 5, game.config.height / 2 - 5, Movement(-1, 0, game.config.ballSpeed))
    keys clear()
    keys update(p1.id, mutable.Map())
    keys update(p2.id, mutable.Map())

    calculateTimes = 0
    lastUpdate = System.currentTimeMillis()
  }

  def sendModel(): Unit = {
    playerFlows(p1.id) ! new GameUpdate(frameId = frameId, time = lastUpdate, me = p1, him = p2, ball = ball)
    playerFlows(p2.id) ! new GameUpdate(frameId = frameId, time = lastUpdate, me = p2, him = p1, ball = ball)
  }

  def handle(pksu: PlayerKeyStrokeUpdate): Unit = {
    pksu match {
      case PlayerKeyStrokeUpdate(_, key, KeyDown) => keys(pksu.pId) update(key, System.currentTimeMillis())
      case PlayerKeyStrokeUpdate(_, key, KeyUp) => keys(pksu.pId) remove key
      case _ =>
    }
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = Logger.debug("Starting actor for game " + game.id)

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = Logger.debug("Stopping actor for game " + game.id)

  override def aroundPreStart(): Unit = {
    prepare()
  }

  def prepare(): Unit = {
    keys clear()
    spectatorFlows ++= playerFlows
    playerFlows clear()
    readyList = List()
    score = Map()
    state = AWAITING_PLAYERS
    calculateTimes = 0
  }
}
