package moodify.enumeration

object Environment extends Enumeration {
  type Type = Value
  val TEST: Environment.Value = Value("TEST")
  val PRODUCTION: Environment.Value = Value("PROD")
}
