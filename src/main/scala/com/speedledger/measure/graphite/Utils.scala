package com.speedledger.measure.graphite

import spray.client.pipelining._
import spray.http.{HttpRequest, GenericHttpCredentials}
import org.parboiled.common.{Base64 => ParboiledBase64}
import com.typesafe.config.Config
import java.io.{PrintWriter, File}
import scala.io.Source

object Utils {

  implicit class Base64String(input: String) extends AnyRef {
    def base64 = ParboiledBase64.rfc2045().encodeToString(input.getBytes, true)
  }

  implicit class RichConfig(config: Config) extends AnyRef {
    def getStringOption(path: String) = if (config.hasPath(path)) Some(config.getString(path)) else None
  }

  object Pipeline {
    /**
     * Adds an Authorization header with Basic scheme, if credentials are provided (not `None`).
     *
     * Uses `GenericHttpCredentials` since `BasicHttpCredentials` will always add an `:`. It expects the format to be
     * `username:password`, which it not always is.
     * @param credentials Credentials, example `username:password`.
     */
    def addOptionalBasicAuthorization(credentials: Option[String]) = {
      def identity: RequestTransformer = (in: HttpRequest) => in
      credentials.map {
        credentials =>
          addCredentials(GenericHttpCredentials("Basic", credentials.base64))
      } getOrElse identity
    }
  }

  object FileIO {
    def read(file: File) = Source.fromFile(file).getLines().mkString("\n")

    def write(data: String, file: File) {
      val writer = new PrintWriter(file)
      writer.write(data)
      writer.close()
    }
  }

}
