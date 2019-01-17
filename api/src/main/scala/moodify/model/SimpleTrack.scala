package moodify.model

import moodify.model.SimpleArtistProtocol._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class SimpleTrack(id: String, name: String, imageUrl: String, artists: List[SimpleArtist])

object SimpleTrackProtocol extends DefaultJsonProtocol {
  implicit val simpleTrackFormat: RootJsonFormat[SimpleTrack] = jsonFormat4(SimpleTrack)
}
