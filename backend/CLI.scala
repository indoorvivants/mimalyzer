package mimalyzer
package backend

import com.monovore.decline.Argument
import cats.data.ValidatedNel
import com.comcast.ip4s.*
import cats.syntax.all.*

import CLI.BindConfig
import decline_derive.CommandApplication

enum CLI derives CommandApplication:
  case Server(bind: BindConfig, workers: Int = 1)
  case Worker(bind: BindConfig)

object CLI:
  case class BindConfig(port: Port = port"8080", host: Host = host"localhost")
      derives CommandApplication

extension [A, B](x: Argument[A])
  def mapValidated(f: A => ValidatedNel[String, B]): Argument[B] =
    new Argument[B]:
      override def read(string: String): ValidatedNel[String, B] =
        x.read(string).andThen(f)

      override def defaultMetavar: String = x.defaultMetavar

given Argument[Port] =
  Argument.readInt.mapValidated(p =>
    Port.fromInt(p).toRight(s"Invalid port $p").toValidatedNel
  )

given Argument[Host] =
  Argument.readString.mapValidated(p =>
    Host.fromString(p).toRight(s"Invalid host $p").toValidatedNel
  )
