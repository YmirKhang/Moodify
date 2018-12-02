package moodify.repository

import moodify.helper.Converter
import moodify.model.SimpleTrack
import moodify.service.{RedisService, SpotifyService}

object TrackRepository {

  /**
    * TTL for track data in Redis, set as 15 days.
    */
  private val trackRedisTTL: Int = 15 * 86400

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
      val spotify = new SpotifyService
      val track = spotify.getTrack(trackId)
      val simpleTrack = Converter.trackToSimpleTrack(track)
      saveSimpleTrack(simpleTrack)
      simpleTrack
    }
  }

  /**
    * Saves given `simpleTrack` data in Redis.
    *
    * @param simpleTrack SimpleTrack
    */
  def saveSimpleTrack(simpleTrack: SimpleTrack): Unit = {
    val key = trackRedisKey(simpleTrack.id)
    val map = Converter.simpleTrackToMap(simpleTrack)
    RedisService.hmset(key, map, trackRedisTTL)
  }

}
