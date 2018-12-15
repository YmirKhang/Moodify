package moodify.repository

import moodify.core.ClientCredentials
import moodify.helper.Converter
import moodify.model.{SimpleTrack, TrackFeatures}
import moodify.service.RedisService

object TrackRepository {

  /**
    * TTL for track data in Redis, set as 15 days.
    */
  private val trackRedisTTL: Int = 15 * 86400

  /**
    * Time to live for track's audio features in Redis.
    */
  private val trackAudioRedisTTL = 30 * 86400 // 30 days.

  /**
    * Redis key for track's audio features.
    *
    * @param trackId Track ID.
    * @return Redis key.
    */
  private def trackAudioRedisKey(trackId: String) = s"track:$trackId:audio"

  /**
    * Get Redis key for given track.
    *
    * @param trackId Track's Spotify ID.
    * @return Redis key.
    */
  private def trackRedisKey(trackId: String): String = s"track:$trackId"

  /**
    * Get SimpleTrack data for given `trackId`.
    *
    * @param trackId Track's Spotify ID.
    * @return SimpleTrack
    */
  def getSimpleTrack(trackId: String): SimpleTrack = {
    val maybeSimpleTrack = RedisService.hgetall(trackRedisKey(trackId))
    if (maybeSimpleTrack.isDefined) {
      Converter.mapToSimpleTrack(maybeSimpleTrack.get)
    } else {
      val spotify = ClientCredentials.getAuthorizedSpotifyService
      val track = spotify.getTrack(trackId)
      val simpleTrack = Converter.trackToSimpleTrack(track)
      setSimpleTrack(simpleTrack)
      simpleTrack
    }
  }

  /**
    * Saves given `simpleTrack` data in Redis.
    *
    * @param simpleTrack SimpleTrack
    */
  def setSimpleTrack(simpleTrack: SimpleTrack): Unit = {
    val key = trackRedisKey(simpleTrack.id)
    val map = Converter.simpleTrackToMap(simpleTrack)
    RedisService.hmset(key, map, trackRedisTTL)
  }

  /**
    * Get audio features for given track id list.
    *
    * First check Redis for given track ids. Then, query Spotify API for remaining tracks.
    *
    * @param trackIdList Track id list.
    * @return Tracks' audio features.
    */
  def getAudioFeatures(trackIdList: List[String]): List[TrackFeatures] = {
    // Fetch audio features from Redis, if available.
    val redisTrackFeaturesList = trackIdList.flatMap { trackId =>
      val redisKey = trackAudioRedisKey(trackId)
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
    val spotify = ClientCredentials.getAuthorizedSpotifyService
    val spotifyTrackFeatureList = spotify.getAudioFeatures(newTrackIdList)

    // Save new tracks' audio features to Redis for future reference.
    spotifyTrackFeatureList.foreach { trackFeatures =>
      val trackId = trackFeatures.trackId
      val redisKey = trackAudioRedisKey(trackId)
      val trendlineMap = Converter.trendlineToMap(trackFeatures.trendline)
      RedisService.hmset(redisKey, trendlineMap, trackAudioRedisTTL)
    }

    // TODO: This concatenation does not preserve the order of `trackIdList`. Fix this.
    redisTrackFeaturesList ++ spotifyTrackFeatureList
  }

}
