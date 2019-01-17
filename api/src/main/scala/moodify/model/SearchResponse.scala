package moodify.model

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class SearchResponse(
                           name: String,
                           id: String,
                           itemType: String,
                           imageUrl: Option[String] = None,
                           extra: Option[String] = None
                         )

object SearchResponseProtocol extends DefaultJsonProtocol {
  implicit val searchResponseFormat: RootJsonFormat[SearchResponse] = jsonFormat5(SearchResponse)
}
