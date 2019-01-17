package moodify.repository

import moodify.core.ClientCredentials
import moodify.helper.Converter
import moodify.model.SimpleArtist
import moodify.service.RedisService

object ArtistRepository {

  /**
    * TTL for artist data in Redis, set as 15 days.
    */
  private val artistRedisTTL: Int = 15 * 86400

  /**
    * Get Redis key for given artist.
    *
    * @param artistId Artist's Spotify ID.
    * @return Redis key.
    */
  private def artistRedisKey(artistId: String): String = s"artist:$artistId"

  /**
    * Get SimpleArtist data for given `artistId`.
    *
    * @param artistId Artist's Spotify ID.
    * @return SimpleArtist
    */
  def getSimpleArtist(artistId: String): SimpleArtist = {
    val maybeSimpleArtist = RedisService.hgetall(artistRedisKey(artistId))
    if (maybeSimpleArtist.isDefined) {
      Converter.mapToSimpleArtist(maybeSimpleArtist.get)
    } else {
      val spotify = ClientCredentials.getAuthorizedSpotifyService
      val artist = spotify.getArtist(artistId)
      val simpleArtist = Converter.artistToSimpleArtist(artist)
      setSimpleArtist(simpleArtist)
      simpleArtist
    }
  }

  /**
    * Saves given `simpleArtist` data in Redis.
    *
    * @param simpleArtist SimpleArtist
    */
  def setSimpleArtist(simpleArtist: SimpleArtist): Unit = {
    val key = artistRedisKey(simpleArtist.id)
    val map = Converter.simpleArtistToMap(simpleArtist)
    RedisService.hmset(key, map, artistRedisTTL)
  }

}
