package moodify.service

import java.net.URI

import com.google.gson.JsonParser
import com.neovisionaries.i18n.CountryCode
import com.typesafe.scalalogging.LazyLogging
import com.wrapper.spotify.SpotifyApi
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials
import com.wrapper.spotify.model_objects.specification._
import moodify.Config
import moodify.model.{RecommendationPreferences, TimeRange, TrackFeatures, Trendline}
import spray.json.DefaultJsonProtocol._
import spray.json._

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
    * Get current user.
    *
    * @return User
    */
  def getCurrentUser: User = {
    val user = spotifyApi
      .getCurrentUsersProfile
      .build
      .execute

    user
  }

  /**
    * Get current user's Spotify ID.
    *
    * @return User ID
    */
  def getCurrentUserId: String = {
    getCurrentUser.getId
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
    * Get artist data for given `artistId`.
    *
    * @param artistId Artist's Spotify ID.
    * @return Artist.
    */
  def getArtist(artistId: String): Artist = {
    val artist = spotifyApi
      .getArtist(artistId)
      .build
      .execute

    artist
  }

  /**
    * Get track data for given `trackId`.
    *
    * @param trackId Track's Spotify ID.
    * @return Track.
    */
  def getTrack(trackId: String): Track = {
    val track = spotifyApi
      .getTrack(trackId)
      .build
      .execute

    track
  }

  /**
    * Finds a playlist of current user by playlist's name.
    *
    * @param playlistName Name of the playlist.
    * @return Optional Playlist
    */
  def getCurrentUsersPlaylist(playlistName: String): Option[PlaylistSimplified] = {
    val maybePlaylist = spotifyApi
      .getListOfCurrentUsersPlaylists
      .build
      .execute
      .getItems
      .find(playlist => playlist.getName == playlistName)

    maybePlaylist
  }

  /**
    * Creates a playlist for current user with given playlist name.
    *
    * @param playlistName Name of the playlist.
    * @param description  Description of the playlist.
    * @param isPublic     Publicity of the playlist.
    * @return Playlist
    */
  def createPlaylist(playlistName: String, description: String, isPublic: Boolean): Playlist = {
    val playlist = spotifyApi
      .createPlaylist(getCurrentUserId, playlistName)
      .description(description)
      .public_(isPublic)
      .build
      .execute

    playlist
  }

  /**
    * Changes the description of given playlist of current user with given description text.
    *
    * @param playlistId  Spotify ID of the playlist.
    * @param description Description text.
    */
  def changePlaylistsDescription(playlistId: String, description: String): Unit = {
    spotifyApi
      .changePlaylistsDetails(getCurrentUserId, playlistId)
      .description(description)
      .build
      .execute
  }

  /**
    * Deletes all tracks of current user's given playlist.
    *
    * @param playlistId Spotify ID of the playlist.
    */
  def flushPlaylist(playlistId: String): Unit = {
    val userId = getCurrentUserId

    val playlistTracks = spotifyApi
      .getPlaylistsTracks(userId, playlistId)
      .build
      .execute
      .getItems

    val playlistTracksJsonString = playlistTracks
      .map(track => Map("uri" -> track.getTrack.getUri))
      .toList
      .toJson
      .compactPrint

    val playlistTracksJsonArray = new JsonParser()
      .parse(playlistTracksJsonString)
      .getAsJsonArray

    spotifyApi
      .removeTracksFromPlaylist(userId, playlistId, playlistTracksJsonArray)
      .build
      .execute
  }

  /**
    * Adds given tracks to current user's given playlist.
    *
    * @param playlistId   Spotify ID of the playlist.
    * @param trackUriList Spotify URI list off tracks to be added.
    */
  def addTracksToPlaylist(playlistId: String, trackUriList: Array[String]): Unit = {
    spotifyApi
      .addTracksToPlaylist(getCurrentUserId, playlistId, trackUriList)
      .build
      .execute
  }

  /**
    * Get recommendations for given seeds and audio features.
    *
    * @param preferences Preferences for recommended tracks.
    * @param limit       Number of tracks to recommend.
    * @param maybeMarket Market availability.
    * @return Recommended tracks.
    */
  def getRecommendations(preferences: RecommendationPreferences, limit: Int, maybeMarket: Option[CountryCode] = None): Array[TrackSimplified] = {
    val request = spotifyApi
      .getRecommendations
      .limit(limit)

    if (maybeMarket.isDefined) request.market(maybeMarket.get)
    if (preferences.seedArtistIdList.isDefined) request.seed_artists(preferences.seedArtistIdList.get.mkString(","))
    if (preferences.seedTrackIdList.isDefined) request.seed_tracks(preferences.seedTrackIdList.get.mkString(","))
    if (preferences.acousticness.isDefined) request.target_acousticness(preferences.acousticness.get.toFloat)
    if (preferences.instrumentalness.isDefined) request.target_instrumentalness(preferences.instrumentalness.get.toFloat)
    if (preferences.speechiness.isDefined) request.target_speechiness(preferences.speechiness.get.toFloat)
    if (preferences.danceability.isDefined) request.target_danceability(preferences.danceability.get.toFloat)
    if (preferences.liveness.isDefined) request.target_liveness(preferences.liveness.get.toFloat)
    if (preferences.energy.isDefined) request.target_energy(preferences.energy.get.toFloat)
    if (preferences.valence.isDefined) request.target_valence(preferences.valence.get.toFloat)

    val recommendations = request
      .build
      .execute
      .getTracks

    recommendations
  }

}
