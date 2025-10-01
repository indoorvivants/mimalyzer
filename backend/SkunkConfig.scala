package mimalyzer


case class SkunkConfig(
    maxSessions: Int,
    strategy: skunk.TypingStrategy,
    debug: Boolean
)

object SkunkConfig
    extends SkunkConfig(
      maxSessions = 2,
      strategy = skunk.TypingStrategy.SearchPath,
      debug = false
    )
