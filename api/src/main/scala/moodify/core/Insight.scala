package moodify.core

import moodify.helper.Converter
import moodify.model._
import moodify.repository.{ArtistRepository, TrackRepository}
import moodify.service.{RedisService, SpotifyService}

/**
  * Processes user's Spotify data and retrieves insights about user's behaviour.
  *
  * @param spotify Authenticated Spotify service.
  * @param userId  Spotify User ID.
  */
class Insight(spotify: SpotifyService, userId: String) {

  /**
    * Time to live for user's trendline data in Redis.
    */
  private val userTrendlineTTL = 5 * 60 // 5 minutes.

  /**
    * Get top artists of current user.
    *
    * @param timeRange Time range for operation.
    * @param limit     Number of artists.
    * @return Top artists.
    */
  def getTopArtists(timeRange: TimeRange.Value, limit: Int): List[SimpleArtist] = {
    val userRedisKey = s"user:$userId:top:artist:$timeRange"

    // Get user's top artist id list from Redis. If size is enough get artist data and return.
    val maybeTopArtistIdList = RedisService.lrange(userRedisKey, size = limit)
    if (maybeTopArtistIdList.isDefined) {
      val topArtistIdList = maybeTopArtistIdList.get
        .map(maybeArtistId => maybeArtistId.getOrElse(""))
        .filter(artistId => artistId.nonEmpty)

      if (topArtistIdList.length == limit) {
        val simpleArtistList = topArtistIdList.map(artistId => ArtistRepository.getSimpleArtist(artistId))
        return simpleArtistList
      }
    }

    // Redis does not hold required data. Get top artists from Spotify.
    val topArtists = spotify.getTopArtists(timeRange, limit).toList
    val topSimpleArtists = topArtists.map(artist => Converter.artistToSimpleArtist(artist))
    topSimpleArtists.foreach(simpleArtist => ArtistRepository.setSimpleArtist(simpleArtist))


    topSimpleArtists
  }

  /**
    * Get top tracks of current user.
    *
    * @param timeRange Time range for operation.
    * @param limit     Number of tracks.
    * @return Top tracks.
    */
  def getTopTracks(timeRange: TimeRange.Value, limit: Int): List[SimpleTrack] = {
    val userRedisKey = s"user:$userId:top:track:$timeRange"

    // Get user's top track id list from Redis. If size is enough get track data and return.
    val maybeTopTrackIdList = RedisService.lrange(userRedisKey, size = limit)
    if (maybeTopTrackIdList.isDefined) {
      val topTrackIdList = maybeTopTrackIdList.get
        .map(maybeTrackId => maybeTrackId.getOrElse(""))
        .filter(trackId => trackId.nonEmpty)

      if (topTrackIdList.length == limit) {
        val simpleTrackList = topTrackIdList.map(trackId => TrackRepository.getSimpleTrack(trackId))
        return simpleTrackList
      }
    }

    // Redis does not hold required data. Get top tracks from Spotify.
    val topTracks = spotify.getTopTracks(timeRange, limit).toList
    val topSimpleTracks = topTracks.map(track => Converter.trackToSimpleTrack(track))
    topSimpleTracks.foreach(simpleTrack => TrackRepository.setSimpleTrack(simpleTrack))

    topSimpleTracks
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
    val recentTracks = spotify.getRecentTracks(numTracks)
    val recentTracksIdList = recentTracks.map(track => track.getTrack.getId).toList
    val trackFeatureList = TrackRepository.getAudioFeatures(spotify, recentTracksIdList)

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

}
