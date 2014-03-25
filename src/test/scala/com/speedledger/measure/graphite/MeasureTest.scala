package com.speedledger.measure.graphite

import org.scalatest.{Matchers, FreeSpec}

/**
 * Tests for [[com.speedledger.measure.graphite.Measure]].
 */
class MeasureTest extends FreeSpec with Matchers {
  "A Measure" - {
    "should clean the path from separators and parentheses" - {
      val measure = Measure(Seq("(a)", "b .c", " /d"), 1000, 1000)
      measure.cleanPath should contain theSameElementsInOrderAs Seq("a", "b__c", "__d")
    }

    "should produce a dot separated path" - {
      val measure = Measure(Seq("a", "b", "c"), 1000, 1000)
      measure.dotPath shouldEqual "a.b.c"
    }

    "should produce a dot separated clean path" - {
      val measure = Measure(Seq("(a)", "b .c", " /d"), 1000, 1000)
      measure.dotPath shouldEqual "a.b__c.__d"
    }

    "should output time in seconds" - {
      val measure = Measure(Seq("a", "b", "c"), 1000, 123001)
      measure.timeInSeconds shouldEqual 123
    }
  }
}
