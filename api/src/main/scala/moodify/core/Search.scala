package moodify.core

import com.typesafe.scalalogging.LazyLogging
import com.wrapper.spotify.model_objects.specification.Paging
import moodify.model.{ItemType, SearchResponse}
import moodify.repository.UserRepository
import moodify.service.SpotifyService

import scala.collection.mutable.ListBuffer

object Search extends LazyLogging {

  /**
    * Queries Spotify with given query string for given types.
    *
    * @param spotify  Spotify service.
    * @param userId   Spotify user ID for market.
    * @param rawQuery Raw query submitted from client application.
    * @param types    Comma separated type list for search such as artist, track.
    * @param limit    Number of search result for each type.
    * @return Query result in a simplified manner.
    */
  def query(spotify: SpotifyService, userId: String, rawQuery: String, types: String, limit: Int): List[SearchResponse] = {
    val userProfile = UserRepository.getUser(spotify, userId)
    val maybeMarket = Some(userProfile.countryCode)
    val queryResult = spotify.search(rawQuery, types, limit, maybeMarket)

    val result = ListBuffer[SearchResponse]()
    val artists = queryResult.getArtists
    val tracks = queryResult.getTracks
    val albums = queryResult.getAlbums
    val playlists = queryResult.getPlaylists

    if (isNonEmptyResult(artists)) {
      result ++= artists.getItems.map(artist => SearchResponse(artist.getName, artist.getId, ItemType.ARTIST))
    }

    if (isNonEmptyResult(tracks)) {
      result ++= tracks.getItems.map(track => SearchResponse(track.getName, track.getId, ItemType.TRACK, Some(track.getArtists.head.getName)))
    }

    if (isNonEmptyResult(albums)) {
      result ++= albums.getItems.map(album => SearchResponse(album.getName, album.getId, ItemType.ALBUM))
    }

    if (isNonEmptyResult(playlists)) {
      result ++= playlists.getItems.map(playlist => SearchResponse(playlist.getName, playlist.getId, ItemType.PLAYLIST))
    }

    result.toList
  }

  /**
    * Checks if given collection is empty or not.
    *
    * @param collection Collection of ItemType.
    * @return True if collection is not empty.
    */
  private def isNonEmptyResult[A <: AnyRef](collection: Paging[A]): Boolean = {
    collection != null && collection.getTotal > 0
  }

}
