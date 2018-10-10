package moodify.core

import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import moodify.service.{RedisService, SpotifyService}

/**
  * Handles authentication and authorization.
  *
  * - Previously authenticated users can be authorized with stored tokens.
  * - New users are authenticated with Spotify code.
  */
object Identification {

  /**
    * Get Redis key for access token of given user.
    *
    * @param userId Moodify user id.
    * @return Redis key.
    */
  private def accessTokenKey(userId: String) = s"user:$userId:token:access"

  /**
    * Get Redis key for refresh token of given user.
    *
    * @param userId Moodify user id.
    * @return Redis key.
    */
  private def refreshTokenKey(userId: String) = s"user:$userId:token:refresh"

  /**
    * Authenticate given user with given authentication code.
    *
    * @param userId Moodify user id.
    * @param code   Spotify authentication code.
    */
  def authenticate(userId: String, code: String): Boolean = {
    val spotify = new SpotifyService
    val maybeCredentials = spotify.authenticate(code)

    // If user is authenticated save credentials.
    val success = if (maybeCredentials.isDefined) {
      val credentials = maybeCredentials.get
      updateCredentials(userId, credentials)
    } else false

    success
  }

  /**
    * Authorize given user.
    *
    * The user should have authorized Moodify before and must have tokens in Redis.
    *
    * @param userId Moodify user id.
    * @return Access key if exists.
    */
  def authorize(userId: String): Option[String] = {
    val spotify = new SpotifyService

    // Check if access token exists.
    val maybeRedisAccessToken = RedisService.get(accessTokenKey(userId))
    val maybeAccessToken = if (maybeRedisAccessToken.isDefined) {
      maybeRedisAccessToken
    }
    // Access token does not exist. Check if refresh token exists.
    else {
      val maybeRefreshToken = RedisService.get(refreshTokenKey(userId))
      if (maybeRefreshToken.isDefined) {
        val refreshToken = maybeRefreshToken.get
        val credentials = spotify.refreshAccessToken(refreshToken)
        val success = updateCredentials(userId, credentials)
        if (success) Some(credentials.getAccessToken) else None
      } else None
    }

    maybeAccessToken
  }

  /**
    * Store access token and refresh token in Redis for further usage.
    *
    * @param userId      Moodify user id.
    * @param credentials AuthorizationCodeCredentials
    * @return Success.
    */
  private def updateCredentials(userId: String, credentials: AuthorizationCodeCredentials): Boolean = {
    val accessToken = credentials.getAccessToken
    val refreshToken = credentials.getRefreshToken
    val accessTokenTTL = credentials.getExpiresIn
    val refreshTokenTTL = 30 * 86400 // 30 days.

    try {
      RedisService.set(accessTokenKey(userId), accessToken, accessTokenTTL)
      RedisService.set(refreshTokenKey(userId), refreshToken, refreshTokenTTL)
      true
    }
    catch {
      case _: Throwable => false
    }
  }

}
