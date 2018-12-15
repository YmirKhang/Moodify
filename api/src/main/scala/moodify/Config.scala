package moodify

import com.typesafe.config.ConfigFactory

object Config {

  /**
    * Environment that app is running on.
    */
  val ENV: String = System.getenv("ENV")

  /**
    * Config file location.
    */
  private val configFile = ENV match {
    case "PROD" => "prod.conf"
    case "TEST" | _ => "test.conf"
  }

  /**
    * Load config file.
    */
  private val config: com.typesafe.config.Config = ConfigFactory.load(configFile)

  /**
    * Client ID for Spotify API access.
    */
  val SPOTIFY_CLIENT_ID: String = System.getenv("SPOTIFY_CLIENT_ID")

  /**
    * Client Secret for Spotify API access.
    */
  val SPOTIFY_CLIENT_SECRET: String = System.getenv("SPOTIFY_CLIENT_SECRET")

  /**
    * URI that Spotify redirects user after authentication process.
    */
  val SPOTIFY_REDIRECT_URI: String = System.getenv("SPOTIFY_REDIRECT_URI")

  /**
    * Redis host.
    */
  val REDIS_HOST: String = config.getString("REDIS.HOST")

  /**
    * Redis port.
    */
  val REDIS_PORT: Int = config.getInt("REDIS.PORT")

  /**
    * Redis password.
    */
  val REDIS_PASSWORD: String = config.getString("REDIS.PASS")

  /**
    * HTTP interface for Akka.
    */
  val HTTP_INTERFACE: String = config.getString("HTTP.INTERFACE")

  /**
    * HTTP port for Akka.
    */
  val HTTP_PORT: Int = config.getInt("HTTP.PORT")

  /**
    * Limit for top artists and tracks.
    */
  val TOP_ARTIST_TRACK_LIMIT: Int = config.getInt("SPOTIFY.TOP_ARTIST_TRACK_LIMIT")

  /**
    * Size of newly created playlist.
    */
  val NEW_PLAYLIST_SIZE: Int = config.getInt("SPOTIFY.NEW_PLAYLIST_SIZE")

  /**
    * Margin in seconds for Spotify tokens that are being reused, to avoid end up with an invalid token.
    */
  val TOKEN_TTL_MARGIN: Int = config.getInt("SPOTIFY.TOKEN_TTL_MARGIN")

}
