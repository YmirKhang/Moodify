package moodify.service

import java.net.URI

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.specification.PlayHistory
import moodify.Config

/**
  * Communicates with Spotify API.
  */
class SpotifyService extends Config {

  /**
    * Spotify API wrapper instance.
    */
  private val spotifyApi = new SpotifyApi.Builder()
    .setClientId(SPOTIFY_CLIENT_ID)
    .setClientSecret(SPOTIFY_CLIENT_SECRET)
    .setRedirectUri(new URI(SPOTIFY_REDIRECT_URI))
    .build

  /**
    * Authenticate user with given code.
    *
    * @param userId Moodify user id.
    * @param code   The code generated by Spotify for user's authentication.
    * @return Success.
    */
  def authenticate(userId: String, code: String): Boolean = {
    val redisAccessTokenKey = s"user:$userId:token:access"
    val redisRefreshTokenKey = s"user:$userId:token:refresh"
    val refreshTokenTTL = 30 * 24 * 60 * 60 // 30 days.

    try {
      // Check if access token exists.
      val maybeAccessToken = RedisService.get(redisAccessTokenKey)
      if (maybeAccessToken.isDefined) {
        val accessToken = maybeAccessToken.get
        spotifyApi.setAccessToken(accessToken)
      }
      // Access token does not exist. Check if refresh token exists.
      else {
        val maybeRefreshToken = RedisService.get(redisRefreshTokenKey)
        if (maybeRefreshToken.isDefined) {
          val refreshToken = maybeRefreshToken.get
          spotifyApi.setRefreshToken(refreshToken)
        }

        // Get new credentials from Spotify.
        val credentials = spotifyApi.authorizationCode(code).build.execute
        val accessToken = credentials.getAccessToken
        val refreshToken = credentials.getRefreshToken
        val accessTokenTTL = credentials.getExpiresIn

        // Update access token and refresh token.
        spotifyApi.setAccessToken(accessToken)
        spotifyApi.setRefreshToken(refreshToken)
        RedisService.set(redisAccessTokenKey, accessToken, accessTokenTTL)
        RedisService.set(redisRefreshTokenKey, refreshToken, refreshTokenTTL)
      }

      true
    }
    catch {
      case exception: Exception =>
        println(exception.getMessage)
        false
    }
  }

  /**
    * Get recent tracks of current user.
    *
    * @param limit Maximum number of tracks to return.
    * @return Recent tracks.
    */
  def getRecentTracks(limit: Int): Array[PlayHistory] = {
    val recentTracks = spotifyApi.getCurrentUsersRecentlyPlayedTracks
      .limit(limit)
      .build
      .execute
      .getItems

    recentTracks
  }

}
