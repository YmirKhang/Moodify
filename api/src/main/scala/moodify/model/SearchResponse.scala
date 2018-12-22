package moodify.model

import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class SearchResponse(name: String, id: String, itemType: String, extra: Option[String] = None)

object SearchResponseProtocol extends DefaultJsonProtocol {
  implicit val searchResponseFormat: RootJsonFormat[SearchResponse] = jsonFormat4(SearchResponse)
}
