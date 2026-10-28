import toml.*

case class Snippet(
    description: String,
    bincompat: Boolean,
    sourcecompat: Boolean,
    scala: String
) derives Codec, upickle.default.ReadWriter


