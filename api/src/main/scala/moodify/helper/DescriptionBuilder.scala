package moodify.helper

import moodify.model.RecommendationPreferences
import moodify.repository.{ArtistRepository, TrackRepository}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object DescriptionBuilder {

  /**
    * Maximum allowed description length by Spotify.
    */
  private val characterLimit = 300

  /**
    * Get description for playlist with given preferences.
    *
    * @param preferences Recommendation preferences.
    * @return Playlist description.
    */
  def getDescription(preferences: RecommendationPreferences): String = {
    val seedText = getSeedText(preferences)

    val descriptionBuilder = new StringBuilder("Highly personalized with Moodify based on ")
    descriptionBuilder.append(seedText)
    descriptionBuilder.append("Check https://moodify.app for more")
    val description = descriptionBuilder.toString.take(characterLimit)

    description
  }

  /**
    * Get description text for seed artists and tracks.
    *
    * @param preferences RecommendationPreferences
    * @return Description text for audio preferences.
    */
  private def getSeedText(preferences: RecommendationPreferences): String = {
    val seedListBuffer = mutable.ListBuffer[String]()

    if (preferences.seedArtistIdList.isDefined) {
      val artistIdList = preferences.seedArtistIdList.get
      val artistNameList = artistIdList.map(artistId => ArtistRepository.getSimpleArtist(artistId).name)
      artistNameList.foreach(artist => seedListBuffer.append(artist))
    }

    if (preferences.seedTrackIdList.isDefined) {
      val seedTrackIdList = preferences.seedTrackIdList.get
      val trackNameList = seedTrackIdList.map(trackId => TrackRepository.getSimpleTrack(trackId).name)
      trackNameList.foreach(track => seedListBuffer.append(track))
    }

    val seedText = stringifyList(seedListBuffer.toList)

    seedText
  }

  /**
    * Convert given String List into a legible String.
    *
    * @param list String List
    * @return A legible String
    */
  private def stringifyList(list: List[String]): String = {
    val listSeparator = ", "
    var string = list.mkString("", listSeparator, ". ")
    if (list.length > 1) {
      val index = string.lastIndexOf(listSeparator)
      val (head, tail) = string.splitAt(index)
      val cleanTail = tail.replace(listSeparator, "")
      string = s"$head and $cleanTail"
    }

    string
  }

}
