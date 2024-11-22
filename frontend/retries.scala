package mimalyzer.frontend

import org.scalajs.dom

import scala.concurrent.duration.*
import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.Promise
import scala.scalajs.js.Thenable.Implicits.*

case class Stability(
    initialDelay: FiniteDuration = 100.millis,
    timeout: FiniteDuration = 5.seconds,
    delay: FiniteDuration = 100.millis,
    maxRetries: Int = 5
)

def exponentialFetch[A](
    req: () => Promise[A],
    shouldRetry: A => Boolean = {
      (_: Any) match
        case err: smithy4s.http.UnknownErrorResponse =>
          err.code == 502 || err.code == 504 || err.code == 503
        case other =>
          dom.console.log(s"Not retrying $other")
          false

    }
)(using stability: Stability): Promise[A] =
  import scalajs.js.Promise
  import stability.*

  type Result = Either[String, A]

  def go(attemptsRemaining: Int, lastResponse: Option[A]): Promise[Result] =
    val nAttempt = maxRetries - attemptsRemaining
    val newDelay: FiniteDuration =
      if nAttempt == 0 then initialDelay
      else (Math.pow(2.0, nAttempt) * delay.toMillis).millis

    if nAttempt != 0 then
      dom.console.log(
        s"Request will be retried, $attemptsRemaining remaining, with delay $newDelay",
        new Date()
      )

    def sleep(delay: FiniteDuration): Promise[Unit] =
      Promise.apply((resolve, reject) =>
        dom.window.setTimeout(() => resolve(()), delay.toMillis)
      )

    def reqPromise: Promise[Result] =
      req().`then`(resp => Right(resp))

    if attemptsRemaining == 0 then
      lastResponse match
        case None =>
          Promise.resolve(Left("no attempts left"))
        case Some(value) =>
          Promise.resolve(Right(value))
    else
      Promise
        .race(js.Array(reqPromise, sleep(timeout).`then`(_ => Left("timeout"))))
        .`then` {
          case Left(reason) =>
            sleep(newDelay).`then`(_ => go(attemptsRemaining - 1, lastResponse))
          case r @ Right(res) =>
            if shouldRetry(res) then
              sleep(newDelay).`then`(_ => go(attemptsRemaining - 1, Some(res)))
            else Promise.resolve(r)
        }
    end if
  end go

  go(maxRetries, None).`then` {
    case Left(err) =>
      Promise.reject(s"Request failed after all retries: $err")
    case Right(value) =>
      Promise.resolve(value)
  }
end exponentialFetch
