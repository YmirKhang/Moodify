package moodify.core

import moodify.helper.Converter
import moodify.model.{SimpleArtist, TimeRange, TrackFeatures, Trendline}
import moodify.repository.ArtistRepository
import moodify.service.{RedisService, SpotifyService}

/**
  * Processes user's Spotify data and retrieves insights about user's behaviour.
  *
  * @param spotifyService Authenticated Spotify service.
  * @param userId         Moodify User ID.
  */
class Insight(spotifyService: SpotifyService, userId: String) {

  /**
    * Time to live for user's trendline data in Redis.
    */
  private val userTrendlineTTL = 5 * 60 // 5 minutes.

  /**
    * Time to live for track's trendline data in Redis.
    */
  private val trackFeaturesTTL = 30 * 86400 // 30 days.

  /**
    * Get top artists of current user.
    *
    * @param timeRange Time range for operation.
    * @param limit     Number of artists.
    * @return Top artists.
    */
  def getTopArtists(timeRange: TimeRange.Value, limit: Int): Array[SimpleArtist] = {
    val userRedisKey = s"user:$userId:top:artist:$timeRange"

    // Get user's top artist id list from Redis. If size is enough get artist data and return.
    val maybeTopArtistIdList = RedisService.lrange(userRedisKey, size = limit)
    if (maybeTopArtistIdList.isDefined) {
      val topArtistIdList = maybeTopArtistIdList.get
        .map(maybeArtistId => maybeArtistId.getOrElse(""))
        .filter(artistId => artistId.nonEmpty)

      if (topArtistIdList.length == limit) {
        val simpleArtistArray = topArtistIdList.map(artistId => ArtistRepository.getSimpleArtist(artistId)).toArray
        return simpleArtistArray
      }
    }

    // Redis does not hold required data. Get top artists from Spotify.
    val topArtists = spotifyService.getTopArtists(timeRange, limit)
    val topSimpleArtists = topArtists.map(artist => Converter.artistToSimpleArtist(artist))
    topSimpleArtists.foreach(simpleArtist => ArtistRepository.saveSimpleArtist(simpleArtist))


    topSimpleArtists
  }

  /**
    * Creates trendline for authenticated user using last played `numTracks` many tracks.
    *
    * @param numTracks Recently played track count to be used.
    * @return User's Trendline.
    */
  def getTrendline(numTracks: Int): Trendline = {
    val redisKey = s"user:$userId:trendline:$numTracks"

    // Get specified trendline from Redis.
    val maybeTrendline = RedisService.hgetall(redisKey)
    if (maybeTrendline.isDefined) {
      val trendline = Converter.mapToTrendline(maybeTrendline.get)
      return trendline
    }

    // Redis does not hold required data. Generate user's trendline.
    val recentTracks = spotifyService.getRecentTracks(numTracks)
    val recentTracksIdList = recentTracks.map(track => track.getTrack.getId).toList
    val trackFeatureList = getAudioFeatures(recentTracksIdList)

    val trendlineList = trackFeatureList.map(track => track.trendline)
    val zeroTrendline = Trendline(0, 0, 0, 0, 0, 0, 0)

    // Take the average of tracks' trendline.
    val avgTrendline = trendlineList.fold(zeroTrendline) { (accum, trendline) =>
      Trendline(
        accum.acousticness + trendline.acousticness / numTracks,
        accum.instrumentalness + trendline.instrumentalness / numTracks,
        accum.speechiness + trendline.speechiness / numTracks,
        accum.danceability + trendline.danceability / numTracks,
        accum.liveness + trendline.liveness / numTracks,
        accum.energy + trendline.energy / numTracks,
        accum.valence + trendline.valence / numTracks
      )
    }

    // Save calculated trendline to Redis.
    RedisService.hmset(redisKey, Converter.trendlineToMap(avgTrendline), userTrendlineTTL)

    avgTrendline
  }

  /**
    * Get audio features for given track id list.
    *
    * First check Redis for given track ids. Then, query Spotify API for remaining tracks.
    *
    * @param trackIdList Track id list.
    * @return Tracks' audio features.
    */
  private def getAudioFeatures(trackIdList: List[String]): List[TrackFeatures] = {
    // Fetch audio features from Redis, if available.
    val redisTrackFeaturesList = trackIdList.flatMap { trackId =>
      val redisKey = s"track:$trackId:audio"
      val maybeAudioFeatures = RedisService.hgetall(redisKey)
      if (maybeAudioFeatures.isDefined) {
        val audioFeatures = maybeAudioFeatures.get
        val trendline = Converter.mapToTrendline(audioFeatures)
        List(TrackFeatures(trackId, trendline))
      }
      // Audio features for current track is not available in Redis.
      else List()
    }

    // Separate tracks with no audio feature data.
    val redisTrackIdList = redisTrackFeaturesList.map(track => track.trackId)
    val newTrackIdList = trackIdList.diff(redisTrackIdList)

    // Get new tracks' audio features from Spotify.
    val spotifyTrackFeatureList = spotifyService.getAudioFeatures(newTrackIdList)

    // Save new tracks' audio features to Redis for future reference.
    spotifyTrackFeatureList.foreach { trackFeatures =>
      val trendlineMap = Converter.trendlineToMap(trackFeatures.trendline)
      RedisService.hmset(trackFeatures.trackId, trendlineMap, trackFeaturesTTL)
    }

    // TODO: This concatenation does not preserve the order of `trackIdList`. Fix this.
    redisTrackFeaturesList ++ spotifyTrackFeatureList
  }

}
