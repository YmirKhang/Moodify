package moodify.model

import spray.json.DefaultJsonProtocol._
import spray.json._

object Response {

  /**
    * Respond success with additional data.
    *
    * @return Success response as JSON string.
    */
  def success(data: JsValue = JsObject.empty): String = {
    val response = Map(
      "success" -> JsBoolean(true),
      "data" -> data,
    )

    response.toJson.compactPrint
  }

  /**
    * Respond error with error message.
    *
    * @return Error response as JSON string.
    */
  def error(message: String = "An error occurred."): String = {
    val response = Map(
      "success" -> JsBoolean(false),
      "message" -> JsString(message)
    )

    response.toJson.compactPrint
  }

  /**
    * Respond with respect to success.
    *
    * @return Response as JSON string.
    */
  def json(success: Boolean, data: JsValue = JsObject.empty, message: String = "An error occurred."): String = {
    if (success) this.success(data) else error(message)
  }

}
