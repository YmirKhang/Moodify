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
    val audioPreferenceText = getAudioPreferenceText(preferences)

    val descriptionBuilder = new StringBuilder("Highly personalized with Moodify based on ")
    descriptionBuilder.append(seedText)
    descriptionBuilder.append(audioPreferenceText)
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
    * Get description text for audio preferences.
    *
    * @param preferences RecommendationPreferences
    * @return Description text for audio preferences.
    */
  private def getAudioPreferenceText(preferences: RecommendationPreferences): String = {
    val descriptionListBuffer = new ListBuffer[String]()

    if (preferences.acousticness.isDefined) {
      val target = preferences.acousticness.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level acousticness"
      descriptionListBuffer.append(text)
    }

    if (preferences.instrumentalness.isDefined) {
      val target = preferences.instrumentalness.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level instrumentalness"
      descriptionListBuffer.append(text)
    }

    if (preferences.speechiness.isDefined) {
      val target = preferences.speechiness.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level speechiness"
      descriptionListBuffer.append(text)
    }

    if (preferences.danceability.isDefined) {
      val target = preferences.danceability.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level danceability"
      descriptionListBuffer.append(text)
    }

    if (preferences.liveness.isDefined) {
      val target = preferences.liveness.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level liveness"
      descriptionListBuffer.append(text)
    }

    if (preferences.energy.isDefined) {
      val target = preferences.energy.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level energy"
      descriptionListBuffer.append(text)
    }

    if (preferences.valence.isDefined) {
      val target = preferences.valence.get.toFloat
      val level = getTargetLevel(target)
      val text = s"$level valence"
      descriptionListBuffer.append(text)
    }

    var audioFeatureDescription = ""
    if (descriptionListBuffer.nonEmpty) {
      val audioFeatureString = stringifyList(descriptionListBuffer.toList)
      audioFeatureDescription = s"Preferred audio features are $audioFeatureString"
    }

    audioFeatureDescription
  }

  /**
    * Get level text for given target value.
    *
    * @param target Target value for feature.
    * @return Level text
    */
  private def getTargetLevel(target: Float): String = {
    if (target <= 0.35) "low"
    else if (target >= 0.65) "high"
    else "medium"
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
