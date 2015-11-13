package io.scalac.octopus.client

import io.scalac.octopus.client.tools.DateOps._
import utest._
import io.scalac.octopus.client.tools.TimeUnit._
import scala.concurrent.duration.DurationConversions
import scala.language.postfixOps
import scala.scalajs.js.Date

object DateOpsTests extends TestSuite {
  override val tests = TestSuite {
    "month start should have" - {
      val now = new Date(2015, 11, 15)
      val monthStart = getMonthStart(now)

      "day equal to 1" - {
        assert(monthStart.getDate() == 1)
      }

      "the same month as today" - {
        assert(monthStart.getMonth() == now.getMonth())
      }

      "the same year as today" - {
        assert(monthStart.getFullYear() == now.getFullYear())
      }
    }

    "month end should have" - {
      val now = new Date(2015, 11, 15)
      val monthEnd = getMonthEnd(now)

      "maximum month date" - {
        val nextMonthNumber = getNextMonthStart(now).getMonth()
        assert(monthEnd.getMonth() == now.getMonth())
        assert((monthEnd + (1 days)).getMonth() == nextMonthNumber)
      }

      "the same month as today" - assert(monthEnd.getMonth() == now.getMonth())

      "the same year as today" - assert(monthEnd.getFullYear() == now.getFullYear())
    }

    "next month should have" - {
      "if it's December now" - {
        val decemberDay = new Date(2015, 11, 15)
        val nextMonth = getNextMonthStart(decemberDay)
        "day 1" - assert(nextMonth.getDate() == 1)
        "month 0" - assert(nextMonth.getMonth() == 0)
        "next year" - assert(nextMonth.getFullYear() == decemberDay.getFullYear() + 1)
      }

      "if it's not December now" - {
        val notDecemberDay = new Date(2015, 0, 15)
        val nextMonth = getNextMonthStart(notDecemberDay)
        "day 1" - assert(nextMonth.getDate() == 1)
        "next month" - assert(nextMonth.getMonth() == notDecemberDay.getMonth() + 1)
        "same year" - assert(nextMonth.getFullYear() == notDecemberDay.getFullYear())
      }
    }

    "previous month should have" - {
      "if it's January now" - {
        val januaryDay = new Date(2015, 0, 15)
        val previousMonth = getPreviousMonthStart(januaryDay)
        "day 1" - assert(previousMonth.getDate() == 1)
        "month 11" - assert(previousMonth.getMonth() == 11)
        "previous year" - assert(previousMonth.getFullYear() == januaryDay.getFullYear() - 1)
      }

      "if it's not January now" - {
        val notJanuaryDay = new Date(2015, 5, 15)
        val previousMonth = getPreviousMonthStart(notJanuaryDay)
        "day 1" - assert(previousMonth.getDate() == 1)
        "previous month" - assert(previousMonth.getMonth() == notJanuaryDay.getMonth() - 1)
        "same year" - assert(previousMonth.getFullYear() == notJanuaryDay.getFullYear())
      }
    }

    "a DateOps should" - {
      val now = new Date(2015, 10, 15)

      "be able to add some days" - {
        (1 to 100).foreach { i =>
          val someDays = i.days
          val added = now + someDays
          val diff = (added.valueOf() - now.valueOf()) / 1000
          assert(diff == 3600 * 24 * i)
        }
      }

      "be able to substract some days" - {
        (1 to 100).foreach { i =>
          val substracted = now - (i days)
          val diff = (now.valueOf() - substracted.valueOf()) / 1000
          assert(diff == 3600 * 24 * i)
        }
      }

      "tell if two dates are on the same" - {
        "day" - assert(now isSameDay now)
        "month" - assert(now isSameMonth getMonthStart(now))
        "year" - assert(now isSameYear getNextMonthStart(now))
      }
    }
  }
}