package moodify.model

import com.neovisionaries.i18n.CountryCode
import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}

object CountryCodeProtocol extends DefaultJsonProtocol {

  implicit val countryCodeFormat: RootJsonFormat[CountryCode] = CountryCodeJsonFormat

  object CountryCodeJsonFormat extends RootJsonFormat[CountryCode] {

    def write(countryCode: CountryCode): JsValue = JsString(countryCode.getAlpha2)

    def read(json: JsValue): CountryCode = CountryCode.valueOf(json.toString)
  }

}
