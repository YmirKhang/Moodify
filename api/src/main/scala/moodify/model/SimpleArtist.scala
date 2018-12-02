package moodify.model

import spray.json._

case class SimpleArtist(id: String, name: String, imageUrl: String)

object SimpleArtistProtocol extends DefaultJsonProtocol {
  implicit val simpleArtistFormat: RootJsonFormat[SimpleArtist] = jsonFormat3(SimpleArtist)
}
