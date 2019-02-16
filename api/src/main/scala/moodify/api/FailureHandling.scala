package moodify.api

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, NotFound}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, respondWithHeaders}
import akka.http.scaladsl.server.{ExceptionHandler, MissingQueryParamRejection, RejectionHandler, ValidationRejection}
import com.typesafe.scalalogging.LazyLogging
import moodify.Config.ENVIRONMENT
import moodify.helper.HTTPHelper

/**
  * Handles API failures such as validation rejection, missing query parameters, invalid route etc.
  */
trait FailureHandling extends LazyLogging {

  private val headers: List[RawHeader] = HTTPHelper.getHeaders(ENVIRONMENT)

  implicit def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle {
        case ValidationRejection(response, _) =>
          respondWithHeaders(headers) {
            complete((BadRequest, response))
          }

        case MissingQueryParamRejection(parameterName) =>
          respondWithHeaders(headers) {
            complete(
              (BadRequest, Response.error(s"Missing parameter: $parameterName"))
            )
          }

        case _ =>
          respondWithHeaders(headers) {
            complete(
              (BadRequest, Response.error("Bad request."))
            )
          }
      }
      .handleNotFound {
        respondWithHeaders(headers) {
          complete((NotFound, Response.error("Invalid request.")))
        }
      }
      .result()

  implicit def exceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case exception: Throwable =>
        logger.error("Global Exception Handler", exception)
        val response = Response.error("An error occurred while processing the request.")
        respondWithHeaders(headers) {
          complete(HttpResponse(InternalServerError, entity = response))
        }
    }

}
