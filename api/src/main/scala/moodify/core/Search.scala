package moodify.core

import com.typesafe.scalalogging.LazyLogging
import moodify.enumeration.ItemType
import moodify.helper.Utils
import moodify.model.SearchResponse
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

    if (Utils.isNonEmptyResult(artists)) {
      result ++= artists.getItems.map(artist =>
        SearchResponse(
          artist.getName,
          artist.getId,
          ItemType.ARTIST,
          Some(Utils.getSquareImageURL(artist.getImages))
        )
      )
    }

    if (Utils.isNonEmptyResult(tracks)) {
      result ++= tracks.getItems.map(track =>
        SearchResponse(
          track.getName,
          track.getId,
          ItemType.TRACK,
          Some(Utils.getSquareImageURL(track.getAlbum.getImages)),
          Some(track.getArtists.head.getName)
        )
      )
    }

    if (Utils.isNonEmptyResult(albums)) {
      result ++= albums.getItems.map(album =>
        SearchResponse(
          album.getName,
          album.getId,
          ItemType.ALBUM,
          Some(Utils.getSquareImageURL(album.getImages))
        )
      )
    }

    if (Utils.isNonEmptyResult(playlists)) {
      result ++= playlists.getItems.map(playlist =>
        SearchResponse(
          playlist.getName,
          playlist.getId,
          ItemType.PLAYLIST,
          Some(Utils.getSquareImageURL(playlist.getImages))
        )
      )
    }

    result.toList
  }

}
