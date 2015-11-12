package io.scalac.octopus.client

import io.scalac.octopus.client.tools.DateOps._
import utest._
import io.scalac.octopus.client.tools.TimeUnit._
import scala.language.postfixOps
import scala.scalajs.js.Date

object DateOpsTests extends TestSuite {
  override val tests = TestSuite {
    "month start from now should have" - {
      val now = new Date(Date.now())
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

    "month end from now should have" - {
      val now = new Date(Date.now())
      val monthEnd = getMonthEnd(now)

      "maximum month date" - {
        val willPassYear = monthEnd.getMonth() == 11

        val nextMonthNumber =
          if (willPassYear) 0
          else monthEnd.getMonth() + 1

        assert((monthEnd + (1 days)).getMonth() == nextMonthNumber)
      }

      "the same month as today" - assert(monthEnd.getMonth() == now.getMonth())

      "the same year as today" - assert(monthEnd.getFullYear() == now.getFullYear())
    }

    "next month should have" - {
      val now = new Date(Date.now())
      val nextMonth = getNextMonth(now)

      "day 1" - assert(nextMonth.getDate() == 1)

      val willPassYear = now.getMonth() == 11

      "month greater by 1 or equal to zero" - {
        if (willPassYear)
          assert(nextMonth.getMonth() == 0)
        else assert(nextMonth.getMonth() == now.getMonth() + 1)
      }

      "same year or next one" - {
        if (willPassYear)
          assert(nextMonth.getFullYear() == now.getFullYear() + 1)
        else assert(nextMonth.getFullYear() == now.getFullYear())
      }
    }

    "previous month should have" - {
      val now = new Date(Date.now())
      val previousMonth = getPreviousMonth(now)

      "day 1" - assert(previousMonth.getDate() == 1)

      val willPassYear = now.getMonth() == 0

      "month smaller by 1 or equal to 11" - {
        if (willPassYear)
          assert(previousMonth.getMonth() == 11)
        else assert(previousMonth.getMonth() == now.getMonth() - 1)
      }

      "same year or previous one" - {
        if (willPassYear)
          assert(previousMonth.getFullYear() == now.getFullYear() - 1)
        else assert(previousMonth.getFullYear() == now.getFullYear())
      }
    }

    "a DateOps should" - {
      val now = new Date(Date.now())

      "be able to add some days" - {
        (1 to 100).foreach { i =>
          val added = now + (i days)
          val diff = (added - now).valueOf() / 1000
          assert(diff == 3600 * 24 * i)
        }
      }

      "be able to substract some days" - {
        (1 to 100).foreach { i =>
          val substracted = now - (i days)
          val diff = (now - substracted).valueOf() / 1000
          assert(diff == 3600 * 24 * i)
        }
      }

      "tell if two dates are on the same" - {
        "day" - assert(now isSameDay now)
        "month" - assert(now isSameMonth getMonthStart(now))
        "year" - assert(now isSameYear getNextMonth(now))
      }
    }
  }
}