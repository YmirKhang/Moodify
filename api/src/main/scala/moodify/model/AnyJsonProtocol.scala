package moodify.model

import spray.json._

object AnyJsonProtocol extends Serializable {

  implicit object AnyJsonFormat extends JsonFormat[Any] {

    /**
      * Write method is being used by toJson method, which converts Scala object to JsValue.
      *
      * @param anyValue Scala object to be converted
      * @return JsValue
      */
    def write(anyValue: Any): JsValue = anyValue match {
      case value: Int => JsNumber(value)
      case value: Long => JsNumber(value)
      case value: Double => JsNumber(value)
      case value: Float => JsNumber(value)
      case value: Boolean => if (value) JsTrue else JsFalse
      case value: BigDecimal => JsNumber(value)
      case value: String => JsString(value)
      case value: List[_] => JsArray(value.map {
        any: Any => write(any)
      }.toVector)
      case value: Map[_, _] => JsObject(value.map {
        case (key: String, valueAny: Any) =>
          key -> write(valueAny)
      })
      case _ => throw new Exception("Could not create JSON.")
    }

    /**
      * Read method is being used by convertTo[T] method, which converts JsValue to Scala object.
      *
      * @param jsValue JsValue to be converted
      * @return Scala object
      */
    def read(jsValue: JsValue): Any = jsValue match {
      case JsNumber(value) => if (value.toString.contains(".")) value.doubleValue() else value.longValue()
      case JsString(value) => value
      case JsTrue => true
      case JsFalse => false
      case JsArray(value) => value.map {
        js => read(js)
      }
      case JsObject(value) => value.map {
        case (key: String, js: JsValue) =>
          key -> read(js)
      }
      case _ => throw new Exception("Could not parse JSON.")
    }
  }

}
