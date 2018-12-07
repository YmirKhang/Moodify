package moodify.repository

import com.neovisionaries.i18n.CountryCode
import moodify.service.{RedisService, SpotifyService}

object UserRepository {

  /**
    * TTL for user's country data in Redis.
    */
  private val userCountryRedisTTL: Int = 30 * 86400 // Days

  /**
    * TTL for user's image data in Redis.
    */
  private val userImageRedisTTL: Int = 1 * 86400 // Days

  /**
    * Get Redis key for user's country.
    *
    * @param userId User's Moodify ID.
    * @return Redis key.
    */
  private def userCountryRedisKey(userId: String): String = s"user:$userId:country"

  /**
    * Get Redis key for user's image.
    *
    * @param userId User's Moodify ID.
    * @return Redis key.
    */
  private def userImageRedisKey(userId: String): String = s"user:$userId:image"

  /**
    * Get country code for given user.
    *
    * @param spotifyService User authorized Spotify service.
    * @param userId         User's Moodify ID.
    * @return Country Code
    */
  def getCountryCode(spotifyService: SpotifyService, userId: String): CountryCode = {
    val maybeCountry = RedisService.get(userCountryRedisKey(userId))
    if (maybeCountry.isDefined) {
      CountryCode.valueOf(maybeCountry.get)
    } else {
      val countryCode = spotifyService.getCurrentUserCountryCode
      setCountryCode(userId, countryCode)
      countryCode
    }
  }

  /**
    * Get image URL for given user.
    *
    * @param spotifyService User authorized Spotify service.
    * @param userId         User's Moodify ID.
    * @return URL for user's Spotify profile picture.
    */
  def getImageUrl(spotifyService: SpotifyService, userId: String): String = {
    val maybeImageUrl = RedisService.get(userImageRedisKey(userId))
    if (maybeImageUrl.isDefined) {
      maybeImageUrl.get
    } else {
      val imageUrl = spotifyService.getCurrentUserImageUrl
      setImageUrl(userId, imageUrl)
      imageUrl
    }
  }

  /**
    * Set user's country code.
    *
    * @param userId      User's Moodify ID.
    * @param countryCode User's country code.
    * @return Success
    */
  def setCountryCode(userId: String, countryCode: CountryCode): Boolean = {
    RedisService.set(userCountryRedisKey(userId), countryCode.getAlpha2, userCountryRedisTTL)
  }

  /**
    * Set user's image url.
    *
    * @param userId   User's Moodify ID.
    * @param imageUrl User's Spotify profile image's URL.
    * @return
    */
  def setImageUrl(userId: String, imageUrl: String): Boolean = {
    RedisService.set(userImageRedisKey(userId), imageUrl, userImageRedisTTL)
  }

}
