/*
 * Copyright (C) 2016-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.stream.alpakka.googlecloud.pubsub.impl

import akka.annotation.InternalApi
import akka.stream.Materializer
import akka.stream.alpakka.googlecloud.pubsub.impl.GoogleTokenApi.AccessTokenExpiry

import scala.concurrent.Future
import scala.io.Source

@InternalApi
private[googlecloud] class GoogleSession(clientEmail: String,
                                         privateKey: String,
                                         tokenApi: GoogleTokenApi,
                                         tokenPath: Option[String] = None) {
  protected var maybeAccessToken: Option[Future[AccessTokenExpiry]] = None

  private def getNewToken()(implicit materializer: Materializer): Future[AccessTokenExpiry] = {
    val accessToken = tokenApi.getAccessToken(clientEmail = clientEmail, privateKey = privateKey)
    maybeAccessToken = Some(accessToken)
    accessToken
  }

  private def expiresSoon(g: AccessTokenExpiry): Boolean =
    g.expiresAt < (tokenApi.now + 60)

  def getToken()(implicit materializer: Materializer): Future[String] = {
    tokenPath match {
      case Some(path) =>
        Future.successful(Source.fromFile(path).mkString)

      case None =>
        import materializer.executionContext
        maybeAccessToken
          .getOrElse(getNewToken())
          .flatMap { result =>
            if (expiresSoon(result)) {
              getNewToken()
            } else {
              Future.successful(result)
            }
          }
          .map(_.accessToken)
    }
  }
}
