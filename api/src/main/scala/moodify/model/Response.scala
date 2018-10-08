package moodify.model

import moodify.model.AnyJsonProtocol._
import spray.json.DefaultJsonProtocol._
import spray.json._

class Response(success: Boolean, data: Any = "") {

  /**
    * Convert response to JSON.
    *
    * @return Response as JSON string.
    */
  def toJson: String = {
    val response = Map(
      "success" -> success,
      "data" -> data
    )

    response.toJson.compactPrint
  }

}
