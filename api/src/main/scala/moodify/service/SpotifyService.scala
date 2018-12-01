package moodify.service

import java.net.URI

import com.typesafe.scalalogging.LazyLogging
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import com.wrapper.spotify.model_objects.specification.{Artist, PlayHistory, Track, TrackSimplified}
import moodify.Config
import moodify.model.{TimeRange, TrackFeatures, Trendline}

/**
  * Communicates with Spotify API for authenticated user.
  */
class SpotifyService extends Config with LazyLogging {

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
      case exception: Throwable =>
        logger.error(exception.getMessage)
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

    val credentialsRefreshToken = credentials.getRefreshToken
    val newRefreshToken = if (credentialsRefreshToken != null) credentialsRefreshToken else refreshToken
    val newAccessToken = credentials.getAccessToken

    spotifyApi.setAccessToken(newAccessToken)
    spotifyApi.setRefreshToken(newRefreshToken)

    val renewedCredentials = new AuthorizationCodeCredentials.Builder()
      .setAccessToken(newAccessToken)
      .setRefreshToken(newRefreshToken)
      .setExpiresIn(credentials.getExpiresIn)
      .build()

    renewedCredentials
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
      spotifyApi.getAudioFeaturesForSeveralTracks(idList: _*)
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

  /**
    * Get recent tracks of current user.
    *
    * @param limit Maximum number of tracks to return.
    * @return Recent tracks.
    */
  def getRecentTracks(limit: Int): Array[PlayHistory] = {
    val recentTracks = spotifyApi
      .getCurrentUsersRecentlyPlayedTracks
      .limit(limit)
      .build
      .execute
      .getItems

    recentTracks
  }

  /**
    * Get top artists for current user.
    *
    * @param timeRange Time range for operation.
    * @param limit     Number of artists.
    * @return Top artists.
    */
  def getTopArtists(timeRange: TimeRange.Value, limit: Int): Array[Artist] = {
    val topArtists = spotifyApi
      .getUsersTopArtists
      .limit(limit)
      .time_range(timeRange.toString)
      .build
      .execute
      .getItems

    topArtists
  }

  /**
    * Get top tracks for current user.
    *
    * @param timeRange Time range for operation.
    * @param limit     Number of tracks.
    * @return Top tracks.
    */
  def getTopTracks(timeRange: TimeRange.Value, limit: Int): Array[Track] = {
    val topTracks = spotifyApi
      .getUsersTopTracks
      .limit(limit)
      .time_range(timeRange.toString)
      .build
      .execute
      .getItems

    topTracks
  }

  /**
    * Get recommendations for given seeds and audio features.
    *
    * @param seedArtists Comma separated list of artists ids.
    * @param seedTracks  Comma separated list of track ids.
    * @param limit       Number of tracks to recommend.
    * @return
    */
  def getRecommendations(seedArtists: String,
                         seedTracks: String,
                         limit: Int,
                         acousticness: Double = -1,
                         instrumentalness: Double = -1,
                         speechiness: Double = -1,
                         danceability: Double = -1,
                         liveness: Double = -1,
                         energy: Double = -1,
                         valence: Double = -1): Array[TrackSimplified] = {

    var request = spotifyApi
      .getRecommendations
      .limit(limit)
      .seed_artists(seedArtists)
      .seed_tracks(seedTracks)

    val recommendations = request
      .build
      .execute
      .getTracks

    recommendations
  }

}
