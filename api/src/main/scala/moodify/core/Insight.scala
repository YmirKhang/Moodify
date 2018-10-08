package moodify.core

import moodify.model.Trendline
import moodify.service.{RedisService, SpotifyService}

/**
  * Processes user's Spotify data and retrieves insights about user's behaviour.
  *
  * @param spotifyService Authenticated Spotify service.
  * @param userId         Moodify User ID.
  */
class Insight(spotifyService: SpotifyService, userId: String) {

  /**
    * Creates trendline for authenticated user using last played `numTracks` many tracks.
    *
    * @param numTracks Recently played track count to be used.
    * @return User's Trendline.
    */
  def trendline(numTracks: Int): Trendline = {
    val redisTrendlineKey = s"user:$userId:trendline:$numTracks"
    val redisTTL = 5 * 60 // 5 minutes.

    // Get specified trendline from Redis.
    val maybeTrendline = RedisService.hgetall(redisTrendlineKey)
    if (maybeTrendline.isDefined) {
      val trendline = maybeTrendline.get

      return Trendline(
        trendline("Acousticness").toDouble,
        trendline("Instrumentalness").toDouble,
        trendline("Speechiness").toDouble,
        trendline("Danceability").toDouble,
        trendline("Liveness").toDouble,
        trendline("Energy").toDouble,
        trendline("Valence").toDouble
      )
    }

    // Redis does not hold required data. Generate user's trendline.
    val recentTracks = spotifyService.getRecentTracks(numTracks)
    val recentTracksIdList = recentTracks.map(track => track.getTrack.getId).toList
    val trackFeatureList = spotifyService.getAudioFeatures(recentTracksIdList)

    // TODO: Calculate mean of `trackFeatureList`.

  }

}
