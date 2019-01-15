package moodify.helper

import com.wrapper.spotify.model_objects.specification.{Image, Paging}

object Utils {

  /**
    * Finds the first square image in given image array.
    *
    * @param images Images
    * @return An image url or empty string
    */
  def getSquareImageURL(images: Array[Image]): String = {
    val maybeSquareImage = images.find(image => image.getHeight == image.getWidth)
    val url = if (maybeSquareImage.isDefined) maybeSquareImage.get.getUrl else ""

    url
  }

  /**
    * Checks if given collection is empty or not.
    *
    * @param collection Collection of ItemType.
    * @return True if collection is not empty.
    */
  def isNonEmptyResult[A <: AnyRef](collection: Paging[A]): Boolean = {
    collection != null && collection.getTotal > 0
  }

}
