package moodify.core

import moodify.Config.TOKEN_TTL_MARGIN
import moodify.service.{RedisService, SpotifyService}

object ClientCredentials {

  /**
    * Redis key for client credentials's access token.
    */
  private val redisKey = "application:token:access"

  /**
    * Get a Spotify Service instance with client credentials authorization.
    *
    * @return Spotify Service
    */
  def getAuthorizedSpotifyService: SpotifyService = {
    val spotify = new SpotifyService
    val accessToken = getAccessToken(spotify)
    spotify.authorize(accessToken)

    spotify
  }

  /**
    * Get access token for application using clint credentials.
    *
    * @param spotify Spotify Service
    * @return Access Token
    */
  private def getAccessToken(spotify: SpotifyService): String = {
    val maybeAccessToken = RedisService.get(redisKey)
    if (maybeAccessToken.isDefined) {
      maybeAccessToken.get
    } else {
      val clientCredentials = spotify.authenticateApp()
      val accessToken = clientCredentials.getAccessToken
      val ttl = clientCredentials.getExpiresIn - TOKEN_TTL_MARGIN
      RedisService.set(redisKey, accessToken, ttl)

      accessToken
    }
  }

}
