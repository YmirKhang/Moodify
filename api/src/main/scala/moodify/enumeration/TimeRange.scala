package moodify.enumeration

object TimeRange extends Enumeration {
  type Type = Value
  val SHORT_TERM: TimeRange.Value = Value("short_term")
  val MEDIUM_TERM: TimeRange.Value = Value("medium_term")
  val LONG_TERM: TimeRange.Value = Value("long_term")
}
