package com.speedledger.measure.graphite

import spray.httpx.Json4sSupport
import org.json4s.{DefaultFormats, Formats}

/**
 * Implicit JSON formatting.
 */
trait JsonSupport extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats
}
