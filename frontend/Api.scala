package mimalyzer.frontend

import mimalyzer.protocol.*
import smithy4s_fetch.*
import scala.scalajs.js.Promise
import scala.concurrent.Future
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.core.Signal
import scalajs.js.Thenable.Implicits.*
import scala.util.Success
import concurrent.ExecutionContext.Implicits.global
import scala.annotation.implicitNotFound

class Api private (
    val mima: MimaService[Promise]
):

  def promise[A](a: mima.type => Promise[A]): Promise[A] =
    a(this.mima)

  def future[A](a: mima.type => Promise[A]): Future[A] =
    a(this.mima)

  def futureAttempt[A](
      a: mima.type => Promise[A]
  ): Future[Either[Throwable, A]] =
    a(this.mima).transform:
      case Success(value)          => Success(Right(value))
      case util.Failure(exception) => Success(Left(exception))

  def stream[A](a: mima.type => Promise[A]): EventStream[A] =
    EventStream.fromJsPromise(a(this.mima))

  def signal[A](a: mima.type => Promise[A]): Signal[Option[A]] =
    Signal.fromJsPromise(a(this.mima))
end Api

object Api:
  def create(location: String = org.scalajs.dom.window.location.origin) =

    val client =
      SimpleRestJsonFetchClient(MimaService, location).make

    Api(client)

  end create

  inline def client(using
      @implicitNotFound(
        "API client not found in scope – use `given Api = Api.create()` to put it in explicit scope"
      ) api: Api
  ): Api = api
end Api
