package moodify.repository

import moodify.helper.Converter
import moodify.model.UserProfile
import moodify.service.{RedisService, SpotifyService}

object UserRepository {

  /**
    * TTL for user's data in Redis.
    */
  private val userRedisTTL: Int = 1 * 86400 // Days

  /**
    * Get Redis key for user.
    *
    * @param userId Spotify User ID.
    * @return Redis key.
    */
  private def userRedisKey(userId: String): String = s"user:$userId"

  /**
    * Get user data for given user.
    *
    * @param spotify User authorized Spotify service.
    * @param userId  Spotify User ID.
    * @return UserProfile
    */
  def getUser(spotify: SpotifyService, userId: String): UserProfile = {
    val maybeUser = RedisService.hgetall(userRedisKey(userId))
    if (maybeUser.isDefined) {
      Converter.mapToUserProfile(maybeUser.get)
    } else {
      val user = spotify.getCurrentUser
      val userProfile = UserProfile(
        userId = user.getId,
        name = user.getDisplayName,
        imageUrl = user.getImages.head.getUrl,
        countryCode = user.getCountry
      )
      setUser(userProfile, userId)
      userProfile
    }
  }

  /**
    * Saves given `userProfile` data in Redis.
    *
    * @param userProfile UserProfile
    * @param userId      Spotify User ID.
    */
  def setUser(userProfile: UserProfile, userId: String): Unit = {
    val key = userRedisKey(userId)
    val map = Converter.userProfileToMap(userProfile)
    RedisService.hmset(key, map, userRedisTTL)
  }

}
