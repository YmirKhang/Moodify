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
    case "TEST" => "test.conf"
  }

  /**
    * Load config file.
    */
  private val config: com.typesafe.config.Config = ConfigFactory.load(configFile)

}
