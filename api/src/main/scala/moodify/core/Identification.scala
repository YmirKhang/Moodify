package moodify.core

import com.typesafe.scalalogging.LazyLogging
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import moodify.Config.TOKEN_TTL_MARGIN
import moodify.service.{RedisService, SpotifyService}

/**
  * Handles authentication and authorization.
  *
  * - Previously authenticated users can be authorized with stored tokens.
  * - New users are authenticated with Spotify code.
  */
object Identification extends LazyLogging {

  /**
    * UDID TTL for Redis.
    */
  private val udidTTL = 30 * 86400 // 30 days.

  /**
    * Refresh token TTL for Redis.
    */
  private val refreshTokenTTL = 30 * 86400 // 30 days.

  /**
    * Get Redis key for UDID - User Spotify ID.
    *
    * @param udid User Device identifier.
    * @return User's Spotify ID.
    */
  private def udidKey(udid: String) = s"udid:$udid"

  /**
    * Get Redis key for access token of given user.
    *
    * @param userId User's Spotify ID.
    * @return Redis key.
    */
  private def accessTokenKey(userId: String) = s"user:$userId:token:access"

  /**
    * Get Redis key for refresh token of given user.
    *
    * @param userId User's Spotify ID.
    * @return Redis key.
    */
  private def refreshTokenKey(userId: String) = s"user:$userId:token:refresh"

  /**
    * Get User Spotify ID for given UDID.
    *
    * @param udid User Device identifier.
    * @return User's Spotify ID.
    */
  def getUserId(udid: String): Option[String] = {
    RedisService.get(udidKey(udid))
  }

  /**
    * Authenticate given user with given authentication code.
    *
    * @param udid User Device identifier.
    * @param code Spotify authentication code.
    */
  def authenticate(udid: String, code: String): Boolean = {
    val spotify = new SpotifyService
    val maybeCredentials = spotify.authenticate(code)

    // If user is authenticated save credentials.
    val success = if (maybeCredentials.isDefined) {
      val userId = spotify.getCurrentUserId
      val credentials = maybeCredentials.get
      val redisSuccess = RedisService.set(udidKey(udid), userId, udidTTL)
      redisSuccess && updateCredentials(userId, credentials)
    } else false

    success
  }

  /**
    * Authorize given user.
    *
    * The user should have authorized Moodify before and must have tokens in Redis.
    *
    * @param udid User Device identifier.
    * @return Access key if exists.
    */
  def authorize(udid: String): Option[String] = {
    // Get Spotify User ID for given UDID.
    val maybeUserId = getUserId(udid)
    if (maybeUserId.isDefined) {
      val userId = maybeUserId.get
      // Check if access token exists.
      val maybeRedisAccessToken = RedisService.get(accessTokenKey(userId))
      val maybeAccessToken = {
        if (maybeRedisAccessToken.isDefined) {
          maybeRedisAccessToken
        }
        else {
          // Access token does not exist. Check if refresh token exists.
          val maybeRefreshToken = RedisService.get(refreshTokenKey(userId))
          if (maybeRefreshToken.isDefined) {
            val spotify = new SpotifyService
            val refreshToken = maybeRefreshToken.get
            val credentials = spotify.refreshAccessToken(refreshToken)
            val success = updateCredentials(userId, credentials)
            if (success) Some(credentials.getAccessToken) else None
          } else None
        }
      }

      maybeAccessToken
    } else None
  }

  /**
    * Store access token and refresh token in Redis for further usage.
    *
    * @param userId      User's Spotify ID.
    * @param credentials AuthorizationCodeCredentials
    * @return Success.
    */
  private def updateCredentials(userId: String, credentials: AuthorizationCodeCredentials): Boolean = {
    val accessToken = credentials.getAccessToken
    val refreshToken = credentials.getRefreshToken
    val accessTokenTTL = credentials.getExpiresIn - TOKEN_TTL_MARGIN

    try {
      RedisService.set(accessTokenKey(userId), accessToken, accessTokenTTL)
      RedisService.set(refreshTokenKey(userId), refreshToken, refreshTokenTTL)
      true
    }
    catch {
      case exception: Throwable =>
        logger.error(exception.getMessage)
        false
    }
  }

}
