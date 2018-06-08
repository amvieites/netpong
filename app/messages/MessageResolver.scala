package messages

import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.collection.Map

/**
  * Created by alex on 27-Aug-16.
  */
class MessageResolver {

  def parse(msg: String): Option[GameMessage] = {
    Json.parse(msg) match {
      case JsObject(a) => a get "msgType" match {
        case Some(typeValue) => typeValue match {
          case JsString(strType) => strType match {
            case "KEY_STROKE_UPDATE" => parseKeyStrokeUpdate(a)
          }
          case _ => None
        }
        case _ => None
      }
      case _ => None
    }
  }

  def parseKeyStrokeUpdate(ksUpdate: Map[String, JsValue]): Option[GameMessage] = {
    val stringToValue: Map[String, String] = for {
      (k, v) <- ksUpdate
    } yield (k, v match {
      case JsString(str) => str;
      case _ => ""
    })

    KeyStrokeUpdate.fromMap(stringToValue.toMap)
  }
}