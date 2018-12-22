package moodify.model

import com.neovisionaries.i18n.CountryCode
import moodify.model.CountryCodeProtocol._
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class UserProfile(userId: String, name: String, imageUrl: String, countryCode: CountryCode)

object UserProfileProtocol extends DefaultJsonProtocol {
  implicit val userProfileFormat: RootJsonFormat[UserProfile] = jsonFormat4(UserProfile)
}
