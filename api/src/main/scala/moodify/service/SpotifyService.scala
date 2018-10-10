package moodify.service

import java.net.URI

import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import com.wrapper.spotify.model_objects.specification.PlayHistory
import moodify.Config
import moodify.model.{TrackFeatures, Trendline}

/**
  * Communicates with Spotify API for authenticated user.
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
    * Authenticate given user code and get credentials for user.
    *
    * @param code User authentication code.
    * @return Option[AuthorizationCodeCredentials]
    */
  def authenticate(code: String): Option[AuthorizationCodeCredentials] = {
    try {
      val credentials = spotifyApi.authorizationCode(code).build.execute
      spotifyApi.setAccessToken(credentials.getAccessToken)
      spotifyApi.setRefreshToken(credentials.getRefreshToken)

      Some(credentials)
    }
    catch {
      case _: Throwable =>
        None
    }
  }

  /**
    * Authorize Spotify API with given access token.
    *
    * @param accessToken User's access token.
    */
  def authorize(accessToken: String): Unit = {
    spotifyApi.setAccessToken(accessToken)
  }

  /**
    * Refresh user's access token with given refresh token.
    *
    * @param refreshToken User's refresh token.
    * @return AuthorizationCodeCredentials
    */
  def refreshAccessToken(refreshToken: String): AuthorizationCodeCredentials = {
    spotifyApi.setRefreshToken(refreshToken)
    val credentials = spotifyApi.authorizationCodeRefresh.build.execute
    spotifyApi.setAccessToken(credentials.getAccessToken)
    spotifyApi.setRefreshToken(credentials.getRefreshToken)

    credentials
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

  /**
    * Get audio features for given track id list.
    *
    * @param trackIdList Track id list.
    * @return Tracks' audio features.
    */
  def getAudioFeatures(trackIdList: List[String]): List[TrackFeatures] = {

    // Number of tracks for a single request is limited by Spotify API.
    val requestLimit = 100
    val trackIdListGroups = trackIdList.grouped(requestLimit)

    // Get those tracks' audio features from Spotify.
    val audioFeatures = trackIdListGroups.flatMap { idList =>
      val commaSeparatedIdList = idList.mkString(",")
      spotifyApi.getAudioFeaturesForSeveralTracks()
        .ids(commaSeparatedIdList)
        .build.execute
    }.toList

    // Create each track's trendline.
    val trendlineList = audioFeatures.map { audioFeatures =>
      val trackId = audioFeatures.getId
      val trendline = Trendline(
        audioFeatures.getAcousticness.toDouble,
        audioFeatures.getInstrumentalness.toDouble,
        audioFeatures.getSpeechiness.toDouble,
        audioFeatures.getDanceability.toDouble,
        audioFeatures.getLiveness.toDouble,
        audioFeatures.getEnergy.toDouble,
        audioFeatures.getValence.toDouble
      )

      TrackFeatures(trackId, trendline)
    }

    trendlineList
  }

}
