package mimalyzer

enum State:
  case Added
  case Completed
  case Failed
  case Processing

  def stringValue =
    this match
      case Added      => "added"
      case Completed  => "completed"
      case Failed     => "failed"
      case Processing => "processing"
end State

object State:
  val mapping = State.values.map(v => v.stringValue -> v).toMap
  def fromString(s: String) = mapping.get(s)
